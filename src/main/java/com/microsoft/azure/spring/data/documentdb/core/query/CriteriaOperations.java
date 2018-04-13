/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import com.microsoft.azure.spring.data.documentdb.Constants;
import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriteriaOperations {
    @Getter
    private String idFieldName;
    @Getter
    private final Map<String, Object> parameters = new HashMap<>();
    private int count;
    private final StringBuilder queryBuilder = new StringBuilder();

    private static Map<CriteriaType, String> conditions;

    static {
        final Map<CriteriaType, String> map = new HashMap<>();

        map.put(CriteriaType.IS_EQUAL, Constants.IS_EQUAL);

        conditions = Collections.unmodifiableMap(map);
    }

    public CriteriaOperations(@NonNull String idFieldName) {
        this.idFieldName = idFieldName;
    }

    private void parametersCountInitialize() {
        this.count = 1;
    }

    private String generateQuery(@NonNull String template, @NonNull NewCriteria criteria, @NonNull List<?> values) {
        String literal = template;
        final String subject;

        if (criteria.getSubject() != null && criteria.getSubject().equals(this.idFieldName)) {
            subject = Constants.ID_PROPERTY_NAME;
        } else {
            subject = criteria.getSubject();
        }

        literal = literal.replaceAll("@\\?", subject);

        for (final Object value : values) {
            Assert.isTrue(literal.contains("@@"), "should contain placeholder");

            final String parameterName = "@p" + count++;
            literal = literal.replaceFirst("@@", parameterName);

            this.parameters.put(parameterName, value);
        }

        return literal;
    }

    private void criteriaTraversal(NewCriteria criteria) {
        if (criteria == null) {
            return;
        }

        final CriteriaType type = criteria.getType();

        switch (type) {
            case AND:
                Assert.isTrue(criteria.getCriteriaList().size() == 2, "CriteriaList should be 2");

                this.criteriaTraversal(criteria.getCriteriaList().get(0));
                this.queryBuilder.append(Constants.CRITERIA_AND);
                this.criteriaTraversal(criteria.getCriteriaList().get(1));
                break;
            case OR:
                Assert.isTrue(criteria.getCriteriaList().size() == 2, "CriteriaList should be 2");

                this.queryBuilder.append(Constants.CRITERIA_LEFT_BRACKET);
                this.criteriaTraversal(criteria.getCriteriaList().get(0));
                this.queryBuilder.append(Constants.CRITERIA_OR);
                this.criteriaTraversal(criteria.getCriteriaList().get(1));
                this.queryBuilder.append(Constants.CRITERIA_RIGHT_BRACKET);
                break;
            case IS_EQUAL:
                final String template = conditions.get(type);
                this.queryBuilder.append(this.generateQuery(template, criteria, criteria.getCriteriaValues()));
                break;
            default:
                throw new IllegalArgumentException("Unsupported condition");
        }
    }

    public String convertToQueryLiteral(@NonNull NewCriteria criteria) {

        this.parametersCountInitialize();
        this.criteriaTraversal(criteria);

        return this.queryBuilder.toString();
    }

    private NewCriteria findCriteriaDfs(NewCriteria criteria, String name) {
        if (name == null || name.isEmpty() || criteria == null) {
            return null;
        }

        if (criteria.getSubject() != null && criteria.getSubject().equals(name)) {
            return criteria;
        }

        for (final NewCriteria c : criteria.getCriteriaList()) {
            final NewCriteria foundCriteria = this.findCriteriaDfs(c, name);

            if (foundCriteria != null) {
                return foundCriteria;
            }
        }

        return null;
    }

    public NewCriteria findCriteriaByName(@NonNull NewCriteria criteria, @NonNull String name) {
        return this.findCriteriaDfs(criteria, name);
    }
}

