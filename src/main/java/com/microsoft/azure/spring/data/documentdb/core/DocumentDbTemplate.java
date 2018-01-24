/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.core;

import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.documentdb.internal.HttpConstants;
import com.microsoft.azure.spring.data.documentdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.documentdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentDbTemplate implements DocumentDbOperations, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDbTemplate.class);

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

    public DocumentDbTemplate(DocumentClient client,
                              MappingDocumentDbConverter mappingDocumentDbConverter,
                              String dbName) {

        this(new DocumentDbFactory(client), mappingDocumentDbConverter, dbName);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    public <T> T insert(T objectToSave, String partitionKeyFieldValue) {
        return insert(getCollectionName(objectToSave.getClass()),
                objectToSave,
                partitionKeyFieldValue);
    }


    public <T> T insert(String collectionName,
                        T objectToSave,
                        String partitionKeyFieldValue) {
        final Document document = new Document();
        mappingDocumentDbConverter.write(objectToSave, document);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute createDocument in database {} collection {}",
                    this.databaseName, collectionName);
        }

        try {
            documentDbFactory.getDocumentClient()
                    .createDocument(getCollectionLink(this.databaseName, collectionName), document,
                            getRequestOptions(partitionKeyFieldValue, null), false);
            return objectToSave;
        } catch (DocumentClientException e) {
            throw new RuntimeException("insert exception", e);
        }
    }

    public <T> T findById(Object id,
                          Class<T> entityClass,
                          String partitionKeyFieldValue) {
        return findById(getCollectionName(entityClass),
                id,
                entityClass,
                partitionKeyFieldValue);
    }

    public <T> T findById(String collectionName,
                          Object id,
                          Class<T> entityClass,
                          String partitionKeyFieldValue) {

        try {
            final Resource resource = documentDbFactory.getDocumentClient()
                    .readDocument(getDocumentLink(this.databaseName, collectionName, (String) id),
                            getRequestOptions(partitionKeyFieldValue, null)).getResource();

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

            throw new RuntimeException("findById exception", e);
        }
    }

    public <T> void upsert(T object, Object id, String partitionKeyFieldValue) {
        upsert(getCollectionName(object.getClass()), object, id, partitionKeyFieldValue);
    }


    public <T> void upsert(String collectionName, T object, Object id, String partitionKeyFieldValue) {
        try {
            Document originalDoc = new Document();
            if (object instanceof Document) {
                originalDoc = (Document) object;
            } else {
                mappingDocumentDbConverter.write(object, originalDoc);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("execute upsert document in database {} collection {} with id {}",
                        this.databaseName, collectionName, id);
            }

            documentDbFactory.getDocumentClient().upsertDocument(
                    getCollectionLink(this.databaseName, collectionName),
                    originalDoc,
                    getRequestOptions(partitionKeyFieldValue, null), false);
        } catch (DocumentClientException ex) {
            throw new RuntimeException("Failed to upsert document to database.", ex);
        }
    }

    public <T> List<T> findAll(Class<T> entityClass,
                               String partitionKeyFieldName,
                               String partitionKeyFieldValue) {
        return findAll(getCollectionName(entityClass), entityClass, partitionKeyFieldName, partitionKeyFieldValue);
    }


    public <T> List<T> findAll(String collectionName,
                               final Class<T> entityClass,
                               String partitionKeyFieldName,
                               String partitionKeyFieldValue) {
        final List<DocumentCollection> collections = documentDbFactory.getDocumentClient().
                queryCollections(
                        getDatabaseLink(this.databaseName),
                        new SqlQuerySpec("SELECT * FROM ROOT r WHERE r.id=@id",
                                new SqlParameterCollection(new SqlParameter("@id", collectionName))), null)
                .getQueryIterable().toList();

        if (collections.size() != 1) {
            throw new RuntimeException("expect only one collection: " + collectionName
                    + " in database: " + this.databaseName + ", but found " + collections.size());
        }

        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.setEnableCrossPartitionQuery(true);

        SqlQuerySpec sqlQuerySpec = new SqlQuerySpec("SELECT * FROM root c");
        if (partitionKeyFieldName != null && !partitionKeyFieldName.isEmpty()) {
            sqlQuerySpec = new SqlQuerySpec("SELECT * FROM root c WHERE c." + partitionKeyFieldName + "=@partition",
                    new SqlParameterCollection(new SqlParameter("@partition", partitionKeyFieldValue)));
            feedOptions.setPartitionKey(new PartitionKey(partitionKeyFieldValue));
        }

        final List<Document> results = documentDbFactory.getDocumentClient()
                .queryDocuments(collections.get(0).getSelfLink(), sqlQuerySpec, feedOptions, partitionKeyFieldName)
                .getQueryIterable().toList();

        final List<T> entities = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            final T entity = mappingDocumentDbConverter.read(entityClass, results.get(i));
            entities.add(entity);
        }

        return entities;
    }

    public void deleteAll(String collectionName) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute deleteCollection in database {} collection {} with id {}",
                    this.databaseName, collectionName);
        }

        try {
            documentDbFactory.getDocumentClient()
                    .deleteCollection(getCollectionLink(this.databaseName, collectionName), null);
            if (this.collectionCache.contains(collectionName)) {
                this.collectionCache.remove(collectionName);
            }
        } catch (DocumentClientException ex) {
            if (ex.getStatusCode() == 404) {
                LOGGER.warn("deleteAll in database {} collection {} met NOTFOUND error {}",
                        this.databaseName, collectionName, ex.getMessage());
            } else {
                throw new RuntimeException("deleteAll exception", ex);
            }
        }
    }

    public String getCollectionName(Class<?> entityClass) {
        return entityClass.getSimpleName();
    }

    private Database createDatabaseIfNotExists(String dbName) {
        try {
            final List<Database> dbList = documentDbFactory.getDocumentClient()
                    .queryDatabases(new SqlQuerySpec("SELECT * FROM root r WHERE r.id=@id",
                            new SqlParameterCollection(new SqlParameter("@id", dbName))), null)
                    .getQueryIterable().toList();

            if (!dbList.isEmpty()) {
                return dbList.get(0);
            } else {
                // create new database
                final Database db = new Database();
                db.setId(dbName);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("execute createDatabase {}", dbName);
                }

                final Resource resource = documentDbFactory.getDocumentClient()
                        .createDatabase(db, null).getResource();

                if (resource instanceof Database) {
                    return (Database) resource;
                } else {
                    LOGGER.error("create database {} get unexpected result: {}" + resource.getSelfLink());
                    throw new RuntimeException("create database {} get unexpected result: " + resource.getSelfLink());
                }
            }
        } catch (DocumentClientException ex) {
            throw new RuntimeException("createOrGetDatabase exception", ex);
        }
    }

    public DocumentCollection createCollection(String collectionName,
                                               RequestOptions collectionOptions,
                                               String partitionKeyFieldName) {
        return createCollection(this.databaseName, collectionName, collectionOptions, partitionKeyFieldName);
    }

    public DocumentCollection createCollection(String dbName,
                                               String collectionName,
                                               RequestOptions collectionOptions,
                                               String partitionKeyFieldName) {
        DocumentCollection collection = new DocumentCollection();
        collection.setId(collectionName);

        if (partitionKeyFieldName != null && !partitionKeyFieldName.isEmpty()) {
            final PartitionKeyDefinition partitionKeyDefinition = new PartitionKeyDefinition();
            final ArrayList<String> paths = new ArrayList<String>();

            paths.add(getPartitionKeyPath(partitionKeyFieldName));
            partitionKeyDefinition.setPaths(paths);
            collection.setPartitionKey(partitionKeyDefinition);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute createCollection in database {} collection {}", dbName, collectionName);
        }

        try {
            final Resource resource = documentDbFactory.getDocumentClient()
                    .createCollection(getDatabaseLink(dbName), collection, collectionOptions)
                    .getResource();
            if (resource instanceof DocumentCollection) {
                collection = (DocumentCollection) resource;
            }
            return collection;
        } catch (DocumentClientException e) {
            throw new RuntimeException("createCollection exception", e);
        }

    }

    public DocumentCollection createCollectionIfNotExists(String collectionName,
                                                          String partitionKeyFieldName,
                                                          Integer requestUnit) {
        if (this.databaseCache == null) {
            this.databaseCache = createDatabaseIfNotExists(this.databaseName);
        }

        final List<DocumentCollection> collectionList = documentDbFactory.getDocumentClient()
                .queryCollections(getDatabaseLink(this.databaseName),
                        new SqlQuerySpec("SELECT * FROM root r WHERE r.id=@id",
                                new SqlParameterCollection(new SqlParameter("@id", collectionName))), null)
                .getQueryIterable().toList();

        if (!collectionList.isEmpty()) {
            return collectionList.get(0);
        } else {
            final RequestOptions requestOptions = getRequestOptions(null, requestUnit);
            return createCollection(this.databaseName, collectionName, requestOptions, partitionKeyFieldName);
        }
    }

    public <T> void deleteById(String collectionName,
                               Object id,
                               Class<T> domainClass,
                               String partitionKeyFieldValue) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute deleteById in database {} collection {}", this.databaseName, collectionName);
        }

        try {
            documentDbFactory.getDocumentClient().deleteDocument(
                    getDocumentLink(this.databaseName, collectionName, id.toString()),
                    getRequestOptions(partitionKeyFieldValue, null));

        } catch (DocumentClientException ex) {
            throw new RuntimeException("deleteById exception", ex);
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

    private RequestOptions getRequestOptions(String partitionKeyValue, Integer requestUnit) {
        if ((partitionKeyValue == null || partitionKeyValue.isEmpty()) && requestUnit == null) {
            return null;
        }

        final RequestOptions requestOptions = new RequestOptions();
        if (!(partitionKeyValue == null || partitionKeyValue.isEmpty())) {
            requestOptions.setPartitionKey(new PartitionKey(partitionKeyValue));
        }
        if (requestUnit != null) {
            requestOptions.setOfferThroughput(requestUnit);
        }

        return requestOptions;
    }

  public <T> List<T> find(Query query, Class<T> domainClass, String collectionName) {
        final SqlQuerySpec sqlQuerySpec = createSqlQuerySpec(query);

        final List<DocumentCollection> collections = documentDbFactory.getDocumentClient().
                queryCollections(
                        getDatabaseLink(this.databaseName),
                        new SqlQuerySpec("SELECT * FROM ROOT r WHERE r.id=@id",
                                new SqlParameterCollection(new SqlParameter("@id", collectionName))), null)
                .getQueryIterable().toList();

        if (collections.size() != 1) {
            throw new RuntimeException("expect only one collection: " + collectionName
                    + " in database: " + this.databaseName + ", but found " + collections.size());
        }

        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.setEnableCrossPartitionQuery(true);
        final List<Document> results = documentDbFactory.getDocumentClient()
                .queryDocuments(collections.get(0).getSelfLink(),
                        sqlQuerySpec, feedOptions)
                .getQueryIterable().toList();

        final List<T> entities = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            final T entity = mappingDocumentDbConverter.read(domainClass, results.get(i));
            entities.add(entity);
        }
        return entities;
    }

    private static SqlQuerySpec createSqlQuerySpec(Query query) {
        String queryStr = "SELECT * FROM ROOT r WHERE r.";
        final SqlParameterCollection parameterCollection = new SqlParameterCollection();

        for (final Map.Entry<String, Object> entry : query.getCriteria().entrySet()) {
            queryStr += entry.getKey() + "=@" + entry.getKey();
            parameterCollection.add(new SqlParameter("@" + entry.getKey(), entry.getValue()));
        }
        return new SqlQuerySpec(queryStr, parameterCollection);
    }

    @Override
    public MappingDocumentDbConverter getConverter() {
        return this.mappingDocumentDbConverter;
    }
}
