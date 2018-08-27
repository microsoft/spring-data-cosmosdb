/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.documentdb.internal.HttpConstants;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.CountQueryGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.exception.DatabaseCreationException;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.exception.IllegalCollectionException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class DocumentDbTemplate implements DocumentDbOperations, ApplicationContextAware {
    private static final String COUNT_VALUE_KEY = "_aggregate";

    private final DocumentDbFactory documentDbFactory;
    private final MappingDocumentDbConverter mappingDocumentDbConverter;
    private final String databaseName;

    private Database databaseCache;
    private List<String> collectionCache;

    public DocumentDbTemplate(DocumentDbFactory documentDbFactory,
                              MappingDocumentDbConverter mappingDocumentDbConverter,
                              String dbName) {
        Assert.notNull(documentDbFactory, "DocumentDbFactory must not be null!");
        Assert.notNull(mappingDocumentDbConverter, "MappingDocumentDbConverter must not be null!");

        this.databaseName = dbName;
        this.documentDbFactory = documentDbFactory;
        this.mappingDocumentDbConverter = mappingDocumentDbConverter;
        this.collectionCache = new ArrayList<>();
    }

    public DocumentDbTemplate(DocumentClient client, MappingDocumentDbConverter mappingDocumentDbConverter,
                              String dbName) {
        this(new DocumentDbFactory(client), mappingDocumentDbConverter, dbName);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    private DocumentClient getDocumentClient() {
        return this.documentDbFactory.getDocumentClient();
    }

    public <T> T insert(T objectToSave, PartitionKey partitionKey) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getCollectionName(objectToSave.getClass()), objectToSave, partitionKey);
    }

    public <T> T insert(String collectionName, T objectToSave, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(objectToSave, "objectToSave should not be null");

        final Document document = mappingDocumentDbConverter.writeDoc(objectToSave);

        log.debug("execute createDocument in database {} collection {}", this.databaseName, collectionName);

        try {
            final Resource result = getDocumentClient()
                    .createDocument(getCollectionLink(this.databaseName, collectionName), document,
                            getRequestOptions(partitionKey, null), false).getResource();

            if (result instanceof Document) {
                final Document documentInserted = (Document) result;
                @SuppressWarnings("unchecked") final Class<T> domainClass = (Class<T>) objectToSave.getClass();

                return mappingDocumentDbConverter.read(domainClass, documentInserted);
            } else {
                return null;
            }
        } catch (DocumentClientException e) {
            throw new DocumentDBAccessException("insert exception", e);
        }
    }

    public <T> T findById(Object id, Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findById(getCollectionName(entityClass), id, entityClass);
    }

    public <T> T findById(String collectionName, Object id, Class<T> entityClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(entityClass, "entityClass should not be null");
        assertValidId(id);

        try {
            final RequestOptions options = new RequestOptions();
            final String documentLink = getDocumentLink(this.databaseName, collectionName, id);
            final Resource resource = getDocumentClient().readDocument(documentLink, options).getResource();

            if (resource instanceof Document) {
                final Document document = (Document) resource;
                return mappingDocumentDbConverter.read(entityClass, document);
            } else {
                return null;
            }
        } catch (DocumentClientException e) {
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

            log.debug("execute upsert document in database {} collection {}", this.databaseName, collectionName);

            final String collectionLink = getCollectionSelfLink(collectionName);
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

        try {
            getDocumentClient().deleteCollection(getCollectionLink(this.databaseName, collectionName), null);
            this.collectionCache.remove(collectionName);
        } catch (DocumentClientException ex) {
            throw new DocumentDBAccessException("failed to delete collection: " + collectionName, ex);
        }
    }

    public String getCollectionName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return new DocumentDbEntityInformation<>(domainClass).getCollectionName();
    }

    private Database createDatabaseIfNotExists(String dbName) {
        try {
            final List<Database> dbList = getDocumentClient()
                    .queryDatabases(new SqlQuerySpec("SELECT * FROM root r WHERE r.id=@id",
                            new SqlParameterCollection(new SqlParameter("@id", dbName))), null)
                    .getQueryIterable().toList();

            if (!dbList.isEmpty()) {
                return dbList.get(0);
            } else {
                // create new database
                final Database db = new Database();
                db.setId(dbName);

                log.debug("execute createDatabase {}", dbName);

                final Resource resource = getDocumentClient().createDatabase(db, null).getResource();

                if (resource instanceof Database) {
                    return (Database) resource;
                } else {
                    final String errorMessage = MessageFormat.format(
                            "create database {0} and get unexpected result: {1}", dbName, resource.getSelfLink());

                    log.error(errorMessage);
                    throw new DatabaseCreationException(errorMessage);
                }
            }
        } catch (DocumentClientException ex) {
            throw new DocumentDBAccessException("createOrGetDatabase exception", ex);
        }
    }

    private DocumentCollection createCollection(@NonNull String dbName, String partitionKeyFieldName,
                                                @NonNull DocumentDbEntityInformation information) {
        DocumentCollection collection = new DocumentCollection();
        final String collectionName = information.getCollectionName();
        final IndexingPolicy policy = information.getIndexingPolicy();
        final Integer timeToLive = information.getTimeToLive();
        final RequestOptions requestOptions = getRequestOptions(null, information.getRequestUnit());

        collection.setId(collectionName);
        collection.setIndexingPolicy(policy);

        if (information.getIndexingPolicy().getAutomatic()) {
            collection.setDefaultTimeToLive(timeToLive); // If not Automatic, setDefaultTimeToLive is invalid
        }

        if (partitionKeyFieldName != null && !partitionKeyFieldName.isEmpty()) {
            final PartitionKeyDefinition partitionKeyDefinition = new PartitionKeyDefinition();
            final ArrayList<String> paths = new ArrayList<>();

            paths.add(getPartitionKeyPath(partitionKeyFieldName));
            partitionKeyDefinition.setPaths(paths);
            collection.setPartitionKey(partitionKeyDefinition);
        }

        log.debug("execute createCollection in database {} collection {}", dbName, collectionName);

        try {
            final Resource resource = getDocumentClient()
                    .createCollection(getDatabaseLink(dbName), collection, requestOptions)
                    .getResource();
            if (resource instanceof DocumentCollection) {
                collection = (DocumentCollection) resource;
            }
            return collection;
        } catch (DocumentClientException e) {
            throw new DocumentDBAccessException("createCollection exception", e);
        }
    }

    @Override
    public DocumentCollection createCollectionIfNotExists(@NonNull DocumentDbEntityInformation information,
                                                          String partitionKeyFieldName) {
        if (this.databaseCache == null) {
            this.databaseCache = createDatabaseIfNotExists(this.databaseName);
        }

        final String collectionName = information.getCollectionName();

        final List<DocumentCollection> collectionList = getDocumentClient()
                .queryCollections(getDatabaseLink(this.databaseName),
                        new SqlQuerySpec("SELECT * FROM root r WHERE r.id=@id",
                                new SqlParameterCollection(new SqlParameter("@id", collectionName))), null)
                .getQueryIterable().toList();

        if (!collectionList.isEmpty()) {
            return collectionList.get(0);
        } else {
            return createCollection(this.databaseName, partitionKeyFieldName, information);
        }
    }

    public void deleteById(String collectionName, Object id, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        assertValidId(id);

        log.debug("execute deleteById in database {} collection {}", this.databaseName, collectionName);

        try {
            getDocumentClient().deleteDocument(getDocumentLink(this.databaseName, collectionName, id.toString()),
                    getRequestOptions(partitionKey, null));
        } catch (DocumentClientException ex) {
            throw new DocumentDBAccessException("deleteById exception", ex);
        }
    }

    private String getDatabaseLink(String databaseName) {
        return "dbs/" + databaseName;
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

    private <T> List<T> executeQuery(@NonNull SqlQuerySpec sqlQuerySpec, boolean isCrossPartition,
                                     @NonNull Class<T> domainClass, String collectionName) {
        final FeedResponse<Document> feedResponse = executeQuery(sqlQuerySpec, isCrossPartition, collectionName);
        final List<Document> result = feedResponse.getQueryIterable().toList();

        return result.stream().map(r -> getConverter().read(domainClass, r)).collect(Collectors.toList());
    }

    private FeedResponse<Document> executeQuery(@NonNull SqlQuerySpec sqlQuerySpec, boolean isCrossPartition,
                                                String collectionName) {
        final FeedOptions feedOptions = new FeedOptions();
        final String selfLink = getCollectionSelfLink(collectionName);

        feedOptions.setEnableCrossPartitionQuery(isCrossPartition);

        return getDocumentClient().queryDocuments(selfLink, sqlQuerySpec, feedOptions);
    }

    private FeedResponse<Document> executeQuery(@NonNull SqlQuerySpec sqlQuerySpec, FeedOptions feedOptions,
                                                String collectionName) {
        final String selfLink = getCollectionSelfLink(collectionName);

        return getDocumentClient().queryDocuments(selfLink, sqlQuerySpec, feedOptions);
    }

    public <T> List<T> find(@NonNull DocumentQuery query, @NonNull Class<T> domainClass, String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        if (query.getSort().isSorted()) { // avoiding unnecessary query with DocumentCollection
            query.validateSort(domainClass, isCollectionSupportSortByString(getDocCollection(collectionName)));
        }
        if (query.getCriteria().getType().equals(CriteriaType.STARTS_WITH)) {
            query.validateStartsWith(domainClass, isCollectionSupportStartsWith(getDocCollection(collectionName)));
        }

        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));

        return this.executeQuery(sqlQuerySpec, isCrossPartitionQuery, domainClass, collectionName);
    }

    private Predicate<Index> isIndexingSupportSortByString() {
        return index -> {
            if (index instanceof RangeIndex) {
                final RangeIndex rangeIndex = (RangeIndex) index;
                return rangeIndex.getDataType() == DataType.String && rangeIndex.getPrecision() == -1;
            }

            return false;
        };
    }

    private boolean isCollectionSupportSortByString(@NonNull DocumentCollection collection) {
        final IndexingPolicy policy = collection.getIndexingPolicy();
        final List<Index> indices = new ArrayList<>();

        policy.getIncludedPaths().forEach(p -> indices.addAll(p.getIndexes()));

        return indices.stream().anyMatch(isIndexingSupportSortByString());
    }

    private Predicate<Index> isIndexingSupportStartsWith() {
        return index -> {
            if (index instanceof RangeIndex) {
                final RangeIndex rangeIndex = (RangeIndex) index;
                return rangeIndex.getDataType() == DataType.String;
            }

            return false;
        };
    }

    private boolean isCollectionSupportStartsWith(@NonNull DocumentCollection collection) {
        final IndexingPolicy policy = collection.getIndexingPolicy();
        final List<Index> indices = new ArrayList<>();

        policy.getIncludedPaths().forEach(p -> indices.addAll(p.getIndexes()));

        return indices.stream().anyMatch(isIndexingSupportStartsWith());
    }

    private List<Document> findDocuments(@NonNull DocumentQuery query, @NonNull Class<?> domainClass,
                                         @NonNull String collectionName) {
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
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
        } catch (DocumentClientException e) {
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

        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generate(query);
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
        final SqlQuerySpec querySpec = new CountQueryGenerator().generate(query);

        return getCountValue(querySpec, true, collectionName);
    }

    @Override
    public <T> long count(DocumentQuery query, Class<T> domainClass, String collectionName) {
        Assert.notNull(domainClass, "domainClass should not be null");
        Assert.hasText(collectionName, "collectionName should not be empty");

        final SqlQuerySpec querySpec = new CountQueryGenerator().generate(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));

        return getCountValue(querySpec, isCrossPartitionQuery, collectionName);
    }

    private long getCountValue(SqlQuerySpec querySpec, boolean isCrossPartitionQuery, String collectionName) {
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

    private DocumentCollection getDocCollection(String collectionName) {
        final String databaseLink = getDatabaseLink(this.databaseName);
        final SqlParameterCollection parameters = new SqlParameterCollection(new SqlParameter("@id", collectionName));
        final SqlQuerySpec querySpec = new SqlQuerySpec("SELECT * FROM ROOT r WHERE r.id=@id", parameters);
        final List<DocumentCollection> collections = getDocumentClient().queryCollections(databaseLink, querySpec, null)
                .getQueryIterable().toList();

        if (collections.size() != 1) {
            throw new IllegalCollectionException("expect only one collection: " + collectionName
                    + " in database: " + this.databaseName + ", but found " + collections.size());
        }

        return collections.get(0);
    }

    private String getCollectionSelfLink(@NonNull String collectionName) {
        return String.format("dbs/%s/colls/%s", this.databaseName, collectionName);
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
