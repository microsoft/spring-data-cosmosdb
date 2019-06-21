/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository.support;

import com.microsoft.azure.documentdb.ExcludedPath;
import com.microsoft.azure.documentdb.IncludedPath;
import com.microsoft.azure.documentdb.IndexingMode;
import com.microsoft.azure.documentdb.IndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentIndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class DocumentDbEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {

    private Field id;
    private Field partitionKeyField;
    private String collectionName;
    private Integer requestUnit;
    private Integer timeToLive;
    private IndexingPolicy indexingPolicy;

    public DocumentDbEntityInformation(Class<T> domainClass) {
        super(domainClass);

        this.id = getIdField(domainClass);
        ReflectionUtils.makeAccessible(this.id);

        this.collectionName = getCollectionName(domainClass);
        this.partitionKeyField = getPartitionKeyField(domainClass);
        if (this.partitionKeyField != null) {
            ReflectionUtils.makeAccessible(this.partitionKeyField);
        }

        this.requestUnit = getRequestUnit(domainClass);
        this.timeToLive = getTimeToLive(domainClass);
        this.indexingPolicy = getIndexingPolicy(domainClass);
    }

    @SuppressWarnings("unchecked")
    public ID getId(T entity) {
        return (ID) ReflectionUtils.getField(id, entity);
    }

    public Field getIdField() {
        return this.id;
    }

    @SuppressWarnings("unchecked")
    public Class<ID> getIdType() {
        return (Class<ID>) id.getType();
    }

    public String getCollectionName() {
        return this.collectionName;
    }

    public Integer getRequestUnit() {
        return this.requestUnit;
    }

    public Integer getTimeToLive() {
        return this.timeToLive;
    }

    @NonNull
    public IndexingPolicy getIndexingPolicy() {
        return this.indexingPolicy;
    }

    public String getPartitionKeyFieldName() {
        if (partitionKeyField == null) {
            return null;
        } else {
            final PartitionKey partitionKey = partitionKeyField.getAnnotation(PartitionKey.class);
            return partitionKey.value().equals("") ? partitionKeyField.getName() : partitionKey.value();
        }
    }

    public String getPartitionKeyFieldValue(T entity) {
        return partitionKeyField == null ? null : (String) ReflectionUtils.getField(partitionKeyField, entity);
    }

    private IndexingPolicy getIndexingPolicy(Class<?> domainClass) {
        final IndexingPolicy policy = new IndexingPolicy();

        policy.setAutomatic(this.getIndexingPolicyAutomatic(domainClass));
        policy.setIndexingMode(this.getIndexingPolicyMode(domainClass));
        policy.setIncludedPaths(this.getIndexingPolicyIncludePaths(domainClass));
        policy.setExcludedPaths(this.getIndexingPolicyExcludePaths(domainClass));

        return policy;
    }

    private Field getIdField(Class<?> domainClass) {
        final Field idField;
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(domainClass, Id.class);

        if (fields.isEmpty()) {
            idField = ReflectionUtils.findField(getJavaType(), Constants.ID_PROPERTY_NAME);
        } else if (fields.size() == 1) {
            idField = fields.get(0);
        } else {
            throw new IllegalArgumentException("only one field with @Id annotation!");
        }

        if (idField == null) {
            throw new IllegalArgumentException("domain should contain @Id field or field named id");
        } else if (idField.getType() != String.class
                && idField.getType() != Integer.class && idField.getType() != int.class) {
            throw new IllegalArgumentException("type of id field must be String or Integer");
        }

        return idField;
    }

    private String getCollectionName(Class<?> domainClass) {
        String customCollectionName = domainClass.getSimpleName();

        final Document annotation = domainClass.getAnnotation(Document.class);

        if (annotation != null && annotation.collection() != null && !annotation.collection().isEmpty()) {
            customCollectionName = annotation.collection();
        }

        return customCollectionName;
    }

    private Field getPartitionKeyField(Class<?> domainClass) {
        Field partitionKey = null;

        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(domainClass, PartitionKey.class);

        if (fields.size() == 1) {
            partitionKey = fields.get(0);
        } else if (fields.size() > 1) {
            throw new IllegalArgumentException("Azure Cosmos DB supports only one partition key, " +
                    "only one field with @PartitionKey annotation!");
        }

        if (partitionKey != null && partitionKey.getType() != String.class) {
            throw new IllegalArgumentException("type of PartitionKey field must be String");
        }
        return partitionKey;
    }

    private Integer getRequestUnit(Class<?> domainClass) {
        Integer ru = Integer.parseInt(Constants.DEFAULT_REQUEST_UNIT);
        final Document annotation = domainClass.getAnnotation(Document.class);

        if (annotation != null && annotation.ru() != null && !annotation.ru().isEmpty()) {
            ru = Integer.parseInt(annotation.ru());
        }
        return ru;
    }

    private Integer getTimeToLive(Class<T> domainClass) {
        Integer ttl = Constants.DEFAULT_TIME_TO_LIVE;
        final Document annotation = domainClass.getAnnotation(Document.class);

        if (annotation != null) {
            ttl = annotation.timeToLive();
        }

        return ttl;
    }


    private Boolean getIndexingPolicyAutomatic(Class<?> domainClass) {
        Boolean isAutomatic = Boolean.valueOf(Constants.DEFAULT_INDEXINGPOLICY_AUTOMATIC);
        final DocumentIndexingPolicy annotation = domainClass.getAnnotation(DocumentIndexingPolicy.class);

        if (annotation != null) {
            isAutomatic = Boolean.valueOf(annotation.automatic());
        }

        return isAutomatic;
    }

    private IndexingMode getIndexingPolicyMode(Class<?> domainClass) {
        IndexingMode mode = Constants.DEFAULT_INDEXINGPOLICY_MODE;
        final DocumentIndexingPolicy annotation = domainClass.getAnnotation(DocumentIndexingPolicy.class);

        if (annotation != null) {
            mode = annotation.mode();
        }

        return mode;
    }

    private Collection<IncludedPath> getIndexingPolicyIncludePaths(Class<?> domainClass) {
        final Collection<IncludedPath> pathsCollection = new ArrayList<>();
        final DocumentIndexingPolicy annotation = domainClass.getAnnotation(DocumentIndexingPolicy.class);

        if (annotation == null || annotation.includePaths() == null || annotation.includePaths().length == 0) {
            return null; // Align the default value of IndexingPolicy
        }

        final String[] rawPaths = annotation.includePaths();

        for (final String path : rawPaths) {
            pathsCollection.add(new IncludedPath(path));
        }

        return pathsCollection;
    }

    private Collection<ExcludedPath> getIndexingPolicyExcludePaths(Class<?> domainClass) {
        final Collection<ExcludedPath> pathsCollection = new ArrayList<>();
        final DocumentIndexingPolicy annotation = domainClass.getAnnotation(DocumentIndexingPolicy.class);

        if (annotation == null || annotation.excludePaths() == null || annotation.excludePaths().length == 0) {
            return null; // Align the default value of IndexingPolicy
        }

        final String[] rawPaths = annotation.excludePaths();

        for (final String path : rawPaths) {
            pathsCollection.add(new ExcludedPath(path));
        }

        return pathsCollection;
    }
}

