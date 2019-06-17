/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

public abstract class AbstractDocumentDbQuery implements RepositoryQuery {

    private final DocumentDbQueryMethod method;
    private final DocumentDbOperations operations;

    public AbstractDocumentDbQuery(DocumentDbQueryMethod method, DocumentDbOperations operations) {
        this.method = method;
        this.operations = operations;
    }

    public Object execute(Object[] parameters) {
        final DocumentDbParameterAccessor accessor = new DocumentDbParameterParameterAccessor(method, parameters);
        final DocumentQuery query = createQuery(accessor);

        final ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
        final String collection = ((DocumentDbEntityMetadata) method.getEntityInformation()).getCollectionName();

        final DocumentDbQueryExecution execution = getExecution(accessor, processor.getReturnedType());
        return execution.execute(query, processor.getReturnedType().getDomainType(), collection);
    }


    private DocumentDbQueryExecution getExecution(DocumentDbParameterAccessor accessor, ReturnedType returnedType) {
        if (isDeleteQuery()) {
            return new DocumentDbQueryExecution.DeleteExecution(operations);
        } else if (method.isPageQuery()) {
            return new DocumentDbQueryExecution.PagedExecution(operations, accessor.getPageable());
        } else if (isExistsQuery()) {
            return new DocumentDbQueryExecution.ExistsExecution(operations);
        } else if (method.isCollectionQuery()) {
            return new DocumentDbQueryExecution.MultiEntityExecution(operations);
        } else {
            return new DocumentDbQueryExecution.SingleEntityExecution(operations, returnedType);
        }
    }

    public DocumentDbQueryMethod getQueryMethod() {
        return method;
    }

    protected abstract DocumentQuery createQuery(DocumentDbParameterAccessor accessor);

    protected abstract boolean isDeleteQuery();

    protected abstract boolean isExistsQuery();

}
