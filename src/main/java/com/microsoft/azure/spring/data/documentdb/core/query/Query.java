/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import com.microsoft.azure.spring.data.documentdb.exception.IllegalQueryException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Query {

    private final Map<String, Object> criteria = new LinkedHashMap<>();

    public static Query query(Criteria criteria) {
        return new Query(criteria);
    }

    public Query() {
    }

    public Query(Criteria criteria) {
        final List<Criteria> criteriaList = criteria.getCriteriaChain();
        for (final Criteria c : criteriaList) {
            addCriteria(c);
        }
    }

    public Query addCriteria(CriteriaDefinition criteriaDefinition) {
        final Object existing = this.criteria.get(criteriaDefinition.getKey());

        if (existing == null) {
            this.criteria.put(criteriaDefinition.getKey(), criteriaDefinition.getCriteriaObject());
        } else {
            throw new IllegalQueryException("invalid criteriaDefinition, criteria already exists.");
        }
        return this;
    }

    public Map<String, Object> getCriteria() {
        return this.criteria;
    }
}


