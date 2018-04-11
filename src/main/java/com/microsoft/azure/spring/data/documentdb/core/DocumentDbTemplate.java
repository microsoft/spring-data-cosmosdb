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
import com.microsoft.azure.spring.data.documentdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.documentdb.core.query.Criteria;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;

import com.microsoft.azure.spring.data.documentdb.exception.DatabaseCreationException;
import com.microsoft.azure.spring.data.documentdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.documentdb.exception.IllegalCollectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class DocumentDbTemplate implements DocumentDbOperations, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDbTemplate.class);

    private static final Map<CriteriaType, String> operatorLookup;
    static {
        final Map<CriteriaType, String> init = new HashMap<>();
        init.put(CriteriaType.IS_EQUAL,                 "r.@?=@@");
        init.put(CriteriaType.IS_LESS_THAN,             "r.@?<=@@");
        init.put(CriteriaType.IS_LESS_THAN_OR_EQUAL,    "r.@?<@@");
        init.put(CriteriaType.IS_GREATER_THAN,          "r.@?<=@@");
        init.put(CriteriaType.IS_GREATER_THAN_OR_EQUAL, "r.@?<@@");
        init.put(CriteriaType.BETWEEN,                  "r.@? BETWEEN @@ AND @@");
        init.put(CriteriaType.CONTAINING,               "CONTAINS(r.@?, @@)");
        init.put(CriteriaType.WITHIN,                   "CONTAINS(@@, r.@?)");
        init.put(CriteriaType.ENDING_WITH,              "ENDSWITH(r.@?, @@)");
        init.put(CriteriaType.EXISTS,                   "IS_DEFINED(r.@?)");
        init.put(CriteriaType.IS_EMPTY,                 "LENGTH(r.@?)=0");
        init.put(CriteriaType.IS_NULL,                  "IS_NULL(r.@?)");
        init.put(CriteriaType.STARTING_WITH,            "STARTSWITH(r.@?, @@)");
        operatorLookup = Collections.unmodifiableMap(init);
    }    

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

    public <T> T insert(T objectToSave, PartitionKey partitionKey) {
        return insert(getCollectionName(objectToSave.getClass()),
                objectToSave,
                partitionKey);
    }


    public <T> T insert(String collectionName,
                        T objectToSave,
                        PartitionKey partitionKey) {
        final Document document = new Document();
        mappingDocumentDbConverter.write(objectToSave, document);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("execute createDocument in database {} collection {}",
                    this.databaseName, collectionName);
        }

        try {
            documentDbFactory.getDocumentClient()
                    .createDocument(getCollectionLink(this.databaseName, collectionName), document,
                            getRequestOptions(partitionKey, null), false);
            return objectToSave;
        } catch (DocumentClientException e) {
            throw new DocumentDBAccessException("insert exception", e);
        }
    }

    public <T> T findById(Object id,
                          Class<T> entityClass) {
        return findById(getCollectionName(entityClass),
                id,
                entityClass);
    }

    public <T> T findById(String collectionName,
                          Object id,
                          Class<T> entityClass) {

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

    public <T> void upsert(T object, Object id, PartitionKey partitionKey) {
        upsert(getCollectionName(object.getClass()), object, id, partitionKey);
    }


    public <T> void upsert(String collectionName, T object, Object id, PartitionKey partitionKey) {
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
                    getRequestOptions(partitionKey, null), false);
        } catch (DocumentClientException ex) {
            throw new DocumentDBAccessException("Failed to upsert document to database.", ex);
        }
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        return findAll(getCollectionName(entityClass), entityClass);
    }


    public <T> List<T> findAll(String collectionName,
                               final Class<T> entityClass) {
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

    public DocumentCollection createCollection(String dbName,
                                               String collectionName,
                                               RequestOptions collectionOptions,
                                               String partitionKeyFieldName,
                                               IndexingPolicy policy) {
        DocumentCollection collection = new DocumentCollection();
        collection.setId(collectionName);

        if (partitionKeyFieldName != null && !partitionKeyFieldName.isEmpty()) {
            final PartitionKeyDefinition partitionKeyDefinition = new PartitionKeyDefinition();
            final ArrayList<String> paths = new ArrayList<>();

            paths.add(getPartitionKeyPath(partitionKeyFieldName));
            partitionKeyDefinition.setPaths(paths);
            collection.setPartitionKey(partitionKeyDefinition);
        }

        if (policy != null) {
            collection.setIndexingPolicy(policy);
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
            throw new DocumentDBAccessException("createCollection exception", e);
        }

    }

    public DocumentCollection createCollectionIfNotExists(String collectionName,
                                                          String partitionKeyFieldName,
                                                          Integer requestUnit,
                                                          IndexingPolicy policy) {
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
            return createCollection(this.databaseName, collectionName, requestOptions, partitionKeyFieldName, policy);
        }
    }

    public <T> void deleteById(String collectionName,
                               Object id,
                               Class<T> domainClass,
                               PartitionKey partitionKey) {
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

    public <T> List<T> find(Query query, Class<T> domainClass, String collectionName) {
        final SqlQuerySpec sqlQuerySpec = createSqlQuerySpec(query, domainClass);

        // TODO (wepa) Collection link should be created locally without accessing database,
        // but currently exception will be thrown if not fetching collection url from database.
        // Run repository integration test to reproduce.
        final DocumentCollection collection = getDocCollection(collectionName);
        final FeedOptions feedOptions = new FeedOptions();

        final Optional<Object> partitionKeyValue = getPartitionKeyValue(query, domainClass);
        if (!partitionKeyValue.isPresent()) {
            feedOptions.setEnableCrossPartitionQuery(true);
        }

        final List<Document> results = documentDbFactory.getDocumentClient()
                .queryDocuments(collection.getSelfLink(),
                        sqlQuerySpec, feedOptions)
                .getQueryIterable().toList();

        final List<T> entities = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            final T entity = mappingDocumentDbConverter.read(domainClass, results.get(i));
            entities.add(entity);
        }
        return entities;
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

    private <T> SqlQuerySpec createSqlQuerySpec(Query query, Class<T> entityClass) {

        String queryStr = "SELECT * FROM ROOT r WHERE ";

        final SqlParameterCollection parameterCollection = new SqlParameterCollection();

        final CriteriaProcessor criteriaWriter = new CriteriaProcessor(
                new DocumentDbEntityInformation(entityClass).getId().getName());
        
        query.getCriteria().accept(criteriaWriter);
        
        queryStr += criteriaWriter.getConstraintString();
        
        for (final Map.Entry<String, Object> entry : criteriaWriter.getParameterMap().entrySet()) {

            parameterCollection.add(new SqlParameter(entry.getKey(),
                    mappingDocumentDbConverter.mapToDocumentDBValue(entry.getValue())));
        }

        return new SqlQuerySpec(queryStr, parameterCollection);
    }

    @Override
    public MappingDocumentDbConverter getConverter() {
        return this.mappingDocumentDbConverter;
    }

    @Override
    public <T> List<T> delete(Query query, Class<T> entityClass, String collectionName) {
        final SqlQuerySpec sqlQuerySpec = createSqlQuerySpec(query, entityClass);
        final Optional<Object> partitionKeyValue = getPartitionKeyValue(query, entityClass);

        final DocumentCollection collection = getDocCollection(collectionName);
        final FeedOptions feedOptions = new FeedOptions();
        if (!partitionKeyValue.isPresent()) {
            feedOptions.setEnableCrossPartitionQuery(true);
        }

        final List<Document> results = documentDbFactory.getDocumentClient()
                .queryDocuments(collection.getSelfLink(), sqlQuerySpec, feedOptions).getQueryIterable().toList();

        final RequestOptions options = new RequestOptions();
        if (partitionKeyValue.isPresent()) {
            options.setPartitionKey(new PartitionKey(partitionKeyValue.get()));
        }

        final List<T> deletedResult = new ArrayList<>();
        for (final Document document : results) {
            try {
                documentDbFactory.getDocumentClient().deleteDocument((document).getSelfLink(), options);
                deletedResult.add(getConverter().read(entityClass, document));
            } catch (DocumentClientException e) {
                throw new DocumentDBAccessException(
                        String.format("Failed to delete document [%s]", (document).getSelfLink()), e);
            }
        }

        return deletedResult;
    }

    private <T> Optional<Object> getPartitionKeyValue(Query query, Class<T> domainClass) {

        if (query == null) {
            return Optional.empty();
        }

        final Optional<String> partitionKeyName = getPartitionKeyField(domainClass);

        if (!partitionKeyName.isPresent()) {
            return Optional.empty();
        }

        final CriteriaKeyFinder finder = new CriteriaKeyFinder(partitionKeyName.get());
        
        query.getCriteria().accept(finder);
        
        final Criteria matched = finder.matchedCriteria();
        
        if (matched == null || matched.getCriteriaValues().size() != 1) {
            return Optional.empty();
        }

        return Optional.of(matched.getCriteriaValues().get(0));
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<String> getPartitionKeyField(Class<T> domainClass) {
        final DocumentDbEntityInformation entityInfo = new DocumentDbEntityInformation(domainClass);
        if (entityInfo.getPartitionKeyFieldName() == null) {
            return Optional.empty();
        }

        return Optional.of(entityInfo.getPartitionKeyFieldName());
    }

    private static class CriteriaKeyFinder implements Criteria.Visitor {

        private String keyToFind;
        private Criteria matchedCriteria = null;
        
        CriteriaKeyFinder(String keyToFind) {
            this.keyToFind = keyToFind;
        }
        
        @Override
        public void visitCriteria(Criteria criteria) {

            if (criteria.getCriteriaSubject() != null && criteria.getCriteriaSubject().equals(keyToFind)) {
                matchedCriteria = criteria;
            }
            
            for (final Criteria val : criteria.getCriteriaList()) {
                val.accept(this);
            }
        }
        
        public Criteria matchedCriteria() {
            return matchedCriteria;
        }
        
    }
    
    private static class CriteriaProcessor implements Criteria.Visitor {

        private final String entityClassIdFieldName;
        private final List<String> constraint = new ArrayList<>();
        private final Map<String, Object> parameterMap = new HashMap<>();
        private int parameterCount = 1;

        public CriteriaProcessor(String entityClassIdFieldName) {
            this.entityClassIdFieldName =  entityClassIdFieldName;
        }
        
        @Override
        public void visitCriteria(Criteria criteria) {

            switch(criteria.getCriteriaType()) {
                
                case AND_CONDITION:
    
                    criteria.getCriteriaList().get(0).accept(this);
                    constraint.add(" AND ");
                    criteria.getCriteriaList().get(1).accept(this);
                    
                    return;
                    
                case OR_CONDITION:
                
                    constraint.add("(");
                    criteria.getCriteriaList().get(0).accept(this);
                    constraint.add(") OR (");
                    criteria.getCriteriaList().get(1).accept(this);
                    constraint.add(")");
                    
                    return;


                case IN:
                {
                    if (criteria.getCriteriaValues().size() != 1 ||
                            !(criteria.getCriteriaValues().get(0) instanceof List<?>)) {

                        throw new IllegalArgumentException("value provided for IN parameter is not a list value");
                    }

                    final List<?> listItems = (List<?>) criteria.getCriteriaValues().get(0);

                    final String template = "r.@? IN (" + 
                            String.join(",", Collections.nCopies(listItems.size(), "@@")) + ")";

                    processTemplate(template, criteria, listItems);
                    
                    return;
                }
                    
                default:
                {
                    
                    if (operatorLookup.containsKey(criteria.getCriteriaType())) {

                        final String template = operatorLookup.get(criteria.getCriteriaType());
                        
                        final List<Object> values = criteria.getCriteriaValues();

                        processTemplate(template, criteria, values);
                        
                        return;
                    }
                }
            }
            
            throw new IllegalArgumentException("Unsupported condition");
        }

        public String getConstraintString() {
            
            return String.join("", constraint);
        }
        
        public Map<String, Object> getParameterMap() {
            
            return parameterMap;
        }
        
        private void processTemplate(String template, Criteria criteria, List<?> values) {

            String workingTemplate = template;
            
            if (criteria.shouldIgnoreCase()) {
                
                workingTemplate = workingTemplate.replaceAll("@\\?", "LOWER(@\\?)");
                workingTemplate = workingTemplate.replaceAll("@@", "LOWER(@@)");
            }

            // If the field name used in any of the criteria parameters matches the id field specified for
            // entity (with @Id) then replace the field name with "id"
            
            workingTemplate = workingTemplate.replaceAll("@\\?", 
                    criteria.getCriteriaSubject() != null && 
                            criteria.getCriteriaSubject()
                                .equals(entityClassIdFieldName) ? "id" : criteria.getCriteriaSubject());
            
            int index = 0;
            while (workingTemplate.indexOf("@@") != -1 && index < values.size()) {
                final String parameterName = "@p" + parameterCount++;
                workingTemplate = workingTemplate.replaceFirst("@@", parameterName);
                parameterMap.put(parameterName, values.get(index));
                ++index;
            }
            
            if (index != values.size()) {
                
                throw new IllegalArgumentException(
                        "incorrect number of values for " + criteria.getCriteriaType() + " operation ");
            }                    

            // If the operator is inverted then reverse the boolean outcome of the test
            
            if (criteria.isNegated()) {
                workingTemplate = "NOT (" + template + ")";
            }
            constraint.add(workingTemplate);
            
            return;            
        }
    }
}
