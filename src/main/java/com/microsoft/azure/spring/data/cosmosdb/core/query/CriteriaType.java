/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.repository.query.parser.Part;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum CriteriaType {

    ALL(""),
    IS_EQUAL("="),
    OR("OR"),
    AND("AND"),
    BEFORE("<"),
    AFTER(">"),
    GREATER_THAN(">"),
    GREATER_THAN_EQUAL(">=");

    @Getter
    private String sqlKeyword;

    // Map Part.Type to CriteriaType
    private static final Map<Part.Type, CriteriaType> PART_TREE_TYPE_TO_CRITERIA;

    static {
        final Map<Part.Type, CriteriaType> map = new HashMap<>();

        map.put(Part.Type.SIMPLE_PROPERTY, CriteriaType.IS_EQUAL);
        map.put(Part.Type.BEFORE, CriteriaType.BEFORE);
        map.put(Part.Type.AFTER, CriteriaType.AFTER);
        map.put(Part.Type.GREATER_THAN, CriteriaType.GREATER_THAN);
        map.put(Part.Type.GREATER_THAN_EQUAL, CriteriaType.GREATER_THAN_EQUAL);

        PART_TREE_TYPE_TO_CRITERIA = Collections.unmodifiableMap(map);
    }

    /**
     * Check if PartType is NOT supported.
     *
     * @param partType
     * @return True if unsupported, or false.
     */
    public static boolean isPartTypeUnSupported(@NonNull Part.Type partType) {
        return !isPartTypeSupported(partType);
    }

    /**
     * Check if PartType is supported.
     *
     * @param partType
     * @return True if supported, or false.
     */
    public static boolean isPartTypeSupported(@NonNull Part.Type partType) {
        return PART_TREE_TYPE_TO_CRITERIA.containsKey(partType);
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
     *
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
     *
     * @param type
     * @return True if contains, or false.
     */
    public static boolean isUnary(CriteriaType type) {
        switch (type) {
            case IS_EQUAL:
            case BEFORE:
            case AFTER:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
                return true;
            default:
                return false;
        }
    }
}
