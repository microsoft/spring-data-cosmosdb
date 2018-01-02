/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
import org.springframework.util.Assert;

public class SimpleDocumentDbEntityMetadata<T> implements DocumentDbEntityMetadata<T> {

    private final Class<T> type;
    private final DocumentDbEntityInformation<T, String> entityInformation;

    public SimpleDocumentDbEntityMetadata(Class<T> type, DocumentDbEntityInformation<T, String> entityInformation) {
        Assert.notNull(type, "type must not be null!");
        Assert.notNull(entityInformation, "entityInformation must not be null!");

        this.type = type;
        this.entityInformation = entityInformation;
    }

    public Class<T> getJavaType() {
        return type;
    }

    public String getCollectionName() {
        return entityInformation.getCollectionName();
    }
}
