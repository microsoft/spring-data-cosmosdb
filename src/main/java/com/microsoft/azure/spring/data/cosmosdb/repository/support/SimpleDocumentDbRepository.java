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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class SimpleDocumentDbRepository<T, ID extends Serializable> implements DocumentDbRepository<T, ID> {

    private final DocumentDbOperations operation;

    private final DocumentDbEntityInformation<T, ID> information;

    private final String collectionName;

    private final String partitionKeyName;

    private final Class<T> entityClass;

    private final boolean isIdFieldAsPartitionKey;

    public SimpleDocumentDbRepository(DocumentDbEntityInformation<T, ID> metadata, ApplicationContext context) {
        this(metadata, context.getBean(DocumentDbOperations.class));
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
     * save the entity
     *
     * @param entity to be saved
     * @param <S>    entity type
     * @return entity saved
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

    /**
     * batch save entities
     *
     * @param entities iterable batch entities
     * @param <S>      entity type
     * @return iterable batch entities saved
     */
    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities, "Iterable entities should not be null");

        return StreamSupport.stream(entities.spliterator(), true).map(this::save).collect(Collectors.toList());
    }

    /**
     * find all entities from one collection
     *
     * @return iterable batch entities found
     */
    @Override
    public Iterable<T> findAll() {
        return operation.findAll(collectionName, entityClass, partitionKeyName);
    }

    /**
     * find entities based on id list from one collection without partitions
     *
     * @param ids iterable ids
     * @return batch entities found
     */
    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "Iterable ids should not be null");

        return StreamSupport.stream(ids.spliterator(), true)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * find one entity with given id
     *
     * @param id value of id
     * @return Optional of entity
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

    /**
     * count the documents in one collection
     *
     * @return the total count of documents
     */
    @Override
    public long count() {
        return operation.count(collectionName);
    }

    /**
     * delete one document from given entity id
     *
     * @param id the id of entity
     */
    @Override
    public void deleteById(ID id) {
        Assert.notNull(id, "id to be deleted should not be null");

        final PartitionKey partitionKey = isIdFieldAsPartitionKey ? new PartitionKey(id) : null;

        operation.deleteById(collectionName, id, partitionKey);
    }

    /**
     * delete one document from given entity
     *
     * @param entity the entity instance
     */
    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "entity to be deleted should not be null");

        final PartitionKey partitionKey = information.getPartitionKey(entity);
        final Object id = information.getId(entity);

        operation.deleteById(collectionName, id, partitionKey);
    }

    /**
     * delete all the domains of a collection
     */
    @Override
    public void deleteAll() {
        this.operation.deleteAll(collectionName, partitionKeyName);
    }

    /**
     * delete from given iterable entities
     *
     * @param entities of iterable
     */
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "Iterable entities should not be null");

        StreamSupport.stream(entities.spliterator(), true).forEach(this::delete);
    }

    /**
     * check if document exists from given id
     *
     * @param id of entity
     * @return true if document exists, or false
     */
    @Override
    public boolean existsById(ID id) {
        Assert.notNull(id, "id should not be null");

        return findById(id).isPresent();
    }

    /**
     * find all entities sorted by the given option
     *
     * @param sort of option
     * @return all entities with option sorted
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
}
