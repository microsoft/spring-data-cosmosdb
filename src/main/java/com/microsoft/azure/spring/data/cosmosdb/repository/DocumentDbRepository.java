/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import rx.Observable;

import java.io.Serializable;

/**
 * CosmosDB SQL API specific extension of the {@link PagingAndSortingRepository} interface.
 */
@NoRepositoryBean
public interface DocumentDbRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

    /**
     * Save {@code domain}, the {@link Observable} upon successful completion will contain a single domain object.
     *
     * @param domain the {@code domain} object to be saved.
     * @param <S>    type of the {@code domain} to be saved
     * @return {@link Observable} which emit the single saved object.
     */
    <S extends T> Observable<S> saveAsync(S domain);

    /**
     * Save collection of {@code entities}.
     *
     * @param entities collection of entities to be saved.
     * @param <S>      type of the {@code entities} to be saved.
     * @return {@link Observable} which emits all saved entities.
     */
    <S extends T> Observable<S> saveAllAsync(Iterable<S> entities);

    /**
     * Delete collection of {@code entities}.
     *
     * @param entities collection of entities to be deleted.
     * @return {@link Observable} which emits all deleted entities.
     */
    Observable<T> deleteAllAsync(Iterable<? extends T> entities);

    /**
     * Find documents by collection of {@code ids} and convert to domain type.
     *
     * @param ids collection of {@code ids} to be queried.
     * @return {@link Observable} which emits all entities queried by given {@code ids}.
     */
    Observable<T> findAllByIdAsync(Iterable<ID> ids);

    /**
     * Find document by {@code id} and convert to domain type.
     *
     * @param id to be queried domain id.
     * @return {@link Observable} which emits the matched entity or {@link Observable#empty()} if {@code id}
     * not found.
     */
    Observable<T> findByIdAsync(ID id);

    /**
     * Check whether certain {@code id} exists.
     *
     * @param id the value of id to be checked.
     * @return {@link Observable} which emits a single boolean value indicating whether given {@code id} exists.
     */
    Observable<Boolean> existsByIdAsync(ID id);

    /**
     * Delete document matching given {@code id}.
     *
     * @param id the value of the id to be deleted.
     * @return {@link Observable} which emits the deleted id if deleted successfully.
     */
    Observable<Object> deleteByIdAsync(ID id);

    /**
     * Delete all documents in current collection.
     *
     * @return {@link Observable#empty()} if all documents are deleted successfully.
     */
    Observable<T> deleteAllAsync();

    /**
     * Find documents with pagination query.
     *
     * @param pageable pagination information for the pagination query.
     * @return {@link Observable} emits {@link Page} elements, each {@link Page} element contains list of
     * objects.
     */
    Observable<Page<T>> findAllAsync(Pageable pageable);

    /**
     * Find all documents with given {@code sort} parameter.
     *
     * @param sort the sort option for query.
     * @return {@link Observable} which emits all objects in the collection and all objects emitted are sorted by
     * given {@code sort} condition.
     */
    Observable<T> findAllAsync(Sort sort);

    /**
     * Delete given {@code entity}.
     *
     * @param entity the entity to be deleted.
     * @return {@link Observable} which emits the deleted entity if delete completed successfully.
     */
    Observable<T> deleteAsync(T entity);

    /**
     * Find all documents in the collection.
     *
     * @return {@link Observable} which emits all objects converted from document in the collection.
     */
    Observable<T> findAllAsync();

    /**
     * Count how many documents exist in the collection.
     *
     * @return {@link Observable} which emits the number of total documents if completed successfully.
     */
    Observable<Long> countAllAsync();
}

