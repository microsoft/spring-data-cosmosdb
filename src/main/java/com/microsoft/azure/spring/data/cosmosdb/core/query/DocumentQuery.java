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
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DocumentQuery {

    @Getter
    private final Criteria criteria;

    @Getter
    private Sort sort;

    @Getter
    private Pageable pageable = Pageable.unpaged();

    public DocumentQuery(@NonNull Criteria criteria, @NonNull Sort sort) {
        this.criteria = criteria;
        this.sort = sort;
    }

    public DocumentQuery with(@NonNull Pageable pageable) {
        Assert.notNull(pageable, "pageable should not be null");

        this.pageable = pageable;
        return this;
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
     * Get the criteria with given subject name.
     *
     * @param subjectName the Name of method subject.
     * @return Optional of criteria if success, or Optional.empty().
     */
    public Optional<Criteria> getSubjectCriteria(@NonNull String subjectName) {
        if (StringUtils.hasText(subjectName)) {
            return getSubjectCriteria(criteria, subjectName);
        } else {
            return Optional.empty();
        }
    }

    private boolean hasPartitionKeyOnly(@NonNull List<String> partitionKeys, @NonNull Criteria criteria) {
        if (!partitionKeys.contains(criteria.getSubject())) {
            return false;
        }

        final List<Criteria> subCriteriaList = criteria.getSubCriteria();

        for (final Criteria c : subCriteriaList) {
            if (!hasPartitionKeyOnly(partitionKeys, c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if Document Query subjects only have partition Key
     *
     * @param partitionKeys the partitionKey name list.
     * @return true if Query got only partition Key, or return false.
     */
    public boolean hasPartitionKeyOnly(@NonNull List<String> partitionKeys) {
        if (partitionKeys.isEmpty()) {
            return true;
        } else {
            return hasPartitionKeyOnly(partitionKeys, criteria);
        }
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
}
