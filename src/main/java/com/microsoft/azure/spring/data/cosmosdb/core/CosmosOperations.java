/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CosmosOperations {

    String getCollectionName(Class<?> domainType);

    CosmosContainerProperties createCollectionIfNotExists(CosmosEntityInformation<?, ?> information);

    <T> List<T> findAll(Class<T> domainType);

    <T> List<T> findAll(String collectionName, Class<T> domainType);

    <T> T findById(Object id, Class<T> domainType);

    <T> T findById(String collectionName, Object id, Class<T> domainType);

    <T> T findById(Object id, Class<T> domainType, PartitionKey partitionKey);

    <T> T insert(T objectToSave, PartitionKey partitionKey);

    <T> T insert(String collectionName, T objectToSave, PartitionKey partitionKey);

    <T> void upsert(T object, PartitionKey partitionKey);

    <T> void upsert(String collectionName, T object, PartitionKey partitionKey);

    void deleteById(String collectionName, Object id, PartitionKey partitionKey);

    void deleteAll(String collectionName, Class<?> domainType);

    void deleteCollection(String collectionName);

    <T> List<T> delete(DocumentQuery query, Class<T> domainType, String collectionName);

    <T> List<T> find(DocumentQuery query, Class<T> domainType, String collectionName);

    <T, ID> List<T> findByIds(Iterable<ID> ids, Class<T> domainType, String collectionName);

    <T> Boolean exists(DocumentQuery query, Class<T> domainType, String collectionName);

    <T> Page<T> findAll(Pageable pageable, Class<T> domainType, String collectionName);

    <T> Page<T> paginationQuery(DocumentQuery query, Class<T> domainType, String collectionName);

    long count(String collectionName);

    <T> long count(DocumentQuery query, Class<T> domainType, String collectionName);

    MappingCosmosConverter getConverter();
}
