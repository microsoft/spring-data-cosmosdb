/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
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
    public EntityMetadata<?> getEntityInformation() {
        final Class<?> domainClass = getDomainClass();
        final DocumentDbEntityInformation<Object, String> entityInformation =
                new DocumentDbEntityInformation(domainClass);

        this.metadata = new SimpleDocumentDbEntityMetadata<Object>((Class<Object>) domainClass, entityInformation);
        return this.metadata;
    }

}
