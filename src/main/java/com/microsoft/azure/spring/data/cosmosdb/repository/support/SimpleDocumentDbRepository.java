/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository.support;

import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.DocumentDbRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import rx.Observable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SimpleDocumentDbRepository<T, ID extends Serializable> implements DocumentDbRepository<T, ID> {

    private final DocumentDbOperations operation;

    private final DocumentDbEntityInformation<T, ID> information;

    private final String collectionName;

    private final String partitionKeyName;

    private final Class<T> entityClass;

    private final boolean isIdFieldAsPartitionKey;

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata,
                                      ApplicationContext applicationContext) {
        this(metadata, applicationContext.getBean(DocumentDbOperations.class));
    }

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata,
                                      DocumentDbOperations dbOperations) {
        this.information = metadata;
        this.operation = dbOperations;
        this.collectionName = information.getCollectionName();
        this.partitionKeyName = information.getPartitionKeyFieldName();
        this.entityClass = information.getJavaType();
        this.isIdFieldAsPartitionKey = information.isIdFieldAsPartitionKey();

        createCollectionIfNotExists();
    }

    private void createCollectionIfNotExists() {
        this.operation.createCollectionIfNotExists(this.information);
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

        final PartitionKey partitionKey = information.getPartitionKey(entity);

        if (information.isNew(entity)) {
            return operation.insert(collectionName, entity, partitionKey);
        } else {
            operation.upsert(collectionName, entity, partitionKey);

            return entity;
        }
    }

    @Override
    public <S extends T> Observable<S> saveAsync(@NonNull S entity) {
        final PartitionKey partitionKey = information.getPartitionKey(entity);

        if (information.isNew(entity)) {
            return operation.insertAsync(collectionName, entity, partitionKey);
        } else {
            return operation.upsertAsync(collectionName, entity, partitionKey);
        }
    }

    /**
     * batch save entities
     *
     * @param entities
     * @param <S>
     * @return
     */
    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities, "Iterable entities should not be null");

        entities.forEach(this::save);

        return entities;
    }

    /**
     * find all entities from one collection without configuring partition key value
     *
     * @return
     */
    @Override
    public Iterable<T> findAll() {
        return operation.findAll(collectionName, entityClass, partitionKeyName);
    }

    /**
     * find entities based on id list from one collection without partitions
     *
     * @param ids
     * @return
     */
    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "Iterable ids should not be null");

        final List<T> entities = new ArrayList<>();

        ids.forEach(id -> findById(id).ifPresent(entities::add));

        return entities;
    }

    /**
     * find one entity per id without partitions
     *
     * @param id
     * @return
     */
    @Override
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "id must not be null");

        if (id instanceof String && !StringUtils.hasText((String) id)) {
            return Optional.empty();
        }

        final PartitionKey key = this.isIdFieldAsPartitionKey ? new PartitionKey(id) : null;

        return this.operation.findById(collectionName, id, entityClass, key);
    }

    @Override
    public Observable<T> findByIdAsync(ID id) {
        Assert.notNull(id, "id must not be null");

        if (id instanceof String && !StringUtils.hasText((String) id)) {
            return Observable.empty();
        }

        final PartitionKey key = this.isIdFieldAsPartitionKey ? new PartitionKey(id) : null;

        return this.operation.findByIdAsync(collectionName, id, entityClass, key);
    }

    /**
     * return count of documents in one collection without partitions
     *
     * @return
     */
    @Override
    public long count() {
        return operation.count(collectionName);
    }

    @Override
    public Observable<Long> countAllAsync() {
        return operation.countAsync(collectionName);
    }

    /**
     * delete one document per id without configuring partition key value
     *
     * @param id
     */
    @Override
    public void deleteById(ID id) {
        Assert.notNull(id, "id to be deleted should not be null");

        final PartitionKey partitionKey = isIdFieldAsPartitionKey ? new PartitionKey(id) : null;

        operation.deleteById(collectionName, id, partitionKey);
    }

    @Override
    public Observable<Object> deleteByIdAsync(ID id) {
        Assert.notNull(id, "id to be deleted should not be null");

        final PartitionKey partitionKey = isIdFieldAsPartitionKey ? new PartitionKey(id) : null;

        return operation.deleteByIdAsync(collectionName, id, partitionKey);
    }

    /**
     * delete one document per entity
     *
     * @param entity
     */
    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "entity to be deleted should not be null");

        final PartitionKey partitionKey = information.getPartitionKey(entity);
        final Object id = information.getId(entity);

        operation.deleteById(collectionName, id, partitionKey);
    }

    @Override
    public Observable<T> deleteAsync(T entity) {
        Assert.notNull(entity, "entity to be deleted should not be null");

        final PartitionKey partitionKey = information.getPartitionKey(entity);
        final Object id = information.getId(entity);

        return operation.deleteByIdAsync(collectionName, id, partitionKey).map(d -> entity);
    }

    /**
     * delete all the domains of a collection
     */
    @Override
    public void deleteAll() {
        this.operation.deleteAll(collectionName, partitionKeyName);
    }

    @Override
    public Observable<T> deleteAllAsync() {
        return operation.deleteAllAsync(collectionName, partitionKeyName);
    }

    /**
     * delete list of entities without partitions
     *
     * @param entities
     */
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "Iterable entities should not be null");

        entities.forEach(this::delete);
    }

    /**
     * check if an entity exists per id without partition
     *
     * @param id
     * @return
     */
    @Override
    public boolean existsById(ID id) {
        Assert.notNull(id, "id should not be null");

        return findById(id).isPresent();
    }

    /**
     * Returns all entities sorted by the given options.
     *
     * @param sort
     * @return all entities sorted by the given options
     */
    @Override
    public Iterable<T> findAll(@NonNull Sort sort) {
        Assert.notNull(sort, "sort of findAll should not be null");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(sort);

        return operation.find(query, collectionName, entityClass, partitionKeyName);
    }

    /**
     * FindQuerySpecGenerator
     * Returns a Page of entities meeting the paging restriction provided in the Pageable object.
     *
     * @param pageable
     * @return a page of entities
     */
    @Override
    public Page<T> findAll(Pageable pageable) {
        Assert.notNull(pageable, "pageable should not be null");

        return operation.findAll(pageable, collectionName, entityClass, partitionKeyName);
    }

    /**
     * Returns an Observable of Page of entities meeting the paging restriction provided in the Pageable object.
     *
     * @param pageable
     * @return an Observable that emits searched page
     */
    @Override
    public Observable<Page<T>> findAllAsync(Pageable pageable) {
        Assert.notNull(pageable, "pageable should not be null");

        return operation.findAllAsync(pageable, collectionName, entityClass, partitionKeyName);
    }

    @Override
    public Observable<T> findAllAsync() {
        return operation.findAllAsync(collectionName, entityClass, partitionKeyName);
    }
}
