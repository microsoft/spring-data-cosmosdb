/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.repository.support;


import com.microsoft.azure.spring.data.documentdb.core.DocumentDbOperations;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.spring.data.documentdb.repository.DocumentDbRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SimpleDocumentDbRepository<T, ID extends Serializable> implements DocumentDbRepository<T, ID> {

    private final DocumentDbOperations documentDbOperations;
    private final DocumentDbEntityInformation<T, ID> entityInformation;

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata,
                                      ApplicationContext applicationContext) {
        this.documentDbOperations = applicationContext.getBean(DocumentDbOperations.class);
        this.entityInformation = metadata;
    }

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata,
                                      DocumentDbOperations dbOperations) {
        this.documentDbOperations = dbOperations;
        this.entityInformation = metadata;
    }

    /**
     * save entity without partition
     *
     * @param entity to be saved
     * @param <S>
     * @return entity
     */
    @Override
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "entity must not be null");

        // create collection if not exists
        documentDbOperations.createCollectionIfNotExists(entityInformation.getCollectionName(),
                entityInformation.getPartitionKeyFieldName(),
                entityInformation.getRequestUnit(),
                entityInformation.getIndexingPolicy());

        // save entity
        if (entityInformation.isNew(entity)) {
            documentDbOperations.insert(entityInformation.getCollectionName(),
                    entity,
                    createKey(entityInformation.getPartitionKeyFieldValue(entity)));
        } else {
            documentDbOperations.upsert(entityInformation.getCollectionName(),
                    entity,
                    entityInformation.getId(entity),
                    createKey(entityInformation.getPartitionKeyFieldValue(entity)));
        }

        return entity;
    }

    private PartitionKey createKey(String partitionKeyValue) {
        if (StringUtils.isEmpty(partitionKeyValue)) {
            return null;
        }

        return new PartitionKey(partitionKeyValue);
    }

    /**
     * batch save entities
     *
     * @param entities
     * @param <S>
     * @return
     */
    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        // create collection if not exists
        documentDbOperations.createCollectionIfNotExists(entityInformation.getCollectionName(),
                entityInformation.getPartitionKeyFieldName(),
                entityInformation.getRequestUnit(),
                entityInformation.getIndexingPolicy());

        for (final S entity : entities) {
            save(entity);
        }

        return entities;
    }

    /**
     * find all entities from one collection without configuring partition key value
     *
     * @return
     */
    @Override
    public Iterable<T> findAll() {
        return documentDbOperations.findAll(entityInformation.getCollectionName(), entityInformation.getJavaType());
    }

    /**
     * find entities based on id list from one collection without partitions
     *
     * @param ids
     * @return
     */
    @Override
    public List<T> findAll(Iterable<ID> ids) {
        final List<T> entities = new ArrayList<T>();

        for (final ID id : ids) {
            final T entity = findOne(id);

            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    /**
     * find one entity per id without partitions
     *
     * @param id
     * @return
     */
    @Override
    public T findOne(ID id) {
        Assert.notNull(id, "id must not be null");
        return documentDbOperations.findById(
                entityInformation.getCollectionName(), id, entityInformation.getJavaType());
    }

    /**
     * return count of documents in one collection without partitions
     *
     * @return
     */
    @Override
    public long count() {
        return findAll().spliterator().getExactSizeIfKnown();
    }

    /**
     * delete one document per id without configuring partition key value
     *
     * @param id
     */
    @Override
    public void delete(ID id) {
        documentDbOperations.deleteById(entityInformation.getCollectionName(),
                id,
                entityInformation.getJavaType(),
                null);
    }

    /**
     * delete one document per entity
     *
     * @param entity
     */
    @Override
    public void delete(T entity) {
        final String paritionKeyValue = entityInformation.getPartitionKeyFieldValue(entity);

        documentDbOperations.deleteById(entityInformation.getCollectionName(),
                entityInformation.getId(entity),
                entityInformation.getJavaType(),
                paritionKeyValue == null ? null : new PartitionKey(paritionKeyValue));
    }

    /**
     * delete an collection
     */
    @Override
    public void deleteAll() {
        documentDbOperations.deleteAll(entityInformation.getCollectionName());
    }

    /**
     * delete list of entities without partitions
     *
     * @param entities
     */
    @Override
    public void delete(Iterable<? extends T> entities) {
        for (final T entity : entities) {
            delete(entity);
        }
    }

    /**
     * check if an entity exists per id without partition
     *
     * @param primaryKey
     * @return
     */
    @Override
    public boolean exists(ID primaryKey) {
        return findOne(primaryKey) != null;
    }
}
