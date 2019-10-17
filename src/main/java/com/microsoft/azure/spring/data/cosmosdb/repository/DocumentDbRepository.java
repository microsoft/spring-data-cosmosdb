/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository;

import com.microsoft.azure.documentdb.PartitionKey;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.Optional;

/**
 * {@link DocumentDbRepository} is deprecated.
 * Instead use CosmosRepository, which is introduced in 2.2.0 version.
 */
@Deprecated
@NoRepositoryBean
public interface DocumentDbRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @param partitionKey partition key value of entity, must not be null.
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     * @throws IllegalArgumentException if {@code id} is {@literal null}.
     */
    Optional<T> findById(ID id, PartitionKey partitionKey);

    /**
     * Deletes an entity by its id and partition key.
     * @param id must not be {@literal null}.
     * @param partitionKey partition key value of the entity, must not be null.
     * @throws IllegalArgumentException in case the given {@code id} is {@literal null}.
     */
    void deleteById(ID id, PartitionKey partitionKey);

}

