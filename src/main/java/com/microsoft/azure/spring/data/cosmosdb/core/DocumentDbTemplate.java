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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbOperationValidator.validate;

@Slf4j
public class DocumentDbTemplate implements DocumentDbOperations, ApplicationContextAware {

    private static final String COUNT_VALUE_KEY = "_aggregate";

    private final DocumentDbFactory documentDbFactory;

    private final MappingDocumentDbConverter mappingDocumentDbConverter;

    private final String dbName;

    private final String dbLink;

    private AsyncDocumentClient asyncDocumentClient;

    public DocumentDbTemplate(DocumentDbFactory documentDbFactory,
                              MappingDocumentDbConverter mappingDocumentDbConverter, String dbName) {
        Assert.notNull(documentDbFactory, "DocumentDbFactory must not be null!");
        Assert.notNull(mappingDocumentDbConverter, "MappingDocumentDbConverter must not be null!");

        this.dbName = dbName;
        this.dbLink = "dbs/" + dbName;
        this.documentDbFactory = documentDbFactory;
        this.mappingDocumentDbConverter = mappingDocumentDbConverter;
    }

    private AsyncDocumentClient getAsyncDocumentClient() {
        if (this.asyncDocumentClient == null) {
            this.asyncDocumentClient = this.documentDbFactory.getAsyncDocumentClient();
        }

        return this.asyncDocumentClient;
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    @Override
    public <T> Observable<T> insertAsync(String collectionName, T entity, PartitionKey key) {
        validate(collectionName, entity);

        @SuppressWarnings("unchecked") final Class<T> entityClass = (Class<T>) entity.getClass();
        final String collectionLink = getCollectionLink(collectionName);
        final Document document = mappingDocumentDbConverter.toCosmosdbDocument(entity);

        return getAsyncDocumentClient()
                .createDocument(collectionLink, document, getRequestOptions(key, null), false)
                .doOnNext(r -> log.debug("Create Document Async from {}.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to insert entity", e);
                })
                .map(ResourceResponse::getResource)
                .map(d -> this.mappingDocumentDbConverter.read(entityClass, d));
    }

    @Override
    public <T> T insert(String collectionName, T entity, PartitionKey key) {
        validate(collectionName, entity);

        return insertAsync(collectionName, entity, key).toBlocking().single();
    }

    @Override
    public <T> Observable<T> upsertAsync(String collectionName, T entity, PartitionKey key) {
        validate(collectionName, entity);

        final String collectionLink = getCollectionLink(collectionName);
        final Document document = mappingDocumentDbConverter.toCosmosdbDocument(entity);

        return getAsyncDocumentClient()
                .upsertDocument(collectionLink, document, getRequestOptions(key, null), false)
                .doOnNext(r -> log.debug("Upsert Document Async from {}.", collectionLink))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("failed to upsert entity", e);
                })
                .map(d -> entity);
    }

    @Override
    public <T> void upsert(String collectionName, T entity, PartitionKey key) {
        validate(collectionName, entity);

        upsertAsync(collectionName, entity, key).toCompletable().await();
    }

    @Override
    public <T> Observable<T> findByIdAsync(String collectionName, Object id, Class<T> entityClass, PartitionKey key) {
        validate(id, collectionName, entityClass);

        final RequestOptions options = new RequestOptions();
        final String collectionLink = getCollectionLink(collectionName);

        options.setPartitionKey(key);

        return getAsyncDocumentClient()
                .readDocument(getDocumentLink(collectionName, id), options)
                .doOnNext(r -> log.debug("Read Document Async from {}.", collectionLink))
                .map(ResourceResponse::getResource)
                .map(d -> this.mappingDocumentDbConverter.read(entityClass, d));
    }

    @Override
    public <T> Optional<T> findById(String collectionName, Object id, Class<T> entityClass, PartitionKey key) {
        validate(id, collectionName, entityClass);

        try {
            return Optional.of(findByIdAsync(collectionName, id, entityClass, key).toBlocking().single());
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof DocumentClientException) {
                final DocumentClientException exception = (DocumentClientException) cause;
                if (exception.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    return Optional.empty();
                }
            }

            throw new DocumentDBAccessException("Failed to read Document", e);
        }
    }

    private Observable<Document> deleteDocuments(DocumentQuery query, String collectionName, String partitionKeyName) {
        validate(query, collectionName);

        return findDocuments(query, collectionName, partitionKeyName)
                .doOnSubscribe(() -> log.debug("Delete Documents Async."))
                .onErrorResumeNext(e -> {
                    throw new DocumentDBAccessException("Failed to delete Document", e);
                })
                .flatMap(d -> {
                    final RequestOptions options = new RequestOptions();

                    if (StringUtils.hasText(partitionKeyName)) {
                        options.setPartitionKey(new PartitionKey(d.get(partitionKeyName)));
                    }

                    return getAsyncDocumentClient()
                            .deleteDocument(d.getSelfLink(), options)
                            .map(r -> d);
                });
    }

    @Override
    public <T> Observable<T> deleteAllAsync(String collectionName, String partitionKeyName) {
        validate(collectionName);

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        return deleteDocuments(query, collectionName, partitionKeyName)
                .flatMap(d -> Observable.empty());
    }

    @Override
    public void deleteAll(String collectionName, String partitionKeyName) {
        validate(collectionName);

        deleteAllAsync(collectionName, partitionKeyName).toCompletable().await();
    }

    @Override
    public <T> Observable<T> findAllAsync(String collectionName, Class<T> entityClass, String partitionKeyName) {
        validate(collectionName, entityClass);

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        return findDocuments(query, collectionName, partitionKeyName)
                .doOnSubscribe(() -> log.debug("Find all documents for Class {} async", entityClass.getSimpleName()))
                .onErrorReturn(e -> {
                    throw new DocumentDBAccessException("Failed to find all documents.", e);
                })
                .map(d -> this.mappingDocumentDbConverter.read(entityClass, d));
    }

    @Override
    public <T> List<T> findAll(String collectionName, Class<T> entityClass, String partitionKeyName) {
        validate(collectionName, entityClass);

        return findAllAsync(collectionName, entityClass, partitionKeyName).toList().toBlocking().single();
    }

    @Override
    public Observable<String> deleteCollectionAsync(String collectionName) {
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

    @Override
    public void deleteCollection(String collectionName) {
        validate(collectionName);

        deleteCollectionAsync(collectionName).toCompletable().await();
    }

    @Override
    public Observable<Object> deleteByIdAsync(String collectionName, Object id, PartitionKey key) {
        validate(id, collectionName);

        final String documentLink = getDocumentLink(collectionName, id.toString());
        final RequestOptions options = getRequestOptions(key, null);

        return getAsyncDocumentClient()
                .deleteDocument(documentLink, options)
                .doOnNext(r -> log.debug("Delete Document Async from {}", documentLink))
                .map(r -> id);
    }

    @Override
    public void deleteById(String collectionName, Object id, PartitionKey partitionKey) {
        validate(id, collectionName);

        try {
            deleteByIdAsync(collectionName, id, partitionKey).toCompletable().await();
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof DocumentClientException) {
                final DocumentClientException exception = (DocumentClientException) cause;
                if (exception.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    return;
                }
            }

            throw new DocumentDBAccessException("Failed to delete Document", e);
        }
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

    private DocumentCollection createDocumentCollectionInstance(DocumentDbEntityInformation information) {
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

    private Optional<DocumentCollection> getDocumentCollection(String collectionName) {
        final String sqlQuery = "SELECT * FROM r WHERE r.id=@id";
        final SqlParameter sqlParameter = new SqlParameter("@id", collectionName);
        final SqlParameterCollection sqlParameterCollection = new SqlParameterCollection(sqlParameter);
        final SqlQuerySpec sqlQuerySpec = new SqlQuerySpec(sqlQuery, sqlParameterCollection);

        try {
            final DocumentCollection collection = getAsyncDocumentClient()
                    .queryCollections(dbLink, sqlQuerySpec, null)
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
    public DocumentCollection createCollectionIfNotExists(DocumentDbEntityInformation information) {
        validate(information);

        final String collectionName = information.getCollectionName();

        createDatabaseIfNotExists();

        return getDocumentCollection(collectionName).orElseGet(() -> {
            final DocumentCollection collection = createDocumentCollectionInstance(information);

            collection.setId(collectionName);

            log.info("Creating Collection [{}] of Database [{}] ...", collectionName, this.dbName);

            return getAsyncDocumentClient()
                    .createCollection(this.dbLink, collection, null)
                    .map(ResourceResponse::getResource)
                    .toBlocking()
                    .single();
        });
    }

    private Observable<Document> executeQueryDocument(SqlQuerySpec sqlQuerySpec, String collectionName,
                                                      FeedOptions options) {
        return executeQuery(sqlQuerySpec, collectionName, options)
                .map(FeedResponse::getResults)
                .flatMap(Observable::from);
    }

    private Observable<FeedResponse<Document>> executeQuery(SqlQuerySpec sqlQuerySpec, String collectionName,
                                                            FeedOptions options) {
        final String selfLink = getCollectionLink(collectionName);

        return getAsyncDocumentClient()
                .queryDocuments(selfLink, sqlQuerySpec, options)
                .doOnNext(r -> log.debug("Query Document Async from {}", selfLink));
    }

    @Override
    public <T> Observable<T> findAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                       String partitionKeyName) {
        validate(query, collectionName, entityClass);

        final FeedOptions options = new FeedOptions();
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);

        options.setEnableCrossPartitionQuery(query.isCrossPartitionQuery(partitionKeyName));

        return executeQueryDocument(sqlQuerySpec, collectionName, options)
                .map(d -> getConverter().read(entityClass, d));
    }

    @Override
    public <T> List<T> find(DocumentQuery query, String collectionName, Class<T> entityClass, String partitionKeyName) {
        validate(query, collectionName, entityClass);
        validateQuery(query, entityClass, collectionName);

        return Lists.newArrayList(
                findAsync(query, collectionName, entityClass, partitionKeyName).toBlocking().getIterator());
    }

    @Override
    public Observable<Boolean> existsAsync(DocumentQuery query, String collectionName, Class<?> entityClass,
                                           String partitionKeyName) {
        validate(query, collectionName, entityClass);

        return countAsync(query, collectionName, entityClass, partitionKeyName).map(c -> c > 0).single();
    }

    @Override
    public Boolean exists(DocumentQuery query, String collectionName, Class<?> entityClass, String partitionKeyName) {
        validate(query, collectionName, entityClass);

        return existsAsync(query, collectionName, entityClass, partitionKeyName).toBlocking().single();
    }

    private void validateQuery(DocumentQuery query, Class<?> entityClass, String collectionName) {
        if (!query.getSort().isSorted() && !query.getCriteriaByType(CriteriaType.STARTS_WITH).isPresent()) {
            return;
        }

        final Optional<DocumentCollection> optional = getDocumentCollection(collectionName);

        Assert.isTrue(optional.isPresent(), "Collection should be created already.");

        if (query.getSort().isSorted()) { // avoiding unnecessary query with DocumentCollection
            query.validateSort(entityClass, QueryValidator.isCollectionSupportSortByString(optional.get()));
        }
        if (query.getCriteriaByType(CriteriaType.STARTS_WITH).isPresent()) {
            query.validateStartsWith(QueryValidator.isCollectionSupportStartsWith(optional.get()));
        }
    }

    private Observable<Document> findDocuments(DocumentQuery query, String collectionName, String partitionKeyName) {
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final FeedOptions options = new FeedOptions();
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(partitionKeyName);

        options.setEnableCrossPartitionQuery(isCrossPartitionQuery);

        return executeQueryDocument(sqlQuerySpec, collectionName, options);
    }

    @Override
    public <T> Observable<T> deleteAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                         String partitionKeyName) {
        validate(query, collectionName, entityClass);

        return deleteDocuments(query, collectionName, partitionKeyName)
                .map(d -> getConverter().read(entityClass, d));
    }

    /**
     * Delete the DocumentQuery, need to query the entity at first, then delete the document from the result.
     * The cosmosdb Sql API do _NOT_ support DELETE query, we cannot add one DeleteQueryGenerator.
     *
     * @param query          The representation for query method.
     * @param entityClass    Class of entity
     * @param collectionName Collection Name of database
     * @param <T>            Entity type
     * @return All the deleted documents as List.
     */
    @Override
    public <T> List<T> delete(DocumentQuery query, String collectionName, Class<T> entityClass,
                              String partitionKeyNames) {
        validate(query, collectionName, entityClass);

        return Lists.newArrayList(
                deleteAsync(query, collectionName, entityClass, partitionKeyNames).toBlocking().getIterator());
    }

    @Override
    public <T> Observable<Page<T>> findAllAsync(Pageable pageable, String collectionName, Class<T> entityClass,
                                                String partitionKeyName) {
        validate(pageable, collectionName, entityClass);

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(pageable);

        return paginationQueryAsync(query, collectionName, entityClass, partitionKeyName);
    }

    @Override
    public <T> Page<T> findAll(Pageable pageable, String collectionName, Class<T> entityClass,
                               String partitionKeyName) {
        validate(collectionName, entityClass);

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(pageable);

        return paginationQuery(query, collectionName, entityClass, partitionKeyName);
    }

    @Override
    public <T> Observable<Page<T>> paginationQueryAsync(DocumentQuery query, String collectionName,
                                                        Class<T> entityClass, String partitionKeyName) {
        validate(query, collectionName, entityClass, query.getPageable());

        final Pageable pageable = query.getPageable();
        final FeedOptions options = new FeedOptions();

        if (pageable instanceof DocumentDbPageRequest) {
            options.setRequestContinuation(((DocumentDbPageRequest) pageable).getRequestContinuation());
        }

        options.setMaxItemCount(pageable.getPageSize());
        options.setEnableCrossPartitionQuery(query.isCrossPartitionQuery(partitionKeyName));

        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final Observable<Long> countObservable = countAsync(query, collectionName, entityClass, partitionKeyName);
        final Observable<FeedResponse<Document>> responseObservable
                = executeQuery(sqlQuerySpec, collectionName, options).first();

        return Observable.zip(countObservable, responseObservable, PageResponse::new)
                .doOnError(e -> {
                    throw new DocumentDBAccessException("Failed to execute pagination query.", e);
                })
                .map(pageResponse -> {
                    final FeedResponse<Document> r = pageResponse.getResponse();
                    final long count = pageResponse.getCount();

                    log.debug(r.getResults().size() + " documents returned.");
                    final List<T> result = r.getResults()
                            .stream().filter(Objects::nonNull)
                            .map(d -> mappingDocumentDbConverter.read(entityClass, d))
                            .collect(Collectors.toList());

                    final DocumentDbPageRequest pageRequest = DocumentDbPageRequest.of(pageable.getPageNumber(),
                            pageable.getPageSize(), r.getResponseContinuation());

                    return new PageImpl<>(result, pageRequest, count);
                });
    }

    @Override
    public <T> Page<T> paginationQuery(DocumentQuery query, String collectionName, Class<T> entityClass,
                                       String partitionKeyName) {
        validate(query, collectionName, entityClass);

        return paginationQueryAsync(query, collectionName, entityClass, partitionKeyName).toBlocking().single();
    }

    private Observable<Long> getCountValue(DocumentQuery query, boolean isCrossPartitionQuery, String collectionName) {
        final SqlQuerySpec querySpec = new CountQueryGenerator().generate(query);
        final FeedOptions options = new FeedOptions();

        options.setEnableCrossPartitionQuery(isCrossPartitionQuery);

        return executeQuery(querySpec, collectionName, options)
                .map(r -> r.getResults().get(0).getLong(COUNT_VALUE_KEY));
    }

    @Override
    public Observable<Long> countAsync(String collectionName) {
        validate(collectionName);

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        // TODO: not sure if no partitionKey in entity with true Query will result in DocumentClientException.
        return getCountValue(query, true, collectionName);
    }

    @Override
    public long count(String collectionName) {
        validate(collectionName);

        return countAsync(collectionName).toBlocking().single();
    }

    @Override
    public Observable<Long> countAsync(DocumentQuery query, String collectionName, Class<?> entityClass,
                                       String partitionKeyName) {
        validate(query, collectionName, entityClass);

        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(partitionKeyName);

        return getCountValue(query, isCrossPartitionQuery, collectionName);
    }

    @Override
    public long count(DocumentQuery query, String collectionName, Class<?> entityClass, String partitionKeyName) {
        validate(query, collectionName, entityClass);

        return countAsync(query, collectionName, entityClass, partitionKeyName).toBlocking().single();
    }

    @Override
    public MappingDocumentDbConverter getConverter() {
        return this.mappingDocumentDbConverter;
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
