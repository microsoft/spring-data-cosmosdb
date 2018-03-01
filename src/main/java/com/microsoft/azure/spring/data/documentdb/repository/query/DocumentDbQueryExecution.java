/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;

public interface DocumentDbQueryExecution {
    Object execute(Query query, Class<?> type, String collection);

    final class CollectionExecution implements DocumentDbQueryExecution {

        private final DocumentDbOperations operations;

        public CollectionExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(Query query, Class<?> type, String collection) {
            return operations.getCollectionName(type);
        }
    }

    final class MultiEntityExecution implements DocumentDbQueryExecution {

        private final DocumentDbOperations operations;

        public MultiEntityExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(Query query, Class<?> type, String collection) {
            return operations.find(query, type, collection);
        }
    }

    final class DeleteExecution implements DocumentDbQueryExecution {
        private final DocumentDbOperations operations;

        public DeleteExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(Query query, Class<?> type, String collection) {
            return operations.delete(query, type, collection);
        }
    }
}
