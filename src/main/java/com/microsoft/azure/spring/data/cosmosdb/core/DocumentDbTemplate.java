/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.google.common.collect.Lists;
import com.microsoft.azure.cosmosdb.*;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.CountQueryGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DocumentDbTemplate implements DocumentDbOperations, ApplicationContextAware {

    private static final String COUNT_VALUE_KEY = "_aggregate";

    private final DocumentDbFactory documentDbFactory;

    private final MappingDocumentDbConverter mappingDocumentDbConverter;

    private final String dbName;

    private final String dbLink;

    private AsyncDocumentClient asyncDocumentClient;

    public DocumentDbTemplate(DocumentDbFactory documentDbFactory,
                              MappingDocumentDbConverter mappingDocumentDbConverter,
                              String dbName) {
        Assert.notNull(documentDbFactory, "DocumentDbFactory must not be null!");
        Assert.notNull(mappingDocumentDbConverter, "MappingDocumentDbConverter must not be null!");

        this.dbName = dbName;
        this.dbLink = getDatabaseLink(dbName);
        this.documentDbFactory = documentDbFactory;
        this.mappingDocumentDbConverter = mappingDocumentDbConverter;
    }

    private AsyncDocumentClient getAsyncDocumentClient() {
        if (this.asyncDocumentClient == null) {
            this.asyncDocumentClient = this.documentDbFactory.getAsyncDocumentClient();
        }

        return this.asyncDocumentClient;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    private void validate(String collectionName) {
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces.");
    }

    private void validate(DocumentQuery query, String collectionName, Class<?> entityClass,
                          List<String> partitionKeyNames) {
        validate(query, entityClass);
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces.");
        Assert.notNull(partitionKeyNames, "partitionKeyNames should not be null.");
    }

    private <T> void validate(String collectionName, T entity) {
        validate(collectionName);
        Assert.notNull(entity, "entity should not be null.");
    }

    private void validate(DocumentQuery query, Class<?> entityClass) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(entityClass, "entityClass should not be null.");
    }

    private void validate(String collectionName, Object entity, Class<?> entityClass) {
        validate(collectionName, entity);

        Assert.notNull(entityClass, "entityClass should not be null.");
    }

    private void validate(DocumentQuery query, String collectionName, Class<?> entityClass) {
        validate(collectionName);
        validate(query, entityClass);
    }

    @Override
    public <T> T insert(@NonNull String collectionName, @NonNull T entity, @Nullable PartitionKey key) {
        validate(collectionName, entity);

        return insertAsync(collectionName, entity, key).toBlocking().single();
    }

    @Override
    public <T> Observable<T> insertAsync(@NonNull String collectionName, @NonNull T entity,
                                         @Nullable PartitionKey key) {
        validate(collectionName, entity);

        @SuppressWarnings("unchecked") final Class<T> domainClass = (Class<T>) entity.getClass();
        final String collectionLink = getCollectionLink(collectionName);
        final Document document = mappingDocumentDbConverter.toCosmosdbDocument(entity);

        return getAsyncDocumentClient()
                .createDocument(collectionLink, document, getRequestOptions(key, null), false)
                .doOnNext(r -> log.debug("Create Document Async from {}.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to insert domain", e);
                })
                .map(ResourceResponse::getResource)
                .map(d -> this.mappingDocumentDbConverter.read(domainClass, d));
    }

    @Override
    public <T> void upsert(@NonNull String collectionName, @NonNull T entity, @Nullable PartitionKey key) {
        validate(collectionName, entity);

        upsertAsync(collectionName, entity, key).toBlocking().single();
    }

    @Override
    public <T> Observable<T> upsertAsync(@NonNull String collectionName, @NonNull T entity,
                                         @Nullable PartitionKey key) {
        validate(collectionName, entity);

        final String collectionLink = getCollectionLink(collectionName);
        final Document document = mappingDocumentDbConverter.toCosmosdbDocument(entity);

        return getAsyncDocumentClient()
                .upsertDocument(collectionLink, document, getRequestOptions(key, null), false)
                .doOnNext(r -> log.debug("Upsert Document Async from {}.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to upsert domain", e);
                })
                .map(d -> entity);
    }

    @Override
    public <T> Optional<T> findById(@NonNull String collectionName, @NonNull Object id, @NonNull Class<T> entityClass,
                                    @Nullable PartitionKey key) {
        validate(collectionName, id, entityClass);
        assertValidId(id);

        try {
            return Optional.of(findByIdAsync(collectionName, id, entityClass, key).toBlocking().single());
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof DocumentClientException
                    && ((DocumentClientException) cause).getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return Optional.empty();
            }

            throw new DocumentDBAccessException("Failed to read Document", e);
        }
    }

    @Override
    public <T> Observable<T> findByIdAsync(@NonNull String collectionName, @NonNull Object id,
                                           @NonNull Class<T> entityClass, @Nullable PartitionKey key) {
        validate(collectionName, id, entityClass);
        assertValidId(id);

        final RequestOptions options = new RequestOptions();
        final String collectionLink = getCollectionLink(collectionName);

        options.setPartitionKey(key);

        return getAsyncDocumentClient()
                .readDocument(getDocumentLink(collectionName, id), options)
                .doOnNext(r -> log.debug("Read Document Async from {}.", collectionLink))
                .map(ResourceResponse::getResource)
                .map(d -> this.mappingDocumentDbConverter.read(entityClass, d));
    }

    private Observable<Document> deleteDocumentsAsync(@NonNull DocumentQuery query, @NonNull String collectionName,
                                                      @NonNull List<String> partitionKeyNames) {
        Assert.isTrue(partitionKeyNames.size() <= 1, "Only one Partition is supported.");

        return findDocumentsAsync(query, collectionName, partitionKeyNames)
                .doOnSubscribe(() -> log.debug("Delete Document Async."))
                .onErrorResumeNext(e -> {
                    throw new DocumentDBAccessException("Failed to delete Document", e);
                })
                .flatMap(d -> {
                    final RequestOptions options = new RequestOptions();

                    if (!partitionKeyNames.isEmpty() && StringUtils.hasText(partitionKeyNames.get(0))) {
                        final String keyName = partitionKeyNames.get(0);
                        options.setPartitionKey(new PartitionKey(d.get(keyName)));
                    }

                    return getAsyncDocumentClient()
                            .deleteDocument(d.getSelfLink(), options)
                            .map(r -> d);
                });
    }

    @Override
    public <T> Observable<T> deleteAllAsync(@NonNull String collectionName, @NonNull List<String> partitionKeyNames) {
        validate(collectionName, partitionKeyNames);

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        return deleteDocumentsAsync(query, collectionName, partitionKeyNames)
                .onErrorResumeNext(e -> {
                    throw new DocumentDBAccessException("Failed to retrieve Document", e);
                })
                .flatMap(d -> Observable.empty());
    }

    @Override
    public void deleteAll(@NonNull String collectionName, @NonNull List<String> partitionKeyNames) {
        validate(collectionName, partitionKeyNames);

        deleteAllAsync(collectionName, partitionKeyNames).toCompletable().await();
    }

    @Override
    public <T> List<T> findAll(String collectionName, final Class<T> domainClass, String partitionKeyName) {
        return findAllAsync(collectionName, domainClass, partitionKeyName).toList().toBlocking().single();
    }

    @Override
    public <T> Observable<T> findAllAsync(String collectionName, Class<T> entityClass, String partitionKeyName) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(entityClass, "entityClass should not be null");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        final List<String> keyNames = StringUtils.hasText(partitionKeyName) ? Arrays.asList(partitionKeyName) :
                new ArrayList<>();

        return findDocumentsAsync(query, collectionName, keyNames)
                .doOnSubscribe(() -> log.debug("Find all documents for Class {} async", entityClass))
                .onErrorResumeNext(e -> {
                    throw new DocumentDBAccessException("Failed to find all documents.", e);
                }).map(d -> this.mappingDocumentDbConverter.read(entityClass, d));
    }

    @Override
    public void deleteCollection(@NonNull String collectionName) {
        validate(collectionName);

        deleteCollectionAsync(collectionName).toCompletable().await();
    }

    @Override
    public Observable<String> deleteCollectionAsync(@NonNull String collectionName) {
        validate(collectionName);

        final String collectionLink = getCollectionLink(collectionName);

        return getAsyncDocumentClient()
                .deleteCollection(collectionLink, null)
                .doOnNext(r -> log.debug("Delete Connection {} Async.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to delete collection: " + collectionName, e);
                })
                .map(r -> collectionName);
    }

    public String getCollectionName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return new DocumentDbEntityInformation<>(domainClass).getCollectionName();
    }

    private void createDatabaseIfNotExists() {
        getAsyncDocumentClient().readDatabase(this.dbLink, null)
                .doOnNext(r -> log.info("Database [{}] exists already.", this.dbName))
                .onErrorResumeNext(e -> {
                    if (e instanceof DocumentClientException) {
                        if (((DocumentClientException) e).getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                            log.info("Creating Database [{}] ...", dbName);

                            final Database db = new Database();
                            db.setId(dbName);

                            return getAsyncDocumentClient().createDatabase(db, null);
                        }
                    }

                    throw new DocumentDBAccessException("createOrGetDatabase exception", e);
                })
                .toCompletable()
                .await();
    }

    private DocumentCollection createDocumentCollectionInstance(@NonNull DocumentDbEntityInformation information) {
        final IndexingPolicy policy = information.getIndexingPolicy();
        final DocumentCollection collection = new DocumentCollection();

        if (policy.getAutomatic()) {
            collection.setDefaultTimeToLive(information.getTimeToLive());
        }

        collection.setId(information.getCollectionName());
        collection.setIndexingPolicy(information.getIndexingPolicy());

        final String partitionKeyName = information.getPartitionKeyFieldName();

        if (StringUtils.hasText(partitionKeyName)) {
            final PartitionKeyDefinition definition = new PartitionKeyDefinition();

            definition.setPaths(Collections.singletonList(getPartitionKeyPath(partitionKeyName)));
            collection.setPartitionKey(definition);
        }

        return collection;
    }

    private Optional<DocumentCollection> getCollection(@NonNull String collectionName) {
        validate(collectionName);

        final String sqlQuery = "SELECT * FROM r WHERE r.id=@id";
        final SqlParameter sqlParameter = new SqlParameter("@id", collectionName);
        final SqlParameterCollection sqlParameterCollection = new SqlParameterCollection(sqlParameter);
        final SqlQuerySpec sqlQuerySpec = new SqlQuerySpec(sqlQuery, sqlParameterCollection);

        try {
            final DocumentCollection collection = getAsyncDocumentClient().queryCollections(dbLink, sqlQuerySpec, null)
                    .filter(r -> !r.getResults().isEmpty())
                    .map(r -> r.getResults().get(0))
                    .toBlocking()
                    .single();

            log.info("Database Collection [{}] of Database [{}] exists already.", collectionName, this.dbName);

            return Optional.of(collection);
        } catch (NoSuchElementException ignore) {
            return Optional.empty();
        }
    }

    @Override
    public DocumentCollection createCollectionIfNotExists(@NonNull DocumentDbEntityInformation information) {
        final String collectionName = information.getCollectionName();

        createDatabaseIfNotExists();

        return getCollection(collectionName).orElseGet(() -> {
            log.info("Creating Collection [{}] of Database [{}] ...", collectionName, this.dbName);
            final DocumentCollection collection = createDocumentCollectionInstance(information);

            collection.setId(collectionName);

            return getAsyncDocumentClient().createCollection(this.dbLink, collection, null)
                    .map(ResourceResponse::getResource)
                    .toBlocking()
                    .single();
        });
    }

    @Override
    public void deleteById(@NonNull String collectionName, @NonNull Object id, @Nullable PartitionKey partitionKey) {
        validate(collectionName, id);
        assertValidId(id);

        try {
            deleteByIdAsync(collectionName, id, partitionKey).toBlocking().single();
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof DocumentClientException
                    && ((DocumentClientException) cause).getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return;
            }

            throw new DocumentDBAccessException("Failed to delete Document", e);
        }
    }

    @Override
    public Observable<Object> deleteByIdAsync(@NonNull String collectionName, @NonNull Object id,
                                              @Nullable PartitionKey key) {
        validate(collectionName, id);
        assertValidId(id);

        final String documentLink = getDocumentLink(collectionName, id.toString());
        final RequestOptions options = getRequestOptions(key, null);

        return getAsyncDocumentClient()
                .deleteDocument(documentLink, options)
                .doOnNext(r -> log.debug("Delete Document Async from {}", documentLink))
                .map(r -> id);
    }

    private String getDatabaseLink(String databaseName) {
        return "dbs/" + databaseName;
    }

    private String getCollectionLink(String collectionName) {
        return this.dbLink + "/colls/" + collectionName;
    }

    private String getDocumentLink(String collectionName, Object documentId) {
        return getCollectionLink(collectionName) + "/docs/" + documentId;
    }

    private String getPartitionKeyPath(String partitionKey) {
        return "/" + partitionKey;
    }

    private RequestOptions getRequestOptions(PartitionKey key, Integer requestUnit) {
        if (key == null && requestUnit == null) {
            return null;
        }

        final RequestOptions requestOptions = new RequestOptions();
        if (key != null) {
            requestOptions.setPartitionKey(key);
        }
        if (requestUnit != null) {
            requestOptions.setOfferThroughput(requestUnit);
        }

        return requestOptions;
    }

    private Observable<com.microsoft.azure.cosmosdb.Document> executeQueryAsyncDocument(
            @NonNull SqlQuerySpec sqlQuerySpec, @NonNull String collectionName, @NonNull FeedOptions options) {
        return executeQueryAsync(sqlQuerySpec, collectionName, options)
                .map(FeedResponse::getResults)
                .flatMap(Observable::from);
    }

    private Observable<FeedResponse<com.microsoft.azure.cosmosdb.Document>> executeQueryAsync(
            @NonNull SqlQuerySpec sqlQuerySpec, @NonNull String collectionName, @NonNull FeedOptions options) {
        final String selfLink = getCollectionLink(collectionName);

        return getAsyncDocumentClient()
                .queryDocuments(selfLink, sqlQuerySpec, options)
                .doOnNext(r -> log.debug("Query Document Async from {}", selfLink));
    }

    @Override
    public <T> List<T> find(@NonNull DocumentQuery query, @NonNull String collectionName,
                            @NonNull Class<T> entityClass, @NonNull List<String> partitionKeyNames) {
        validate(query, collectionName, entityClass, partitionKeyNames);
        validateQuery(query, entityClass, collectionName);

        return Lists.newArrayList(
                findAsync(query, collectionName, entityClass, partitionKeyNames).toBlocking().getIterator());
    }

    @Override
    public <T> Observable<T> findAsync(@NonNull DocumentQuery query, @NonNull String collectionName,
                                       @NonNull Class<T> entityClass, List<String> partitionKeyNames) {
        validate(query, collectionName, entityClass, partitionKeyNames);

        final FeedOptions feedOptions = new FeedOptions();
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);

        feedOptions.setEnableCrossPartitionQuery(query.isCrossPartitionQuery(partitionKeyNames));

        return executeQueryAsync(sqlQuerySpec, collectionName, feedOptions)
                .map(FeedResponse::getResults)
                .flatMap(Observable::from)
                .map(d -> this.getConverter().read(entityClass, d));
    }

    @Override
    public Boolean exists(@NonNull DocumentQuery query, @NonNull String collectionName, @NonNull Class<?> entityClass) {
        validate(query, collectionName, entityClass);

        return existsAsync(query, collectionName, entityClass).toBlocking().single();
    }

    @Override
    public Observable<Boolean> existsAsync(@NonNull DocumentQuery query, @NonNull String collectionName,
                                           @NonNull Class<?> entityClass) {
        validate(query, collectionName, entityClass);

        return countAsync(query, collectionName, entityClass).map(c -> c > 0).single();
    }

    private void validateQuery(@NonNull DocumentQuery query, @NonNull Class<?> domainClass, String collectionName) {
        if (!query.getSort().isSorted() && !query.getCriteriaByType(CriteriaType.STARTS_WITH).isPresent()) {
            return;
        }

        final Optional<DocumentCollection> optional = getCollection(collectionName);

        Assert.isTrue(optional.isPresent(), "Collection should be created already.");

        if (query.getSort().isSorted()) { // avoiding unnecessary query with DocumentCollection
            query.validateSort(domainClass, QueryValidator.isCollectionSupportSortByString(optional.get()));
        }
        if (query.getCriteriaByType(CriteriaType.STARTS_WITH).isPresent()) {
            query.validateStartsWith(QueryValidator.isCollectionSupportStartsWith(optional.get()));
        }
    }

    private Observable<com.microsoft.azure.cosmosdb.Document> findDocumentsAsync(
            @NonNull DocumentQuery query, @NonNull String collectionName, @NonNull List<String> partitionKeyNames) {
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final FeedOptions options = new FeedOptions();
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(partitionKeyNames);

        options.setEnableCrossPartitionQuery(isCrossPartitionQuery);

        return executeQueryAsyncDocument(sqlQuerySpec, collectionName, options);
    }

    /**
     * Delete the DocumentQuery, need to query the domains at first, then delete the document
     * from the result.
     * The cosmosdb Sql API do _NOT_ support DELETE query, we cannot add one DeleteQueryGenerator.
     *
     * @param query          The representation for query method.
     * @param entityClass    Class of domain
     * @param collectionName Collection Name of database
     * @param <T>            Entity type
     * @return All the deleted documents as List.
     */
    @Override
    public <T> List<T> delete(@NonNull DocumentQuery query, @NonNull String collectionName,
                              @NonNull Class<T> entityClass, @NonNull List<String> partitionKeyNames) {
        validate(query, collectionName, entityClass, partitionKeyNames);

        return Lists.newArrayList(
                deleteAsync(query, collectionName, entityClass, partitionKeyNames).toBlocking().getIterator());
    }

    @Override
    public <T> Observable<T> deleteAsync(@NonNull DocumentQuery query, @NonNull String collectionName,
                                         @NonNull Class<T> entityClass, @NonNull List<String> partitionKeyNames) {
        validate(query, collectionName, entityClass, partitionKeyNames);

        return deleteDocumentsAsync(query, collectionName, partitionKeyNames)
                .map(d -> getConverter().read(entityClass, d));
    }

    @Override
    public <T> Page<T> findAll(Pageable pageable, Class<T> domainClass, String collectionName) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(pageable);
        return paginationQuery(query, domainClass, collectionName);
    }

    @Override
    public <T> Observable<Page<T>> findAllAsync(Pageable pageable, Class<T> domainClass, String collectionName) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(pageable);
        return paginationQueryAsync(query, domainClass, collectionName);
    }

    @Override
    public <T> Page<T> paginationQuery(DocumentQuery query, Class<T> domainClass, String collectionName) {
        return paginationQueryAsync(query, domainClass, collectionName).toBlocking().single();
    }

    @Override
    public <T> Observable<Page<T>> paginationQueryAsync(DocumentQuery query, Class<T> domainClass,
                                                        String collectionName) {
        Assert.isTrue(query.getPageable().getPageSize() > 0, "pageable should have page size larger than 0");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        final Pageable pageable = query.getPageable();
        final FeedOptions feedOptions = new FeedOptions();
        if (pageable instanceof DocumentDbPageRequest) {
            feedOptions.setRequestContinuation(((DocumentDbPageRequest) pageable).getRequestContinuation());
        }

        feedOptions.setMaxItemCount(pageable.getPageSize());
        feedOptions.setEnableCrossPartitionQuery(query.isCrossPartitionQuery(getPartitionKeyNames(domainClass)));

        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);

        final Observable<Long> countObservable = countAsync(query, collectionName, domainClass);

        return Observable.zip(countObservable, executeQueryAsync(sqlQuerySpec, collectionName, feedOptions).first(),
                (count, response) -> new PageResponse<>(count, response))
                .doOnError(e -> {
                    throw new DocumentDBAccessException("Failed to execute pagination query.", e);
                })
                .map(pageResponse -> {
                    final FeedResponse<com.microsoft.azure.cosmosdb.Document> r = pageResponse.getResponse();
                    final long count = pageResponse.getCount();

                    log.debug(r.getResults().size() + " documents returned.");
                    final List<T> result = r.getResults().stream().filter(Objects::nonNull)
                            .map(d -> mappingDocumentDbConverter.read(domainClass, d))
                            .collect(Collectors.toList());

                    final DocumentDbPageRequest pageRequest = DocumentDbPageRequest.of(pageable.getPageNumber(),
                            pageable.getPageSize(), r.getResponseContinuation());

                    return new PageImpl<>(result, pageRequest, count);
                });
    }

    @Override
    public long count(String collectionName) {
        Assert.hasText(collectionName, "collectionName should not be empty");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        return getCountValue(query, true, collectionName).toBlocking().single();
    }

    @Override
    public <T> long count(DocumentQuery query, Class<T> domainClass, String collectionName) {
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        return getCountValue(query, isCrossPartitionQuery, collectionName).toBlocking().single();
    }

    @Override
    public Observable<Long> countAsync(String collectionName) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        return getCountValue(query, true, collectionName);
    }

    @Override
    public Observable<Long> countAsync(@NonNull DocumentQuery query, @NonNull String collectionName,
                                       @NonNull Class<?> domainClass) {
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        return getCountValue(query, isCrossPartitionQuery, collectionName);
    }

    private Observable<Long> getCountValue(DocumentQuery query, boolean isCrossPartitionQuery, String collectionName) {
        final SqlQuerySpec querySpec = new CountQueryGenerator().generate(query);

        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.setEnableCrossPartitionQuery(isCrossPartitionQuery);

        return executeQueryAsync(querySpec, collectionName, feedOptions)
                .map(response -> response.getResults().get(0).getLong(COUNT_VALUE_KEY));
    }

    @Override
    public MappingDocumentDbConverter getConverter() {
        return this.mappingDocumentDbConverter;
    }

    @SuppressWarnings("unchecked")
    private List<String> getPartitionKeyNames(Class<?> domainClass) {
        final DocumentDbEntityInformation entityInfo = new DocumentDbEntityInformation(domainClass);

        if (entityInfo.getPartitionKeyFieldName() == null) {
            return new ArrayList<>();
        }

        return Arrays.asList(entityInfo.getPartitionKeyFieldName());
    }

    private void assertValidId(Object id) {
        Assert.notNull(id, "id should not be null");
        if (id instanceof String) {
            Assert.hasText(id.toString(), "id should not be empty or only whitespaces.");
        }
    }

    // Internal class to wrap count and FeedResponse data
    @AllArgsConstructor
    @Getter
    @Setter
    private static class PageResponse<T extends Resource> {
        private long count;
        private FeedResponse<T> response;
    }
}
