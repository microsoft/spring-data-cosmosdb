/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentDbOperationValidator {

    private static void validateId(Object id) {
        Assert.notNull(id, "id should not be null");

        if (id instanceof String) {
            Assert.hasText(id.toString(), "id should not be empty or only whitespaces.");
        }
    }

    public static void validate(Pageable pageable) {
        Assert.isTrue(pageable.getPageSize() > 0, "pageable should have page size larger than 0");
    }

    public static void validate(String collectionName) {
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces.");
    }

    public static void validate(DocumentQuery query) {
        Assert.notNull(query, "DocumentQuery should not be null.");
    }

    public static void validate(Class<?> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null.");
    }

    public static void validate(Object entity) {
        Assert.notNull(entity, "entity should not be null.");
    }

    public static void validate(DocumentDbEntityInformation information) {
        Assert.notNull(information, "entityInformation should not be null.");
    }

    public static void validate(String collectionName, Object entity) {
        validate(collectionName);
        validate(entity);
    }

    public static void validate(Object id, String collectionName) {
        validateId(id);
        validate(collectionName);
    }

    public static void validate(Object id, String collectionName, Class<?> entityClass) {
        validateId(id);
        validate(collectionName);
        validate(entityClass);
    }

    public static void validate(DocumentQuery query, String collectionName) {
        validate(query);
        validate(collectionName);
    }

    public static void validate(DocumentQuery query, String collectionName, Class<?> entityClass, Pageable pageable) {
        validate(query, collectionName, entityClass);
        validateId(pageable);
    }
}
