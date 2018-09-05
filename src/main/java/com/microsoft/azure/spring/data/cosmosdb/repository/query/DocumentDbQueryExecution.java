/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.springframework.data.domain.Pageable;

public interface DocumentDbQueryExecution {
    Object execute(DocumentQuery query, Class<?> type, String collection);

    final class CollectionExecution implements DocumentDbQueryExecution {

        private final DocumentDbOperations operations;

        public CollectionExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.getCollectionName(type);
        }
    }

    final class MultiEntityExecution implements DocumentDbQueryExecution {

        private final DocumentDbOperations operations;

        public MultiEntityExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.find(query, type, collection);
        }
    }

    final class ExistsExecution implements DocumentDbQueryExecution {

        private final DocumentDbOperations operations;

        public ExistsExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.exists(query, type, collection);
        }
    }

    final class DeleteExecution implements DocumentDbQueryExecution {

        private final DocumentDbOperations operations;

        public DeleteExecution(DocumentDbOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.delete(query, type, collection);
        }
    }

    final class PagedExecution implements DocumentDbQueryExecution {
        private final DocumentDbOperations operations;
        private final Pageable pageable;

        public PagedExecution(DocumentDbOperations operations, Pageable pageable) {
            this.operations = operations;
            this.pageable = pageable;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            if (pageable.getPageNumber() != 0 && !(pageable instanceof DocumentDbPageRequest)) {
                throw new IllegalStateException("Not the first page but Pageable is not a valid " +
                        "DocumentDbPageRequest, requestContinuation is required for non first page request");
            }

            query.with(pageable);

            return operations.paginationQuery(query, type, collection);
        }
    }
}
