/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.support;

import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosOperations;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import java.io.Serializable;
import java.util.Optional;

public class ReactiveCosmosRepositoryFactory extends ReactiveRepositoryFactorySupport {

//    private final ReactiveCosmosOperations cosmosOperations;
    private final ApplicationContext applicationContext;

    public ReactiveCosmosRepositoryFactory(ReactiveCosmosOperations cosmosOperations,
                                           ApplicationContext applicationContext) {
//        this.cosmosOperations = cosmosOperations;
        this.applicationContext = applicationContext;
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        // TODO: Move to CosmosDBEntityInformation
        return new DocumentDbEntityInformation<>(domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        final EntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
        return getTargetRepositoryViaReflection(information, entityInformation, this.applicationContext);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleReactiveCosmosRepository.class;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(
            QueryLookupStrategy.Key key, QueryMethodEvaluationContextProvider evaluationContextProvider) {
        // TODO: CosmosQueryLookupStrategy
        return super.getQueryLookupStrategy(key, evaluationContextProvider);
    }

}
