/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.cosmosdb.Database;
import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.IndexingPolicy;
import com.microsoft.azure.cosmosdb.PartitionKeyDefinition;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.SqlParameter;
import com.microsoft.azure.cosmosdb.SqlParameterCollection;
import com.microsoft.azure.cosmosdb.SqlQuerySpec;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.FeedOptions;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.documentdb.Resource;
import com.microsoft.azure.documentdb.internal.HttpConstants;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

    public <T> T insert(T objectToSave, PartitionKey partitionKey) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getCollectionName(objectToSave.getClass()), objectToSave, partitionKey);
    }

    public <T> T insert(String collectionName, T objectToSave, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(objectToSave, "objectToSave should not be null");

        final Document document = mappingDocumentDbConverter.writeDoc(objectToSave);

        log.debug("execute createDocument in database {} collection {}", this.dbName, collectionName);

        try {
            final Resource result = getDocumentClient()
                    .createDocument(getCollectionLink(this.dbName, collectionName), document,
                            getRequestOptions(partitionKey, null), false).getResource();

            if (result instanceof Document) {
                final Document documentInserted = (Document) result;
                @SuppressWarnings("unchecked") final Class<T> domainClass = (Class<T>) objectToSave.getClass();

                return mappingDocumentDbConverter.read(domainClass, documentInserted);
            } else {
                return null;
            }
        } catch (com.microsoft.azure.documentdb.DocumentClientException e) {
            throw new DocumentDBAccessException("insert exception", e);
        }
    }

    public <T> T findById(Object id, Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findById(getCollectionName(entityClass), id, entityClass);
    }

    private boolean isIdFieldAsPartitionKey(@NonNull Class<?> domainClass) {
        @SuppressWarnings("unchecked") final DocumentDbEntityInformation information
                = new DocumentDbEntityInformation(domainClass);
        final String partitionKeyName = information.getPartitionKeyFieldName();
        final String idName = information.getIdField().getName();

        return partitionKeyName != null && partitionKeyName.equals(idName);
    }

    public <T> T findById(String collectionName, Object id, Class<T> domainClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domainClass, "entityClass should not be null");
        assertValidId(id);

        try {
            final RequestOptions options = new RequestOptions();

            if (isIdFieldAsPartitionKey(domainClass)) {
                options.setPartitionKey(new PartitionKey(id));
            }

            final String documentLink = getDocumentLink(this.dbName, collectionName, id);
            final Resource document = getDocumentClient().readDocument(documentLink, options).getResource();

            if (document instanceof Document) {
                return mappingDocumentDbConverter.read(domainClass, (Document) document);
            } else {
                return null;
            }
        } catch (com.microsoft.azure.documentdb.DocumentClientException e) {
            if (e.getStatusCode() == HttpConstants.StatusCodes.NOTFOUND) {
                return null;
            }

            throw new DocumentDBAccessException("findById exception", e);
        }
    }

    public <T> void upsert(T object, PartitionKey partitionKey) {
        Assert.notNull(object, "Upsert object should not be null");

        upsert(getCollectionName(object.getClass()), object, partitionKey);
    }

    public <T> void upsert(String collectionName, T object, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(object, "Upsert object should not be null");

        try {
            Document originalDoc;

            if (object instanceof Document) {
                originalDoc = (Document) object;
            } else {
                originalDoc = mappingDocumentDbConverter.writeDoc(object);
            }

            log.debug("execute upsert document in database {} collection {}", this.dbName, collectionName);

            final String collectionLink = toCollectionSelfLink(collectionName);
            final RequestOptions options = getRequestOptions(partitionKey, null);

            getDocumentClient().upsertDocument(collectionLink, originalDoc, options, false);
        } catch (DocumentClientException ex) {
            throw new DocumentDBAccessException("Failed to upsert document to database.", ex);
        }
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

        getAsyncDocumentClient().deleteCollection(getCollectionLink(this.dbName, collectionName), null)
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

    public void deleteById(String collectionName, Object id, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        assertValidId(id);

        log.debug("execute deleteById in database {} collection {}", this.dbName, collectionName);

        try {
            getDocumentClient().deleteDocument(getDocumentLink(this.dbName, collectionName, id.toString()),
                    getRequestOptions(partitionKey, null));
        } catch (com.microsoft.azure.documentdb.DocumentClientException ex) {
            throw new DocumentDBAccessException("deleteById exception", ex);
        }
    }

    private String getDatabaseLink(String databaseName) {
        return "/dbs/" + databaseName;
    }

    private String getCollectionLink(String databaseName, String collectionName) {
        return getDatabaseLink(databaseName) + "/colls/" + collectionName;
    }

    private String getDocumentLink(String databaseName, String collectionName, Object documentId) {
        return getCollectionLink(databaseName, collectionName) + "/docs/" + documentId;
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
        final FeedResponse<Document> feedResponse = executeQuery(sqlQuerySpec, isCrossPartition, collectionName);
        final List<Document> result = feedResponse.getQueryIterable().toList();

        return result.stream().map(r -> getConverter().read(domainClass, r)).collect(Collectors.toList());
    }

    private FeedResponse<Document> executeQuery(@NonNull com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec,
                                                boolean isCrossPartition, String collectionName) {
        final FeedOptions feedOptions = new FeedOptions();
        final String selfLink = toCollectionSelfLink(collectionName);

        feedOptions.setEnableCrossPartitionQuery(isCrossPartition);

        return getDocumentClient().queryDocuments(selfLink, sqlQuerySpec, feedOptions);
    }

    private FeedResponse<Document> executeQuery(@NonNull com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec,
                                                FeedOptions feedOptions, String collectionName) {
        final String selfLink = toCollectionSelfLink(collectionName);

        return getDocumentClient().queryDocuments(selfLink, sqlQuerySpec, feedOptions);
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
        final FeedResponse<Document> response = executeQuery(sqlQuerySpec, isCrossPartitionQuery, collectionName);

        return response.getQueryIterable().toList();
    }

    private void deleteDocument(@NonNull Document document, @NonNull List<String> partitionKeyNames) {
        try {
            final RequestOptions options = new RequestOptions();

            Assert.isTrue(partitionKeyNames.size() <= 1, "Only one Partition is supported.");

            if (!partitionKeyNames.isEmpty() && StringUtils.hasText(partitionKeyNames.get(0))) {
                options.setPartitionKey(new PartitionKey(document.get(partitionKeyNames.get(0))));
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
    public <T> Page<T> paginationQuery(DocumentQuery query, Class<T> domainClass, String collectionName) {
        Assert.isTrue(query.getPageable().getPageSize() > 0, "pageable should have page size larger than 0");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        final Pageable pageable = query.getPageable();
        final FeedOptions feedOptions = new FeedOptions();
        if (pageable instanceof DocumentDbPageRequest) {
            feedOptions.setRequestContinuation(((DocumentDbPageRequest) pageable).getRequestContinuation());
        }

        feedOptions.setPageSize(pageable.getPageSize());
        feedOptions.setEnableCrossPartitionQuery(query.isCrossPartitionQuery(getPartitionKeyNames(domainClass)));

        final com.microsoft.azure.documentdb.SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final FeedResponse<Document> response = executeQuery(sqlQuerySpec, feedOptions, collectionName);

        final Iterator<Document> it = response.getQueryIterator();

        final List<T> result = new ArrayList<>();
        for (int index = 0; it.hasNext() && index < pageable.getPageSize(); index++) {
            // Limit iterator as inner iterator will automatically fetch the next page
            final Document doc = it.next();
            if (doc == null) {
                continue;
            }

            final T entity = mappingDocumentDbConverter.read(domainClass, doc);
            result.add(entity);
        }

        final DocumentDbPageRequest pageRequest = DocumentDbPageRequest.of(pageable.getPageNumber(),
                pageable.getPageSize(),
                response.getResponseContinuation());

        return new PageImpl<>(result, pageRequest, count(query, domainClass, collectionName));
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
        final FeedResponse<Document> feedResponse = executeQuery(querySpec, isCrossPartitionQuery, collectionName);
        final Object value = feedResponse.getQueryIterable().toList().get(0).getHashMap().get(COUNT_VALUE_KEY);

        if (value instanceof Integer) {
            return Long.valueOf((Integer) value);
        } else if (value instanceof Long) {
            return (Long) value;
        } else {
            throw new IllegalStateException("Unexpected value type " + value.getClass() + " of value: " + value);
        }
    }

    private String toCollectionSelfLink(@NonNull String collectionName) {
        return String.format("dbs/%s/colls/%s", this.dbName, collectionName);
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
