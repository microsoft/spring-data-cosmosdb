/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import lombok.Getter;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Criteria {

    private String subject;
    private List<Object> subjectValues;
    private final CriteriaType type;
    private final List<Criteria> subCriteria;

    private Criteria(CriteriaType type) {
        this.type = type;
        this.subCriteria = new ArrayList<>();
    }

    public static Criteria getInstance(CriteriaType type, @NonNull String subject, @NonNull List<Object> values) {
        final Criteria criteria = new Criteria(type);

        criteria.subject = subject;
        criteria.subjectValues = values;

        return criteria;
    }

    public static Criteria getInstance(CriteriaType type, @NonNull Criteria left, @NonNull Criteria right) {
        final Criteria criteria = new Criteria(type);

        criteria.subCriteria.add(left);
        criteria.subCriteria.add(right);

        return criteria;
    }

    public static Criteria getInstance(CriteriaType type) {
        return new Criteria(type);
    }
}
