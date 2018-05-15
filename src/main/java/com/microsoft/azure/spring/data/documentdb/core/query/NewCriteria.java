/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import lombok.Getter;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NewCriteria {

    private CriteriaType type;
    private String subject;
    private List<NewCriteria> criteriaList;
    private List<Object> criteriaValues;

    public NewCriteria(CriteriaType criteriaType) {
        this.type = criteriaType;
        this.criteriaList = new ArrayList<>();
    }

    public static NewCriteria getInstance(@NonNull String subject, CriteriaType type, @NonNull List<Object> values) {
        final NewCriteria criteria = new NewCriteria(type);

        criteria.subject = subject;
        criteria.criteriaValues = values;

        return criteria;
    }

    public static NewCriteria getAndInstance(@NonNull NewCriteria left, @NonNull NewCriteria right) {
       final NewCriteria criteria = new NewCriteria(CriteriaType.AND);

       criteria.criteriaList.add(left);
       criteria.criteriaList.add(right);

       return criteria;
    }

    public static NewCriteria getOrInstance(@NonNull NewCriteria left, @NonNull NewCriteria right) {
        final NewCriteria criteria = new NewCriteria(CriteriaType.OR);

        criteria.criteriaList.add(left);
        criteria.criteriaList.add(right);

        return criteria;
    }
}
