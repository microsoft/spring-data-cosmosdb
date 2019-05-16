/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

@NoRepositoryBean
public interface ReactiveCosmosRepository<T, ID> extends ReactiveSortingRepository<T, ID> {
}
