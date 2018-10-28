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

@NoRepositoryBean
public interface DocumentDbRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

    <S extends T> Observable<S> saveAsync(S domain);

    <S extends T> Observable<S> saveAllAsync(Iterable<S> entities);

    Observable<T> deleteAllAsync(Iterable<? extends T> entities);

    Observable<T> findByIdAsync(ID id);

    Observable<Boolean> existsByIdAsync(ID id);

    Observable<Object> deleteByIdAsync(ID id);

    Observable<T> deleteAllAsync();

    Observable<Page<T>> findAllAsync(Pageable pageable);

    Observable<T> findAllAsync(Sort sort);

    Observable<T> deleteAsync(T entity);

    Observable<T> findAllAsync();

    Observable<Long> countAllAsync();
}

