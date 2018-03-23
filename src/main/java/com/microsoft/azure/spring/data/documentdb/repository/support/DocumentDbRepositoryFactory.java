/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.repository.support;

import com.microsoft.azure.spring.data.documentdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.documentdb.repository.query.DocumentDbQueryMethod;
import com.microsoft.azure.spring.data.documentdb.repository.query.PartTreeDocumentDbQuery;
import org.springframework.context.ApplicationContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;


public class DocumentDbRepositoryFactory extends RepositoryFactorySupport {

    private final ApplicationContext applicationContext;
    private final DocumentDbOperations dbOperations;

    public DocumentDbRepositoryFactory(DocumentDbOperations dbOperations, ApplicationContext applicationContext) {
        this.dbOperations = dbOperations;
        this.applicationContext = applicationContext;
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleDocumentDbRepository.class;
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        final EntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
        return getTargetRepositoryViaReflection(information, entityInformation, this.applicationContext);
    }

    @Override
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new DocumentDbEntityInformation<T, ID>(domainClass);
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key,
                                                         EvaluationContextProvider evaluationContextProvider) {
        return new DocumentDbQueryLookupStrategy(dbOperations, evaluationContextProvider);
    }

    private static class DocumentDbQueryLookupStrategy implements QueryLookupStrategy {
        private final DocumentDbOperations dbOperations;

        public DocumentDbQueryLookupStrategy(DocumentDbOperations operations, EvaluationContextProvider provider) {
            this.dbOperations = operations;
        }

        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
                                            ProjectionFactory factory, NamedQueries namedQueries) {
            final DocumentDbQueryMethod queryMethod = new DocumentDbQueryMethod(method, metadata, factory);

            Assert.notNull(queryMethod, "queryMethod must not be null!");
            Assert.notNull(dbOperations, "dbOperations must not be null!");
            return new PartTreeDocumentDbQuery(queryMethod, dbOperations);

        }
    }
}
