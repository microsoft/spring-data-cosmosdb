/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface DocumentDbRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {
    List<T> findAll(String partitionKeyValue);

    T findOne(ID id, String partitionKeyValue);

    void delete(ID id, String partitionKeyValue);

    void delete(T entity, String partitionKeyValue);
}

