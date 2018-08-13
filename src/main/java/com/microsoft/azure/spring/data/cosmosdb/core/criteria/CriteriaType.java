/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.criteria;

import static com.microsoft.azure.spring.data.cosmosdb.Constants.SQL_KEYWORD_AND;
import static com.microsoft.azure.spring.data.cosmosdb.Constants.SQL_KEYWORD_OR;

public enum CriteriaType {
    IS_EQUAL,
    OR,
    AND;

    public static String toSqlKeyword(CriteriaType type) {
        switch (type) {
            case AND:
                return SQL_KEYWORD_AND;
            case OR:
                return SQL_KEYWORD_OR;
            default:
                throw new UnsupportedOperationException("Unsupported criteria type.");
        }
    }
}
