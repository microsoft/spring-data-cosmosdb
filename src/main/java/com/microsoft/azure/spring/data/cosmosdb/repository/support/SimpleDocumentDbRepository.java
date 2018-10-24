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

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata,
                                      ApplicationContext applicationContext) {
        this.operation = applicationContext.getBean(DocumentDbOperations.class);
        this.information = metadata;

        createCollectionIfNotExists();
    }

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata,
                                      DocumentDbOperations dbOperations) {
        this.operation = dbOperations;
        this.information = metadata;

        createCollectionIfNotExists();
    }

    private void createCollectionIfNotExists() {
        this.operation.createCollectionIfNotExists(this.information);
    }

    /**
     * save entity without partition
     *
     * @param domain to be saved
     * @param <S>
     * @return entity
     */
    @Override
    public <S extends T> S save(S domain) {
        Assert.notNull(domain, "domain must not be null");

        final String collectionName = information.getCollectionName();
        final PartitionKey partitionKey = createPartitionKey(information.getPartitionKeyFieldValue(domain));

        if (information.isNew(domain)) {
            return operation.insert(collectionName, domain, partitionKey);
        } else {
            operation.upsert(collectionName, domain, partitionKey);

            return domain;
        }
    }

    @Override
    public <S extends T> Observable<S> saveAsync(@NonNull S domain) {
        final String collectionName = information.getCollectionName();
        final PartitionKey partitionKey = createPartitionKey(information.getPartitionKeyFieldValue(domain));

        if (information.isNew(domain)) {
            return operation.insertAsync(collectionName, domain, partitionKey);
        } else {
            return operation.upsertAsync(collectionName, domain, partitionKey);
        }
    }

    private PartitionKey createPartitionKey(String partitionKeyValue) {
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
        return operation.findAll(information.getCollectionName(), information.getJavaType());
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

        final PartitionKey key = information.isIdFieldAsPartitionKey() ? new PartitionKey(id) : null;

        return this.operation.findById(information.getCollectionName(), id, information.getJavaType(), key);
    }

    @Override
    public Observable<T> findByIdAsync(@NonNull ID id) {

        if (id instanceof String && !StringUtils.hasText((String) id)) {
            return Observable.just(null);
        }

        final PartitionKey key = information.isIdFieldAsPartitionKey() ? new PartitionKey(id) : null;

        return this.operation.findByIdAsync(information.getCollectionName(), id, information.getJavaType(), key);
    }

    /**
     * return count of documents in one collection without partitions
     *
     * @return
     */
    @Override
    public long count() {
        return operation.count(information.getCollectionName());
    }

    @Override
    public Observable<Long> countAllAsync() {
        return operation.countAsync(information.getCollectionName());
    }

    /**
     * delete one document per id without configuring partition key value
     *
     * @param id
     */
    @Override
    public void deleteById(ID id) {
        Assert.notNull(id, "id to be deleted should not be null");

        final PartitionKey partitionKey = information.isIdFieldAsPartitionKey() ? new PartitionKey(id) : null;

        operation.deleteById(information.getCollectionName(), id, partitionKey);
    }

    @Override
    public Observable<Object> deleteByIdAsync(ID id) {
        Assert.notNull(id, "id to be deleted should not be null");

        final PartitionKey partitionKey = information.isIdFieldAsPartitionKey() ? new PartitionKey(id) : null;

        return operation.deleteByIdAsync(information.getCollectionName(), id, partitionKey);
    }

    /**
     * delete one document per entity
     *
     * @param entity
     */
    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "entity to be deleted should not be null");

        final String keyValue = information.getPartitionKeyFieldValue(entity);
        final PartitionKey partitionKey = keyValue == null ? null : new PartitionKey(keyValue);

        operation.deleteById(information.getCollectionName(), information.getId(entity), partitionKey);
    }

    /**
     * delete all the domains of a collection
     */
    @Override
    public void deleteAll() {
        final List<String> partitionKeyNames = information.getPartitionKeyNames();

        this.operation.deleteAll(information.getCollectionName(), partitionKeyNames);
    }

    @Override
    public Observable<T> deleteAllAsync() {
        final List<String> partitionKeyNames = information.getPartitionKeyNames();

        return operation.deleteAllAsync(information.getCollectionName(), partitionKeyNames);
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
     * @param primaryKey
     * @return
     */
    @Override
    public boolean existsById(ID primaryKey) {
        Assert.notNull(primaryKey, "primaryKey should not be null");

        return findById(primaryKey).isPresent();
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

        return operation.find(query, information.getJavaType(), information.getCollectionName());
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

        return operation.findAll(pageable, information.getJavaType(), information.getCollectionName());
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

        return operation.findAllAsync(pageable, information.getJavaType(), information.getCollectionName());
    }
}
