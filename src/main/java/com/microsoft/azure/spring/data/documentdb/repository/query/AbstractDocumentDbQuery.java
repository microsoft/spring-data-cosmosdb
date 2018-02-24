/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;

public abstract class AbstractDocumentDbQuery implements RepositoryQuery {

    private final DocumentDbQueryMethod method;
    private final DocumentDbOperations operations;

    public AbstractDocumentDbQuery(DocumentDbQueryMethod method, DocumentDbOperations operations) {
        this.method = method;
        this.operations = operations;
    }

    public Object execute(Object[] parameters) {
        final DocumentDbParameterAccessor accessor = new DocumentDbParameterParameterAccessor(method, parameters);
        final Query query = createQuery(accessor);

        final ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
        final String collection = ((DocumentDbEntityMetadata) method.getEntityInformation()).getCollectionName();

        final DocumentDbQueryExecution execution = getExecution(query, accessor);
        return execution.execute(query, processor.getReturnedType().getDomainType(), collection);
    }


    private DocumentDbQueryExecution getExecution(Query query, DocumentDbParameterAccessor accessor) {
        if (isDeleteQuery()) {
            return new DocumentDbQueryExecution.DeleteExecution(operations);
        } else {
            return new DocumentDbQueryExecution.MultiEntityExecution(operations);
        }
    }

    public DocumentDbQueryMethod getQueryMethod() {
        return method;
    }

    protected abstract Query createQuery(DocumentDbParameterAccessor accessor);

    protected abstract boolean isDeleteQuery();

}
