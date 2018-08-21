/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.documentdb.SqlParameter;
import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter.toDocumentDBValue;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractQueryGenerator {

    private String generateUnaryQuery(@NonNull Criteria criteria, @NonNull List<Pair<String, Object>> parameters) {
        Assert.isTrue(criteria.getSubjectValues().size() == 1, "Unary criteria should have only one subject value");
        Assert.isTrue(CriteriaType.isUnary(criteria.getType()), "Criteria type should be unary operation");

        final String subject = criteria.getSubject();
        final Object subjectValue = toDocumentDBValue(criteria.getSubjectValues().get(0));

        parameters.add(Pair.with(subject, subjectValue));

        if (CriteriaType.isFunction(criteria.getType())) {
            return String.format("%s(r.%s, @%s)", criteria.getType().getSqlKeyword(), subject, subject);
        } else {
            return String.format("r.%s %s @%s", subject, criteria.getType().getSqlKeyword(), subject);
        }
    }

    private String generateBinaryQuery(@NonNull String left, @NonNull String right, CriteriaType type) {
        Assert.isTrue(CriteriaType.isBinary(type), "Criteria type should be binary operation");

        return String.join(" ", left, type.getSqlKeyword(), right);
    }

    private String generateQueryBody(@NonNull Criteria criteria, @NonNull List<Pair<String, Object>> parameters) {
        final CriteriaType type = criteria.getType();

        switch (type) {
            case ALL:
                return "";
            case IS_EQUAL:
            case BEFORE:
            case AFTER:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case CONTAINING:
            case ENDS_WITH:
                return this.generateUnaryQuery(criteria, parameters);
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
    private Pair<String, List<Pair<String, Object>>> generateQueryBody(@NonNull DocumentQuery query) {
        final List<Pair<String, Object>> parameters = new ArrayList<>();
        String queryString = this.generateQueryBody(query.getCriteria(), parameters);

        if (StringUtils.hasText(queryString)) {
            queryString = String.join(" ", "WHERE", queryString);
        }

        return Pair.with(queryString, parameters);
    }

    private String getParameter(@NonNull Sort.Order order) {
        Assert.isTrue(!order.isIgnoreCase(), "Ignore case is not supported");

        final String direction = order.isDescending() ? "DESC" : "ASC";

        return String.format("r.%s %s", order.getProperty(), direction);
    }

    private String generateQuerySort(@NonNull Sort sort) {
        if (sort.isUnsorted()) {
            return "";
        }

        final String queryTail = "ORDER BY";
        final List<String> subjects = sort.stream().map(this::getParameter).collect(Collectors.toList());

        return queryTail + " " + String.join(",", subjects);
    }

    @NonNull
    private String generateQueryTail(@NonNull DocumentQuery query) {
        final List<String> queryTails = new ArrayList<>();

        queryTails.add(generateQuerySort(query.getSort()));

        return String.join(" ", queryTails.stream().filter(StringUtils::hasText).collect(Collectors.toList()));
    }

    /**
     * Generate SqlQuerySpec with given DocumentQuery and query head.
     *
     * @param query     DocumentQuery represent one query method.
     * @param queryHead
     * @return The SqlQuerySpec for DocumentClient.
     */
    protected SqlQuerySpec generateQuery(@NonNull DocumentQuery query, @NonNull String queryHead) {
        Assert.hasText(queryHead, "query head should have text.");

        final Pair<String, List<Pair<String, Object>>> queryBody = generateQueryBody(query);
        final String queryString = String.join(" ", queryHead, queryBody.getValue0(), generateQueryTail(query));
        final List<Pair<String, Object>> parameters = queryBody.getValue1();
        final SqlParameterCollection sqlParameters = new SqlParameterCollection();

        sqlParameters.addAll(
                parameters.stream()
                        .map(p -> new SqlParameter("@" + p.getValue0(), toDocumentDBValue(p.getValue1())))
                        .collect(Collectors.toList())
        );

        return new SqlQuerySpec(queryString, sqlParameters);
    }
}
