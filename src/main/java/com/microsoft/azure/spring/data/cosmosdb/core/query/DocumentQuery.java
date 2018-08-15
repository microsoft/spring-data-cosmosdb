/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

public class DocumentQuery {

    @Getter
    private final Criteria criteria;

    @Getter
    private Sort sort = Sort.unsorted();

    public DocumentQuery(@NonNull Criteria criteria) {
        this.criteria = criteria;
    }

    public DocumentQuery with(@NonNull Sort sort) {
        Assert.notNull(sort, "Sort should not be null");

        if (sort.isSorted()) {
            this.sort = this.sort.and(sort);
        }

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

    /**
     * Check if subject Criteria of Query contains given subject name.
     *
     * @param subjectName the name of subject
     * @return true if contains, or false.
     */
    public boolean containsSubject(@NonNull String subjectName) {
        return this.getSubjectCriteria(subjectName).isPresent();
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
}
