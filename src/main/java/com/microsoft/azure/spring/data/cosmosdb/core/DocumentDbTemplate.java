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
import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.QuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.exception.DatabaseCreationException;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.exception.IllegalCollectionException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public DocumentDbTemplate(DocumentClient client, MappingDocumentDbConverter mappingDocumentDbConverter,
                              String dbName) {

        this(new DocumentDbFactory(client), mappingDocumentDbConverter, dbName);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    public <T> T insert(T objectToSave, PartitionKey partitionKey) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getCollectionName(objectToSave.getClass()),
                objectToSave,
                partitionKey);
    }

    public <T> T insert(String collectionName, T objectToSave, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(objectToSave, "objectToSave should not be null");

        final Document document = mappingDocumentDbConverter.writeDoc(objectToSave);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute createDocument in database {} collection {}",
                    this.databaseName, collectionName);
        }

        try {
            final Resource result = documentDbFactory.getDocumentClient()
                    .createDocument(getCollectionLink(this.databaseName, collectionName), document,
                            getRequestOptions(partitionKey, null),
                            false).getResource();

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
        assertValidId(id);
        Assert.notNull(entityClass, "entityClass should not be null");

        return findById(getCollectionName(entityClass),
                id,
                entityClass);
    }

    public <T> T findById(String collectionName, Object id, Class<T> entityClass) {
        assertValidId(id);
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(entityClass, "entityClass should not be null");

        try {
            final Resource resource = documentDbFactory.getDocumentClient()
                    .readDocument(getDocumentLink(this.databaseName, collectionName, id),
                            new RequestOptions()).getResource();

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

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("execute upsert document in database {} collection {}", this.databaseName, collectionName);
            }

            documentDbFactory.getDocumentClient().upsertDocument(
                    getCollectionLink(this.databaseName, collectionName),
                    originalDoc,
                    getRequestOptions(partitionKey, null), false);
        } catch (DocumentClientException ex) {
            throw new DocumentDBAccessException("Failed to upsert document to database.", ex);
        }
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findAll(getCollectionName(entityClass), entityClass);
    }


    public <T> List<T> findAll(String collectionName, final Class<T> entityClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(entityClass, "entityClass should not be null");

        final List<DocumentCollection> collections = documentDbFactory.getDocumentClient().
                queryCollections(
                        getDatabaseLink(this.databaseName),
                        new SqlQuerySpec("SELECT * FROM ROOT r WHERE r.id=@id",
                                new SqlParameterCollection(new SqlParameter("@id", collectionName))), null)
                .getQueryIterable().toList();

        if (collections.size() != 1) {
            throw new IllegalCollectionException("expect only one collection: " + collectionName
                    + " in database: " + this.databaseName + ", but found " + collections.size());
        }

        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.setEnableCrossPartitionQuery(true);

        final SqlQuerySpec sqlQuerySpec = new SqlQuerySpec("SELECT * FROM root c");

        final List<Document> results = documentDbFactory.getDocumentClient()
                .queryDocuments(collections.get(0).getSelfLink(), sqlQuerySpec, feedOptions)
                .getQueryIterable().toList();

        final List<T> entities = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            final T entity = mappingDocumentDbConverter.read(entityClass, results.get(i));
            entities.add(entity);
        }

        return entities;
    }

    public void deleteAll(String collectionName) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute deleteCollection in database {} collection {}",
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
                throw new DocumentDBAccessException("deleteAll exception", ex);
            }
        }
    }

    public String getCollectionName(Class<?> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

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
                    final String errorMessage = MessageFormat.format(
                            "create database {0} and get unexpected result: {1}", dbName, resource.getSelfLink());

                    LOGGER.error(errorMessage);
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute createCollection in database {} collection {}", dbName, collectionName);
        }

        try {
            final Resource resource = documentDbFactory.getDocumentClient()
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

        final List<DocumentCollection> collectionList = documentDbFactory.getDocumentClient()
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

    public <T> void deleteById(String collectionName, Object id, PartitionKey partitionKey) {
        assertValidId(id);
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute deleteById in database {} collection {}", this.databaseName, collectionName);
        }

        try {
            documentDbFactory.getDocumentClient().deleteDocument(
                    getDocumentLink(this.databaseName, collectionName, id.toString()),
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

    public <T> List<T> find(@NonNull DocumentQuery query, @NonNull Class<T> domainClass,
                            @NonNull String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        final FeedOptions feedOptions = new FeedOptions();
        final DocumentCollection collection = getDocCollection(collectionName);
        final Optional<String> partitionKeyName = getPartitionKeyField(domainClass);
        final QuerySpecGenerator generator = new FindQuerySpecGenerator(domainClass);
        final Optional<Criteria> partitionCriteria = query.getSubjectCriteria(partitionKeyName.orElse(""));

        feedOptions.setEnableCrossPartitionQuery(!partitionCriteria.isPresent());

        final SqlQuerySpec sqlQuerySpec = generator.generate(query);
        final List<Document> result = documentDbFactory.getDocumentClient()
                .queryDocuments(collection.getSelfLink(), sqlQuerySpec, feedOptions).getQueryIterable().toList();

        return result.stream().map(r -> getConverter().read(domainClass, r)).collect(Collectors.toList());
    }

    @Override
    public <T> List<T> delete(@NonNull DocumentQuery query, @NonNull Class<T> domainClass,
                              @NonNull String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        final FeedOptions feedOptions = new FeedOptions();
        final DocumentCollection collection = getDocCollection(collectionName);
        final Optional<String> partitionKeyName = getPartitionKeyField(domainClass);
        final Optional<Criteria> partitionCriteria = query.getSubjectCriteria(partitionKeyName.orElse(""));
        final QuerySpecGenerator generator = new FindQuerySpecGenerator(domainClass);

        feedOptions.setEnableCrossPartitionQuery(!partitionCriteria.isPresent());

        final SqlQuerySpec sqlQuerySpec = generator.generate(query);
        final List<Document> results = documentDbFactory.getDocumentClient()
                .queryDocuments(collection.getSelfLink(), sqlQuerySpec, feedOptions).getQueryIterable().toList();
        final RequestOptions options = new RequestOptions();

        partitionCriteria.ifPresent(c -> options.setPartitionKey(new PartitionKey(c.getSubjectValues().get(0))));

        final List<T> deletedResult = new ArrayList<>();

        for (final Document d : results) {
            try {
                documentDbFactory.getDocumentClient().deleteDocument(d.getSelfLink(), options);
                deletedResult.add(getConverter().read(domainClass, d));
            } catch (DocumentClientException e) {
                throw new DocumentDBAccessException(String.format("Failed to delete document %s", d.getSelfLink()), e);
            }
        }

        return deletedResult;
    }

    private DocumentCollection getDocCollection(String collectionName) {
        final List<DocumentCollection> collections = documentDbFactory.getDocumentClient().
                queryCollections(
                        getDatabaseLink(this.databaseName),
                        new SqlQuerySpec("SELECT * FROM ROOT r WHERE r.id=@id",
                                new SqlParameterCollection(new SqlParameter("@id", collectionName))), null)
                .getQueryIterable().toList();

        if (collections.size() != 1) {
            throw new IllegalCollectionException("expect only one collection: " + collectionName
                    + " in database: " + this.databaseName + ", but found " + collections.size());
        }

        return collections.get(0);
    }

    @Override
    public MappingDocumentDbConverter getConverter() {
        return this.mappingDocumentDbConverter;
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<String> getPartitionKeyField(Class<T> domainClass) {
        final DocumentDbEntityInformation entityInfo = new DocumentDbEntityInformation(domainClass);
        if (entityInfo.getPartitionKeyFieldName() == null) {
            return Optional.empty();
        }

        return Optional.of(entityInfo.getPartitionKeyFieldName());
    }

    private void assertValidId(Object id) {
        Assert.notNull(id, "id should not be null");
        if (id instanceof String) {
            Assert.hasText(id.toString(), "id should not be empty or only whitespaces.");
        }
    }
}
