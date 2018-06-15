/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class NewCriteria {
    
    @Getter
    private final CriteriaType criteriaType;

    @Getter
    private final List<NewCriteria> criteriaList = new ArrayList<>();

    @Getter
    private String criteriaSubject;

    @Getter
    private List<Object> criteriaValues = new ArrayList<>();

    @Getter
    private boolean caseSensitive;

    @Getter
    private boolean negated;

    private NewCriteria(CriteriaType criteriaType) {
        this.criteriaType = criteriaType;
        this.caseSensitive = false;
        this.negated = false;
    }

    public static NewCriteria and(NewCriteria left, NewCriteria right) {

        return linkingCondition(CriteriaType.AND_CONDITION, left, right);
    }

    public static NewCriteria or(NewCriteria left, NewCriteria right) {

        return linkingCondition(CriteriaType.OR_CONDITION, left, right);
    }

    private static NewCriteria linkingCondition(CriteriaType condition, NewCriteria left, NewCriteria right) {

        final NewCriteria criteria = new NewCriteria(condition);
        
        criteria.criteriaList.add(left);
        criteria.criteriaList.add(right);
        
        return criteria;
    }
    
    public static NewCriteria value(String conditionSubject, CriteriaType condition, List<Object> conditionValues) {
        return value(conditionSubject, condition, conditionValues, false, false);
    }

    public static NewCriteria value(String conditionSubject, CriteriaType condition,
            List<Object> conditionValues, boolean ignoreCase) {
        return value(conditionSubject, condition, conditionValues, ignoreCase, false);
    }
    
    public static NewCriteria value(String conditionSubject, CriteriaType condition,
            List<Object> conditionValues, boolean ignoreCase, boolean negated) {

        final NewCriteria criteria = new NewCriteria(condition);
        
        criteria.criteriaSubject = conditionSubject;
        criteria.criteriaValues = conditionValues;
        criteria.caseSensitive = ignoreCase;
        
        return criteria;
    }
    
}
