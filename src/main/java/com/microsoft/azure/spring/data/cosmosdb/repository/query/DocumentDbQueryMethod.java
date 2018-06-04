/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import java.lang.reflect.Method;

public class DocumentDbQueryMethod extends QueryMethod {

    private DocumentDbEntityMetadata<?> metadata;

    public DocumentDbQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        super(method, metadata, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityMetadata<?> getEntityInformation() {
        final Class<Object> domainClass = (Class<Object>) getDomainClass();
        final DocumentDbEntityInformation entityInformation =
                new DocumentDbEntityInformation<Object, String>(domainClass);

        this.metadata = new SimpleDocumentDbEntityMetadata<Object>(domainClass, entityInformation);
        return this.metadata;
    }
}
