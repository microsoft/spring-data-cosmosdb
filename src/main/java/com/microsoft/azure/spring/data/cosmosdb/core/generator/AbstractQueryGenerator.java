/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.javatuples.Pair;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.spring.data.cosmosdb.Constants.ID_PROPERTY_NAME;

public abstract class AbstractQueryGenerator {

    protected final DocumentDbEntityInformation information;

    @SuppressWarnings("unchecked")
    protected <T> AbstractQueryGenerator(@NonNull Class<T> domainClass) {
        this.information = new DocumentDbEntityInformation(domainClass);
    }

    private String getCriteriaSubject(@NonNull Criteria criteria) {
        String subject = criteria.getSubject();

        if (subject.equals(information.getIdField().getName())) {
            subject = ID_PROPERTY_NAME;
        }

        return subject;
    }

    private String generateIsEqual(@NonNull Criteria criteria, @NonNull List<Pair<String, Object>> parameters) {
        final String subject = this.getCriteriaSubject(criteria);

        Assert.isTrue(criteria.getSubjectValues().size() == 1, "IS_EQUAL should have only one subject value");

        parameters.add(Pair.with(subject, criteria.getSubjectValues().get(0)));

        return String.format("r.%s=@%s", subject, subject);
    }

    private String generateBinaryQuery(@NonNull String left, @NonNull String right, CriteriaType type) {
        Assert.isTrue(Criteria.isBinaryOperation(type), "Criteria type should be binary operation");

        final String keyword = CriteriaType.toSqlKeyword(type);

        return String.join(" ", left, keyword, right);
    }

    private String generateQueryBody(@NonNull Criteria criteria, @NonNull List<Pair<String, Object>> parameters) {
        final CriteriaType type = criteria.getType();

        switch (type) {
            case IS_EQUAL:
                return this.generateIsEqual(criteria, parameters);
            case AND:
            case OR:
                Assert.isTrue(criteria.getSubCriteria().size() == 2, "criteria should have two SubCriteria");

                final String left = generateQueryBody(criteria.getSubCriteria().get(0), parameters);
                final String right = generateQueryBody(criteria.getSubCriteria().get(1), parameters);

                return generateBinaryQuery(left, right, type);
            default:
                throw new UnsupportedOperationException("unsupported Criteria type" + type);
        }
    }

    /**
     * Generate a query body for interface QuerySpecGenerator.
     * The query body compose of Sql query String and its' parameters.
     * The parameters organized as a list of Pair, for each pair compose parameter name and value.
     *
     * @param query the representation for query method.
     * @return A pair tuple compose of Sql query.
     */
    @NonNull
    protected Pair<String, List<Pair<String, Object>>> generateQueryBody(@NonNull DocumentQuery query) {
        final List<Pair<String, Object>> parameters = new ArrayList<>();
        final String queryString = this.generateQueryBody(query.getCriteria(), parameters);

        return Pair.with(queryString, parameters);
    }
}
