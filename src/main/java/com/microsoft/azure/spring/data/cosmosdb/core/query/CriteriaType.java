/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.repository.query.parser.Part;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.spring.data.cosmosdb.Constants.*;

public enum CriteriaType {
    IS_EQUAL,
    OR,
    AND,
    BEFORE;

    @Getter
    // Map Part.Type to CriteriaType
    private static final Map<Part.Type, CriteriaType> PART_TREE_TYPE_TO_CRITERIA;

    // Map CriteriaType to Sql Keyword String
    private static final Map<CriteriaType, String> CRITERIA_TYPE_TO_SQL_KEYWORD;

    static {
        final Map<Part.Type, CriteriaType> criteriaMap = new HashMap<>();
        final Map<CriteriaType, String> keywordMap = new HashMap<>();

        criteriaMap.put(Part.Type.SIMPLE_PROPERTY, CriteriaType.IS_EQUAL);
        criteriaMap.put(Part.Type.BEFORE, CriteriaType.BEFORE);

        keywordMap.put(CriteriaType.IS_EQUAL, SQL_KEYWORD_IS_EQUAL);
        keywordMap.put(CriteriaType.AND, SQL_KEYWORD_AND);
        keywordMap.put(CriteriaType.OR, SQL_KEYWORD_OR);
        keywordMap.put(CriteriaType.BEFORE, SQL_KEYWORD_BEFORE);

        PART_TREE_TYPE_TO_CRITERIA = Collections.unmodifiableMap(criteriaMap);
        CRITERIA_TYPE_TO_SQL_KEYWORD = Collections.unmodifiableMap(keywordMap);
    }

    /**
     * Convert CriteriaType to Sql keyword.
     *
     * @param criteriaType
     * @return Sql keyword String of CriteriaType.
     */
    public static String toSqlKeyword(@NonNull CriteriaType criteriaType) {
        final String keyword = CRITERIA_TYPE_TO_SQL_KEYWORD.get(criteriaType);

        if (keyword == null) {
            throw new UnsupportedOperationException("Unsupported criteria type: " + criteriaType);
        }

        return keyword;
    }

    /**
     * Check if PartType is NOT supported.
     *
     * @param partType
     * @return True if unsupported, or false.
     */
    public static boolean isPartTypeUnSupported(@NonNull Part.Type partType) {
        return !PART_TREE_TYPE_TO_CRITERIA.containsKey(partType);
    }

    public static CriteriaType toCriteriaType(@NonNull Part.Type partType) {
        final CriteriaType criteriaType = PART_TREE_TYPE_TO_CRITERIA.get(partType);

        if (criteriaType == null) {
            throw new UnsupportedOperationException("Unsupported part type: " + partType);
        }

        return criteriaType;
    }

    /**
     * Check if CriteriaType operation contains two subjects.
     * @param type
     * @return True if contains, or false.
     */
    public static boolean isBinary(CriteriaType type) {
        switch (type) {
            case AND:
            case OR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if CriteriaType operation contains only one subjects.
     * @param type
     * @return True if contains, or false.
     */
    public static boolean isUnary(CriteriaType type) {
        switch (type) {
            case IS_EQUAL:
            case BEFORE:
                return true;
            default:
                return false;
        }
    }
}
