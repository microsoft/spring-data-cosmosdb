/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.cosmosdb.*;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
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
import lombok.AccessLevel;
import lombok.Getter;
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
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DocumentDbTemplate implements DocumentDbOperations, ApplicationContextAware {

    private static final String COUNT_VALUE_KEY = "_aggregate";

    @Getter(AccessLevel.PRIVATE)
    private final DocumentClient documentClient;

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
        this.documentClient = documentDbFactory.getDocumentClient();
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

    @Override
    public <T> T insert(@NonNull String collectionName, @NonNull T domain, @Nullable PartitionKey key) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domain, "domain should not be null");

        return insertAsync(collectionName, domain, key).toBlocking().single();
    }

    @Override
    public <T> Observable<T> insertAsync(@NonNull String collectionName, @NonNull T domain,
                                         @Nullable PartitionKey key) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domain, "domain should not be null");

        @SuppressWarnings("unchecked") final Class<T> domainClass = (Class<T>) domain.getClass();
        final String collectionLink = getCollectionLink(collectionName);
        final com.microsoft.azure.cosmosdb.Document document = mappingDocumentDbConverter.toCosmosdbDocument(domain);

        return getAsyncDocumentClient()
                .createDocument(collectionLink, document, getRequestOptions(key, null), false)
                .doOnNext(r -> log.debug("Create Document Async from {}.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to insert domain", e);
                })
                .map(ResourceResponse::getResource)
                .map(d -> this.mappingDocumentDbConverter.readAsync(domainClass, d));
    }

    @Override
    public <T> void upsert(@NonNull String collectionName, @NonNull T domain, @Nullable PartitionKey key) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domain, "domain should not be null");

        upsertAsync(collectionName, domain, key).toBlocking().single();
    }

    @Override
    public <T> Observable<T> upsertAsync(@NonNull String collectionName, @NonNull T domain,
                                         @Nullable PartitionKey key) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domain, "domain should not be null");

        final String collectionLink = getCollectionLink(collectionName);
        final com.microsoft.azure.cosmosdb.Document document = mappingDocumentDbConverter.toCosmosdbDocument(domain);

        return getAsyncDocumentClient()
                .upsertDocument(collectionLink, document, getRequestOptions(key, null), false)
                .doOnNext(r -> log.debug("Upsert Document Async from {}.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to upsert domain", e);
                })
                .map(d -> domain);
    }

    @Override
    public <T> Optional<T> findById(@NonNull String collectionName, @NonNull Object id, @NonNull Class<T> entityClass,
                                    @Nullable PartitionKey key) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
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
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        assertValidId(id);

        final RequestOptions options = new RequestOptions();
        final String collectionLink = getCollectionLink(collectionName);

        options.setPartitionKey(key);

        return getAsyncDocumentClient()
                .readDocument(getDocumentLink(collectionName, id), options)
                .doOnNext(r -> log.debug("Read Document Async from {}.", collectionLink))
                .map(ResourceResponse::getResource)
                .map(d -> this.mappingDocumentDbConverter.readAsync(entityClass, d));
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findAll(getCollectionName(entityClass), entityClass);
    }

    public <T> List<T> findAll(String collectionName, final Class<T> domainClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domainClass, "entityClass should not be null");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        final List<Document> results = findDocuments(query, domainClass, collectionName);

        return results.stream().map(d -> getConverter().read(domainClass, d)).collect(Collectors.toList());
    }

    public void deleteAll(@NonNull String collectionName, @NonNull Class<?> domainClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        this.delete(query, domainClass, collectionName);
    }

    @Override
    public void deleteCollection(@NonNull String collectionName) {
        Assert.hasText(collectionName, "collectionName should have text.");

        getAsyncDocumentClient().deleteCollection(getCollectionLink(collectionName), null)
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to delete collection: " + collectionName, e);
                })
                .toCompletable()
                .await();
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
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
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
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
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

    private <T> List<T> executeQuery(@NonNull com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec,
                                     boolean isCrossPartition, @NonNull Class<T> domainClass, String collectionName) {
        final com.microsoft.azure.documentdb.FeedResponse<Document> feedResponse =
                executeQuery(sqlQuerySpec, isCrossPartition, collectionName);
        final List<Document> result = feedResponse.getQueryIterable().toList();

        return result.stream().map(r -> getConverter().read(domainClass, r)).collect(Collectors.toList());
    }

    private com.microsoft.azure.documentdb.FeedResponse<Document> executeQuery(
            @NonNull com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec,
            boolean isCrossPartition, String collectionName) {
        final com.microsoft.azure.documentdb.FeedOptions feedOptions = new com.microsoft.azure.documentdb.FeedOptions();
        final String selfLink = getCollectionLink(collectionName);

        feedOptions.setEnableCrossPartitionQuery(isCrossPartition);

        return getDocumentClient().queryDocuments(selfLink, sqlQuerySpec, feedOptions);
    }

    // Use public for now, will change to private after referenced
    public Observable<com.microsoft.azure.cosmosdb.Document> executeQueryAsyncDocument(
            @NonNull SqlQuerySpec sqlQuerySpec, @NonNull String collectionName, @NonNull FeedOptions options) {
        return executeQueryAsync(sqlQuerySpec, collectionName, options)
                .subscribeOn(Schedulers.io())
                .map(FeedResponse::getResults)
                .subscribeOn(Schedulers.io())
                .flatMap(Observable::from);
    }

    // Use public for now, will change to private after referenced
    public Observable<FeedResponse<com.microsoft.azure.cosmosdb.Document>> executeQueryAsync(
            @NonNull SqlQuerySpec sqlQuerySpec, @NonNull String collectionName, @NonNull FeedOptions options) {
        final String selfLink = getCollectionLink(collectionName);

        return getAsyncDocumentClient()
                .queryDocuments(selfLink, sqlQuerySpec, options)
                .doOnNext(r -> log.debug("Query Document Async from {}", selfLink));
    }

    public <T> List<T> find(@NonNull DocumentQuery query, @NonNull Class<T> domainClass, String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        validateQuery(query, domainClass, collectionName);

        final com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));

        return this.executeQuery(sqlQuerySpec, isCrossPartitionQuery, domainClass, collectionName);
    }

    public <T> Boolean exists(@NonNull DocumentQuery query, @NonNull Class<T> domainClass, String collectionName) {
        return this.find(query, domainClass, collectionName).size() > 0;
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

    private List<Document> findDocuments(@NonNull DocumentQuery query, @NonNull Class<?> domainClass,
                                         @NonNull String collectionName) {
        final com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        final com.microsoft.azure.documentdb.FeedResponse<Document> response =
                executeQuery(sqlQuerySpec, isCrossPartitionQuery, collectionName);

        return response.getQueryIterable().toList();
    }

    private void deleteDocument(@NonNull Document document, @NonNull List<String> partitionKeyNames) {
        try {
            final com.microsoft.azure.documentdb.RequestOptions options =
                    new com.microsoft.azure.documentdb.RequestOptions();

            Assert.isTrue(partitionKeyNames.size() <= 1, "Only one Partition is supported.");

            if (!partitionKeyNames.isEmpty() && StringUtils.hasText(partitionKeyNames.get(0))) {
                options.setPartitionKey(
                        new com.microsoft.azure.documentdb.PartitionKey(document.get(partitionKeyNames.get(0))));
            }

            getDocumentClient().deleteDocument(document.getSelfLink(), options);
        } catch (com.microsoft.azure.documentdb.DocumentClientException e) {
            throw new DocumentDBAccessException("Failed to delete document: " + document.getSelfLink(), e);
        }
    }

    /**
     * Delete the DocumentQuery, need to query the domains at first, then delete the document
     * from the result.
     * The cosmosdb Sql API do _NOT_ support DELETE query, we cannot add one DeleteQueryGenerator.
     *
     * @param query          The representation for query method.
     * @param domainClass    Class of domain
     * @param collectionName Collection Name of database
     * @param <T>
     * @return All the deleted documents as List.
     */
    @Override
    public <T> List<T> delete(@NonNull DocumentQuery query, @NonNull Class<T> domainClass,
                              @NonNull String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        final List<Document> results = findDocuments(query, domainClass, collectionName);
        final List<String> partitionKeyName = getPartitionKeyNames(domainClass);

        results.forEach(d -> deleteDocument(d, partitionKeyName));

        return results.stream().map(d -> getConverter().read(domainClass, d)).collect(Collectors.toList());
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

        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generateAsync(query);

        return executeQueryAsync(sqlQuerySpec, collectionName, feedOptions).first()
                .map(r -> {
                    log.debug(r.getResults().size() + " documents returned.");
                    final List<T> result = r.getResults().stream().filter(d -> d != null)
                            .map(d -> mappingDocumentDbConverter.readAsync(domainClass, d))
                            .collect(Collectors.toList());

                    final DocumentDbPageRequest pageRequest = DocumentDbPageRequest.of(pageable.getPageNumber(),
                            pageable.getPageSize(), r.getResponseContinuation());

                    return new PageImpl<>(result, pageRequest, count(query, domainClass, collectionName));
                });
    }

    @Override
    public long count(String collectionName) {
        Assert.hasText(collectionName, "collectionName should not be empty");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        final com.microsoft.azure.documentdb.SqlQuerySpec querySpec = new CountQueryGenerator().generate(query);

        return getCountValue(querySpec, true, collectionName);
    }

    @Override
    public <T> long count(DocumentQuery query, Class<T> domainClass, String collectionName) {
        Assert.notNull(domainClass, "domainClass should not be null");
        Assert.hasText(collectionName, "collectionName should not be empty");

        final com.microsoft.azure.documentdb.SqlQuerySpec querySpec = new CountQueryGenerator().generate(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));

        return getCountValue(querySpec, isCrossPartitionQuery, collectionName);
    }

    private long getCountValue(com.microsoft.azure.documentdb.SqlQuerySpec querySpec, boolean isCrossPartitionQuery,
                               String collectionName) {
        final com.microsoft.azure.documentdb.FeedResponse<Document> feedResponse =
                executeQuery(querySpec, isCrossPartitionQuery, collectionName);
        final Object value = feedResponse.getQueryIterable().toList().get(0).getHashMap().get(COUNT_VALUE_KEY);

        if (value instanceof Integer) {
            return Long.valueOf((Integer) value);
        } else if (value instanceof Long) {
            return (Long) value;
        } else {
            throw new IllegalStateException("Unexpected value type " + value.getClass() + " of value: " + value);
        }
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
}
