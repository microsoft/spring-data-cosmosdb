package com.microsoft.azure.spring.data.cosmosdb.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

@NoRepositoryBean
public interface ReactiveCosmosRepository<T, ID> extends ReactiveSortingRepository<T, ID> {
}
