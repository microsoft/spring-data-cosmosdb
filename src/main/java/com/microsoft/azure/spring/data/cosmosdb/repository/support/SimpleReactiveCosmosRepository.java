/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.support;

import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.ReactiveCosmosRepository;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@RequiredArgsConstructor
public class SimpleReactiveCosmosRepository<T, ID extends Serializable> implements ReactiveCosmosRepository<T, ID> {

    public final DocumentDbEntityInformation<T, ID> entityInformation;
    public final ReactiveCosmosOperations cosmosOperations;

    @Override
    public Flux<T> findAll(Sort sort) {
        Assert.notNull(sort, "Sort must not be null!");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(sort);

        return cosmosOperations.find(query, entityInformation.getJavaType(), entityInformation.getCollectionName());
    }

    @Override
    public <S extends T> Mono<S> save(S entity) {

        Assert.notNull(entity, "Entity must not be null!");

        if (entityInformation.isNew(entity)) {
            return cosmosOperations.insert(entityInformation.getCollectionName(),
                    entity,
                    createKey(entityInformation.getPartitionKeyFieldValue(entity)));
        } else {
            return cosmosOperations.upsert(entityInformation.getCollectionName(),
                    entity, createKey(entityInformation.getPartitionKeyFieldValue(entity)));
        }
    }

    @Override
    public <S extends T> Flux<S> saveAll(Iterable<S> entities) {

        Assert.notNull(entities, "The given Iterable of entities must not be null!");

        return Flux.fromIterable(entities).flatMap(this::save);
    }

    @Override
    public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {

        Assert.notNull(entityStream, "The given Publisher of entities must not be null!");

        return Flux.from(entityStream).flatMap(this::save);
    }

    @Override
    public Mono<T> findById(ID id) {
        Assert.notNull(id, "The given id must not be null!");
        return cosmosOperations.findById(entityInformation.getCollectionName(), id, entityInformation.getJavaType());
    }

    @Override
    public Mono<T> findById(Publisher<ID> publisher) {
        Assert.notNull(publisher, "The given id must not be null!");

        return Mono.from(publisher).flatMap(
                id -> cosmosOperations.findById(entityInformation.getCollectionName(),
                        id, entityInformation.getJavaType()));
    }

    @Override
    public Mono<Boolean> existsById(ID id) {
        Assert.notNull(id, "The given id must not be null!");

        return cosmosOperations.existsById(id, entityInformation.getJavaType(),
                entityInformation.getCollectionName());
    }

    @Override
    public Mono<Boolean> existsById(Publisher<ID> publisher) {
        Assert.notNull(publisher, "The given id must not be null!");

        return Mono.from(publisher).flatMap(id -> cosmosOperations.existsById(id, entityInformation.getJavaType(),
                entityInformation.getCollectionName()));
    }

    @Override
    public Flux<T> findAll() {
        return cosmosOperations.findAll(entityInformation.getCollectionName(), entityInformation.getJavaType());
    }

    @Override
    public Flux<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "Iterable ids should not be null");
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<T> findAllById(Publisher<ID> ids) {
        Assert.notNull(ids, "The given Publisher of Id's must not be null!");
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Long> count() {
        return cosmosOperations.count(entityInformation.getCollectionName());
    }

    @Override
    public Mono<Void> deleteById(ID id) {
        Assert.notNull(id, "The given id must not be null!");

        return cosmosOperations.deleteById(entityInformation.getCollectionName(), id, null);
    }

    @Override
    public Mono<Void> deleteById(Publisher<ID> publisher) {
        Assert.notNull(publisher, "Id must not be null!");

        return Mono.from(publisher).flatMap(id -> cosmosOperations.deleteById(entityInformation.getCollectionName(),
                id, null)).then();
    }

    @Override
    public Mono<Void> delete(@NonNull T entity) {
        Assert.notNull(entity, "entity to be deleted must not be null!");

        final Object id = entityInformation.getId(entity);
        return cosmosOperations.deleteById(entityInformation.getCollectionName(),
                id,
                createKey(entityInformation.getPartitionKeyFieldValue(entity)));
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "The given Iterable of entities must not be null!");

        return Flux.fromIterable(entities).flatMap(this::delete).then();
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {

        Assert.notNull(entityStream, "The given Publisher of entities must not be null!");

        return Flux.from(entityStream)//
                .map(entityInformation::getRequiredId)//
                .flatMap(this::deleteById)//
                .then();
    }

    @Override
    public Mono<Void> deleteAll() {
        return cosmosOperations.deleteAll(entityInformation.getCollectionName(),
                entityInformation.getPartitionKeyFieldName());
    }

    private PartitionKey createKey(String partitionKeyValue) {
        if (StringUtils.isEmpty(partitionKeyValue)) {
            return null;
        }
        return new PartitionKey(partitionKeyValue);
    }

}
