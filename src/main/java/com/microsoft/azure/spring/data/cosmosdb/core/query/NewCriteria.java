/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import java.util.ArrayList;
import java.util.List;

public class NewCriteria {
    
    private final CriteriaType criteriaType;
    private final List<NewCriteria> criteriaList = new ArrayList<>();

    private String criteriaSubject;
    private List<Object> criteriaValues = new ArrayList<>();
    private boolean shouldIgnoreCase;
    private boolean negated;

    private NewCriteria(CriteriaType criteriaType) {
        this.criteriaType = criteriaType;
        this.shouldIgnoreCase = false;
        this.negated = false;
    }

    public static NewCriteria and(NewCriteria left, NewCriteria right) {

        final NewCriteria criteria = new NewCriteria(CriteriaType.AND_CONDITION);
        
        criteria.criteriaList.add(left);
        criteria.criteriaList.add(right);

        return criteria;
    }

    public static NewCriteria or(NewCriteria left, NewCriteria right) {

        final NewCriteria criteria = new NewCriteria(CriteriaType.OR_CONDITION);
        
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
        criteria.shouldIgnoreCase = ignoreCase;
        
        return criteria;
    }

    
    public List<NewCriteria> getCriteriaList() {
        return criteriaList;
    }
    
    public String getCriteriaSubject() {
        return criteriaSubject;
    }

    public List<Object> getCriteriaValues() {
        return criteriaValues;
    }

    public boolean shouldIgnoreCase() {
        return shouldIgnoreCase;
    }

    public CriteriaType getCriteriaType() {
        return criteriaType;
    }
    
    public boolean isNegated() {
        return negated;
    }
}
