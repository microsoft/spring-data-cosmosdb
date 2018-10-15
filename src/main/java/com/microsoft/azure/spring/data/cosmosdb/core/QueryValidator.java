/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.cosmosdb.*;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class QueryValidator {

    static boolean isCollectionSupportSortByString(@NonNull DocumentCollection collection) {
        final IndexingPolicy policy = collection.getIndexingPolicy();
        final List<Index> indices = new ArrayList<>();

        policy.getIncludedPaths().forEach(p -> indices.addAll(p.getIndexes()));

        return indices.stream().anyMatch(isIndexingSupportSortByString());
    }

    static boolean isCollectionSupportStartsWith(@NonNull DocumentCollection collection) {
        final IndexingPolicy policy = collection.getIndexingPolicy();
        final List<Index> indices = new ArrayList<>();

        policy.getIncludedPaths().forEach(p -> indices.addAll(p.getIndexes()));

        return indices.stream().anyMatch(isIndexingSupportStartsWith());
    }

    private static Predicate<Index> isIndexingSupportSortByString() {
        return index -> {
            if (index instanceof RangeIndex) {
                final RangeIndex rangeIndex = (RangeIndex) index;
                return rangeIndex.getDataType() == DataType.String && rangeIndex.getPrecision() == -1;
            }

            return false;
        };
    }

    private static Predicate<Index> isIndexingSupportStartsWith() {
        return index -> {
            if (index instanceof RangeIndex) {
                final RangeIndex rangeIndex = (RangeIndex) index;
                return rangeIndex.getDataType() == DataType.String;
            }

            return false;
        };
    }
}
