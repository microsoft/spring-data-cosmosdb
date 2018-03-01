/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbPersistentProperty;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeDocumentDbQuery extends AbstractDocumentDbQuery {

    private final PartTree tree;
    private final MappingContext<?, DocumentDbPersistentProperty> mappingContext;
    private final ResultProcessor processor;

    public PartTreeDocumentDbQuery(DocumentDbQueryMethod method, DocumentDbOperations operations) {
        super(method, operations);

        this.processor = method.getResultProcessor();
        this.tree = new PartTree(method.getName(), processor.getReturnedType().getDomainType());
        this.mappingContext = operations.getConverter().getMappingContext();
    }

    public PartTree getTree() {
        return this.tree;
    }

    @Override
    protected Query createQuery(DocumentDbParameterAccessor accessor) {
        final DocumentDbQueryCreator creator = new DocumentDbQueryCreator(tree, accessor, mappingContext);

        final Query query = creator.createQuery();

        if (tree.isLimiting()) {
            throw new NotImplementedException("Limiting is not supported.");
        }
        return query;
    }

    @Override
    protected boolean isDeleteQuery() {
        return tree.isDelete();
    }
}
