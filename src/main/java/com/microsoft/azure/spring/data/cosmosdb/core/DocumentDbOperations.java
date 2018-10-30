/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rx.Observable;

import java.util.List;
import java.util.Optional;

public interface DocumentDbOperations {

    DocumentCollection createCollectionIfNotExists(DocumentDbEntityInformation information);

    <T> List<T> findAll(String collectionName, Class<T> entityClass, String partitionKeyName);

    <T> Optional<T> findById(String collectionName, Object id, Class<T> entityClass, PartitionKey key);

    <T> T insert(String collectionName, T objectToSave, PartitionKey partitionKey);

    <T> void upsert(String collectionName, T object, PartitionKey partitionKey);

    void deleteById(String collectionName, Object id, PartitionKey partitionKey);

    void deleteAll(String collectionName, String partitionKeyName);

    void deleteCollection(String collectionName);

    <T> List<T> delete(DocumentQuery query, String collectionName, Class<T> entityClass, String partitionKeyName);

    <T> List<T> find(DocumentQuery query, String collectionName, Class<T> entityClass, String partitionKeyName);

    Boolean exists(DocumentQuery query, String collectionName, Class<?> entityClass, String partitionKeyName);

    <T> Page<T> findAll(Pageable pageable, String collectionName, Class<T> entityClass, String partitionKeyName);

    <T> Page<T> paginationQuery(DocumentQuery query, String collectionName, Class<T> entityClass,
                                String partitionKeyName);

    <T> Observable<Page<T>> paginationQueryAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                                 String partitionKeyName);

    long count(String collectionName);

    long count(DocumentQuery query, String collectionName, Class<?> entityClass, String partitionKeyName);

    MappingDocumentDbConverter getConverter();

    <T> Observable<T> insertAsync(String collectionName, T domain, PartitionKey key);

    <T> Observable<T> upsertAsync(String collectionName, T domain, PartitionKey key);

    <T> Observable<T> findByIdAsync(String collectionName, Object id, Class<T> entityClass, PartitionKey key);

    Observable<Object> deleteByIdAsync(String collectionName, Object id, PartitionKey key);

    <T> Observable<T> deleteAllAsync(String collectionName, String partitionKeyName);

    <T> Observable<Page<T>> findAllAsync(Pageable pageable, String collectionName, Class<T> entityClass,
                                         String partitionKeyName);

    Observable<DocumentCollection> deleteCollectionAsync(String collectionName);

    <T> Observable<T> deleteAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                  String partitionKeyName);

    <T> Observable<T> findAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                String partitionKeyName);

    Observable<Boolean> existsAsync(DocumentQuery query, String collectionName, Class<?> entityClass,
                                    String partitionKeyName);

    <T> Observable<T> findAllAsync(String collectionName, Class<T> entityClass, String partitionKeyName);

    Observable<Long> countAsync(String collectionName);

    Observable<Long> countAsync(DocumentQuery query, String collectionName, Class<?> entityClass,
                                String partitionKeyName);
}
