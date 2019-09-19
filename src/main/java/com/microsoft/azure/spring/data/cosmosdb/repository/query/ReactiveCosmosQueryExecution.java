/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.springframework.data.domain.Pageable;

public interface ReactiveCosmosQueryExecution {
    Object execute(DocumentQuery query, Class<?> type, String collection);

    final class CollectionExecution implements ReactiveCosmosQueryExecution {

        private final ReactiveCosmosOperations operations;

        public CollectionExecution(ReactiveCosmosOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.getContainerName(type);
        }
    }

    final class MultiEntityExecution implements ReactiveCosmosQueryExecution {

        private final ReactiveCosmosOperations operations;

        public MultiEntityExecution(ReactiveCosmosOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.find(query, type, collection);
        }
    }

    final class ExistsExecution implements ReactiveCosmosQueryExecution {

        private final ReactiveCosmosOperations operations;

        public ExistsExecution(ReactiveCosmosOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.exists(query, type, collection);
        }
    }

    final class DeleteExecution implements ReactiveCosmosQueryExecution {

        private final ReactiveCosmosOperations operations;

        public DeleteExecution(ReactiveCosmosOperations operations) {
            this.operations = operations;
        }

        @Override
        public Object execute(DocumentQuery query, Class<?> type, String collection) {
            return operations.delete(query, type, collection);
        }
    }

    //  TODO: Investigate pagedExecution for Reactive APIs.
//    final class PagedExecution implements ReactiveCosmosQueryExecution {
//        private final ReactiveCosmosOperations operations;
//        private final Pageable pageable;
//
//        public PagedExecution(ReactiveCosmosOperations operations, Pageable pageable) {
//            this.operations = operations;
//            this.pageable = pageable;
//        }
//
//        @Override
//        public Object execute(DocumentQuery query, Class<?> type, String collection) {
//            if (pageable.getPageNumber() != 0 && !(pageable instanceof DocumentDbPageRequest)) {
//                throw new IllegalStateException("Not the first page but Pageable is not a valid " +
//                        "DocumentDbPageRequest, requestContinuation is required for non first page request");
//            }
//
//            query.with(pageable);
//
//            return operations.paginationQuery(query, type, collection);
//        }
//    }
}
