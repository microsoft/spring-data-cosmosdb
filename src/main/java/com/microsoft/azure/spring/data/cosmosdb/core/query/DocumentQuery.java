/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.reflect.FieldUtils;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.exception.IllegalQueryException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DocumentQuery {

    @Getter
    private final Criteria criteria;

    @Getter
    private Sort sort = Sort.unsorted();

    @Getter
    private Pageable pageable = Pageable.unpaged();

    public DocumentQuery(@NonNull Criteria criteria) {
        this.criteria = criteria;
    }

    public DocumentQuery with(@NonNull Sort sort) {
        if (sort.isSorted()) {
            this.sort = sort.and(this.sort);
        }

        return this;
    }

    public DocumentQuery with(@NonNull Pageable pageable) {
        Assert.notNull(pageable, "pageable should not be null");

        this.pageable = pageable;
        return this;
    }

    private boolean isCrossPartitionQuery(@NonNull String keyName) {
        Assert.hasText(keyName, "PartitionKey should have text.");

        final Optional<Criteria> criteria = this.getSubjectCriteria(this.criteria, keyName);

        return criteria.map(criteria1 -> criteria1.getType() != CriteriaType.IS_EQUAL).orElse(true);
    }

    private boolean hasKeywordOr() {
        // If there is OR keyword in DocumentQuery, the top node of Criteria must be OR type.
        return this.criteria.getType() == CriteriaType.OR;
    }

    /**
     * Indicate if DocumentQuery should enable cross partition query.
     *
     * @param partitionKeys The list of partitionKey names.
     * @return
     */
    public boolean isCrossPartitionQuery(@NonNull List<String> partitionKeys) {
        if (partitionKeys.isEmpty()) {
            return true;
        }

        for (final String keyName : partitionKeys) {
            if (isCrossPartitionQuery(keyName)) {
                return true;
            }
        }

        return this.hasKeywordOr();
    }

    private Optional<Criteria> getSubjectCriteria(@NonNull Criteria criteria, @NonNull String keyName) {
        if (keyName.equals(criteria.getSubject())) {
            return Optional.of(criteria);
        }

        final List<Criteria> subCriteriaList = criteria.getSubCriteria();

        for (final Criteria c : subCriteriaList) {
            final Optional<Criteria> subjectCriteria = getSubjectCriteria(c, keyName);

            if (subjectCriteria.isPresent()) {
                return subjectCriteria;
            }
        }

        return Optional.empty();
    }

    /**
     * Validate if Sort is valid for cosmosdb.
     *
     * @param domainClass                     class of domain
     * @param isCollectionSupportSortByString indicate if collection support sort by String.
     */
    public void validateSort(@NonNull Class<?> domainClass, boolean isCollectionSupportSortByString) {
        if (this.sort.isUnsorted()) {
            return;
        }

        if (sort.stream().count() != 1) {
            throw new IllegalQueryException("only one order of Sort is supported");
        }

        final Sort.Order order = sort.iterator().next();
        final String property = order.getProperty();
        final String idFieldName = new DocumentDbEntityInformation<>(domainClass).getIdField().getName();
        final Field[] fields = FieldUtils.getAllFields(domainClass);
        final Optional<Field> field = Arrays.stream(fields).filter(f -> f.getName().equals(property)).findFirst();

        if (order.isIgnoreCase()) {
            throw new IllegalQueryException("sort within case insensitive is not supported");
        } else if (property.equals(Constants.ID_PROPERTY_NAME) || property.equals(idFieldName)) {
            throw new IllegalQueryException("sort by @Id field is not supported");
        } else if (!field.isPresent()) {
            throw new IllegalQueryException("order name must be consistence with domainClass");
        } else if (field.get().getType() == String.class && !isCollectionSupportSortByString) {
            throw new IllegalQueryException("order by String must enable indexing with Range and max Precision.");
        }
    }

    public void validateStartsWith(@NonNull Class<?> domainClass, boolean isCollectionSupportStartswith) {
        final Field[] fields = FieldUtils.getAllFields(domainClass);
        final Optional<Field> field = Arrays.stream(fields)
                .filter(f -> f.getName().equals(this.criteria.getSubject())).findFirst();
        if (field.get().getType() == String.class && !isCollectionSupportStartswith) {
            throw new IllegalQueryException("STARTSWITH keyword must enable indexing with Range and max Precision.");
        }
    }
}
