/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.CosmosContainerResponse;
import com.azure.data.cosmos.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCosmosOperations {

    String getContainerName(Class<?> domainClass);

    Mono<CosmosContainerResponse> createCollectionIfNotExists(CosmosEntityInformation information);

    <T> Flux<T> findAll(String collectionName, Class<T> entityClass);

    <T> Flux<T> findAll(Class<T> entityClass);

    <T> Mono<T> findById(Object id, Class<T> entityClass);

    <T> Mono<T> findById(String collectionName, Object id, Class<T> entityClass);

    <T> Mono<T> findById(Object id, Class<T> entityClass, PartitionKey partitionKey);

    <T> Mono<T> insert(T objectToSave, PartitionKey partitionKey);

    <T> Mono<T> insert(String collectionName, Object objectToSave, PartitionKey partitionKey);

    <T> Mono<T> upsert(T object, PartitionKey partitionKey);

    <T> Mono<T> upsert(String collectionName, T object, PartitionKey partitionKey);

    Mono<Void> deleteById(String collectionName, Object id, PartitionKey partitionKey);

    Mono<Void> deleteAll(String collectionName, String partitionKey);

    void deleteContainer(String collectionName);

    <T> Flux<T> delete(DocumentQuery query, Class<T> entityClass, String collectionName);

    <T> Flux<T> find(DocumentQuery query, Class<T> entityClass, String collectionName);

    Mono<Boolean> exists(DocumentQuery query, Class<?> entityClass, String collectionName);

    Mono<Boolean> existsById(Object id, Class<?> entityClass, String containerName);

    Mono<Long> count(String collectionName);

    Mono<Long> count(DocumentQuery query, String containerName);

    MappingCosmosConverter getConverter();
}
