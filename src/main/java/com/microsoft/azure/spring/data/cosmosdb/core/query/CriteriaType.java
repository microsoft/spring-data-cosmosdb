/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import lombok.Getter;
import org.springframework.data.repository.query.parser.Part;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.spring.data.cosmosdb.Constants.SQL_KEYWORD_AND;
import static com.microsoft.azure.spring.data.cosmosdb.Constants.SQL_KEYWORD_OR;

public enum CriteriaType {
    IS_EQUAL,
    OR,
    AND;

    @Getter
    private static final Map<Part.Type, CriteriaType> criteriaMap;

    static {
        final Map<Part.Type, CriteriaType> map = new HashMap<>();

        map.put(Part.Type.SIMPLE_PROPERTY, CriteriaType.IS_EQUAL);

        criteriaMap = Collections.unmodifiableMap(map);
    }

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
