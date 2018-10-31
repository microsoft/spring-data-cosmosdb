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

/**
 * provide query operations for
 * ${@link com.microsoft.azure.spring.data.cosmosdb.repository.support.SimpleDocumentDbRepository}
 *
 * <p>
 * <b>Collection</b>
 * A collection is a container of JSON documents. Collections can span one or more partitions/servers and can scale to
 * handle practically unlimited volumes of storage or throughput.
 * </p>
 *
 * <p>
 * <b>PartitionKey</b>
 * Azure Cosmos DB provides containers for storing data called collections (for documents), graphs, or tables.
 * Containers are logical resources and can span one or more physical partitions or servers. The number of partitions is
 * determined by Azure Cosmos DB based on the storage size and throughput provisioned for a container or a set of
 * containers.
 * </p>
 */
public interface DocumentDbOperations {

    /**
     * query if collection exists, or create the collection.
     *
     * @param information ${@link DocumentDbEntityInformation} of entity
     * @return the existing or created collection
     */
    DocumentCollection createCollectionIfNotExists(DocumentDbEntityInformation information);

    /**
     * get the ${@link MappingDocumentDbConverter}
     *
     * @return the ${@link MappingDocumentDbConverter}.
     */
    MappingDocumentDbConverter getConverter();

    /**
     * find all entities with given entity class.
     *
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the List of all entities under the collection
     */
    <T> List<T> findAll(String collectionName, Class<T> entityClass, String partitionKeyName);

    /**
     * find the unique entity with given id value.
     *
     * @param collectionName of entity
     * @param id             of entity
     * @param entityClass    class type of entity
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @param <T>            entity type
     * @return the Optional of the unique entity with given id.
     */
    <T> Optional<T> findById(String collectionName, Object id, Class<T> entityClass, PartitionKey key);

    /**
     * insert the entity from given collection, if exist will throw out
     * ${@link com.microsoft.azure.cosmosdb.DocumentClientException}.
     *
     * @param collectionName of entity
     * @param objectToSave   the entity instance
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @param <T>            entity type
     * @return the entity inserted
     */
    <T> T insert(String collectionName, T objectToSave, PartitionKey key);

    /**
     * insert the entity from given collection, if exist will update the entity.
     *
     * @param collectionName of entity
     * @param object         the entity instance
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @param <T>            entity type
     */
    <T> void upsert(String collectionName, T object, PartitionKey key);

    /**
     * delete the unique entity with given id value, if no exist will exit silently.
     *
     * @param collectionName of entity
     * @param id             of entity
     * @param key            ${@link PartitionKey} of entity, nullable.
     */
    void deleteById(String collectionName, Object id, PartitionKey key);

    /**
     * delete all entities under the collection.
     *
     * @param collectionName   of entity
     * @param partitionKeyName the partitionKey field name, nullable
     */
    void deleteAll(String collectionName, String partitionKeyName);

    /**
     * delete the collection of entity repository
     *
     * @param collectionName of entity
     */
    void deleteCollection(String collectionName);

    /**
     * delete the entities based on the result from ${@link DocumentQuery}.
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the list of deleted entities
     */
    <T> List<T> delete(DocumentQuery query, String collectionName, Class<T> entityClass, String partitionKeyName);

    /**
     * find the entities based on the result from ${@link DocumentQuery}.
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the list of found entities
     */
    <T> List<T> find(DocumentQuery query, String collectionName, Class<T> entityClass, String partitionKeyName);

    /**
     * check entities exist based on the result from ${@link DocumentQuery}
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @return true if exist, or false.
     */
    Boolean exists(DocumentQuery query, String collectionName, Class<?> entityClass, String partitionKeyName);

    /**
     * find all entities with ${@link Pageable} option.
     *
     * @param pageable         the option of page
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link Page} of entities.
     */
    <T> Page<T> findAll(Pageable pageable, String collectionName, Class<T> entityClass, String partitionKeyName);

    /**
     * find all entities based on the result from ${@link DocumentQuery}
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link Page} of entities
     */
    <T> Page<T> paginationQuery(DocumentQuery query, String collectionName, Class<T> entityClass,
                                String partitionKeyName);

    /**
     * find all entities asynchronously based on the result from ${@link DocumentQuery}
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link rx.Observable} of ${@link Page}
     */
    <T> Observable<Page<T>> paginationQueryAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                                 String partitionKeyName);

    /**
     * count the total entities
     *
     * @param collectionName of entity
     * @return the total count of collection.
     */
    long count(String collectionName);

    /**
     * count the entities based on the result of ${@link DocumentQuery}
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @return the count of entities.
     */
    long count(DocumentQuery query, String collectionName, Class<?> entityClass, String partitionKeyName);

    /**
     * insert the entity asynchronously from given collection, if exist will throw out
     * ${@link com.microsoft.azure.cosmosdb.DocumentClientException}.
     *
     * @param collectionName of entity
     * @param entity         the entity instance
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @param <T>            entity type
     * @return the ${@link rx.Observable} of inserted entity
     */
    <T> Observable<T> insertAsync(String collectionName, T entity, PartitionKey key);

    /**
     * insert the entity asynchronously from given collection, if exist will update the entity asynchronously
     *
     * @param collectionName of entity
     * @param entity         the entity instance
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @param <T>            entity type
     * @return the ${@link rx.Observable} of updated entity
     */
    <T> Observable<T> upsertAsync(String collectionName, T entity, PartitionKey key);

    /**
     * find the unique entity asynchronously with given id value.
     *
     * @param collectionName of entity
     * @param id             of entity
     * @param entityClass    class type of entity
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @param <T>            entity type
     * @return the ${@link rx.Observable} of the unique entity with given id
     */
    <T> Observable<T> findByIdAsync(String collectionName, Object id, Class<T> entityClass, PartitionKey key);

    /**
     * delete the unique entity asynchronously with given id value, if no exist will exit silently.
     *
     * @param collectionName of entity
     * @param id             of entity
     * @param key            ${@link PartitionKey} of entity, nullable.
     * @return the ${@link rx.Observable} of the id value
     */
    Observable<Object> deleteByIdAsync(String collectionName, Object id, PartitionKey key);

    /**
     * delete all entities asynchronously under the collection.
     *
     * @param collectionName   of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link rx.Observable#empty()}.
     */
    <T> Observable<T> deleteAllAsync(String collectionName, String partitionKeyName);

    /**
     * find all entities asynchronously with ${@link Pageable} option.
     *
     * @param pageable         the option of page
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link rx.Observable} of entities ${@link Page}.
     */
    <T> Observable<Page<T>> findAllAsync(Pageable pageable, String collectionName, Class<T> entityClass,
                                         String partitionKeyName);

    /**
     * delete the collection asynchronously of entity repository
     *
     * @param collectionName of entity
     * @return the ${@link rx.Observable} of deleted ${@link DocumentCollection}.
     */
    Observable<DocumentCollection> deleteCollectionAsync(String collectionName);

    /**
     * delete the entities asynchronously based on the result from ${@link DocumentQuery}.
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link rx.Observable} of deleted entities.
     */
    <T> Observable<T> deleteAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                  String partitionKeyName);

    /**
     * find the entities asynchronously based on the result from ${@link DocumentQuery}.
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link rx.Observable} of found entities.
     */
    <T> Observable<T> findAsync(DocumentQuery query, String collectionName, Class<T> entityClass,
                                String partitionKeyName);

    /**
     * check entities exist asynchronously based on the result from ${@link DocumentQuery}
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @return the ${@link rx.Observable} of ${@link Boolean} indicates exist or not.
     */
    Observable<Boolean> existsAsync(DocumentQuery query, String collectionName, Class<?> entityClass,
                                    String partitionKeyName);

    /**
     * find all entities asynchronously with given entity class.
     *
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @param <T>              entity type
     * @return the ${@link rx.Observable} of all found entities.
     */
    <T> Observable<T> findAllAsync(String collectionName, Class<T> entityClass, String partitionKeyName);

    /**
     * count the total entities asynchronously
     *
     * @param collectionName of entity
     * @return the ${@link rx.Observable} of the total entities count.
     */
    Observable<Long> countAsync(String collectionName);

    /**
     * count the entities asynchronously based on the result of ${@link DocumentQuery}
     *
     * @param query            the Sql query with parameters.
     * @param collectionName   of entity
     * @param entityClass      class type of entity
     * @param partitionKeyName the partitionKey field name, nullable
     * @return the ${@link rx.Observable} of the entities count.
     */
    Observable<Long> countAsync(DocumentQuery query, String collectionName, Class<?> entityClass,
                                String partitionKeyName);
}
