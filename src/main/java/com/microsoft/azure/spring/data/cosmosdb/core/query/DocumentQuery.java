/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

public class DocumentQuery {

    @Getter
    private final Criteria criteria;

    public DocumentQuery(@NonNull Criteria criteria) {
        this.criteria = criteria;
    }

    private Optional<Criteria> getSubjectCriteriaDfs(@NonNull Criteria criteria, @NonNull String keyName) {
        if (keyName.equals(criteria.getSubject())) {
            return Optional.of(criteria);
        }

        final List<Criteria> subCriteriaList = criteria.getSubCriteria();

        for (final Criteria c : subCriteriaList) {
            final Optional<Criteria> subjectCriteria = getSubjectCriteriaDfs(c, keyName);

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
            return getSubjectCriteriaDfs(criteria, subjectName);
        } else {
            return Optional.empty();
        }
    }

    private boolean nonPartitionKeyContainsDfs(@NonNull Criteria criteria, @NonNull List<String> partitionKeys) {
        if (!partitionKeys.contains(criteria.getSubject())) {
            return true;
        }

        final List<Criteria> subCriteriaList = criteria.getSubCriteria();

        for (final Criteria c : subCriteriaList) {
            if (nonPartitionKeyContainsDfs(c, partitionKeys)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if Document Query contains Non-PartitionKey.
     *
     * @param partitionKeys the partitionKey name list.
     * @return true if contains Non-PartitionKey, or return false.
     */
    public boolean nonPartitionKeyContains(@NonNull List<String> partitionKeys) {
        if (partitionKeys.isEmpty()) {
            return true;
        } else {
            return nonPartitionKeyContainsDfs(criteria, partitionKeys);
        }
    }
}
