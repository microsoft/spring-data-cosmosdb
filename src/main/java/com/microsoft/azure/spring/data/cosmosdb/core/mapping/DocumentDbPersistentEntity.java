/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

/**
 * {@link DocumentDbPersistentEntity} is deprecated.
 * Instead use CosmosPersistentEntity, which is introduced in 2.2.0 version.
 */
@Deprecated
public interface DocumentDbPersistentEntity<T> extends PersistentEntity<T, DocumentDbPersistentProperty> {

    String getCollection();

    String getLanguage();
}
