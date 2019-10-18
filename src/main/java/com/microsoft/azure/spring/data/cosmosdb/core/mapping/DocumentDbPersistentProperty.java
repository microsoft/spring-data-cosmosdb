/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core.mapping;

import org.springframework.data.mapping.PersistentProperty;

/**
 * {@link DocumentDbPersistentProperty} is deprecated.
 * Instead use CosmosPersistentProperty, which is introduced in 2.2.0 version.
 */
@Deprecated
public interface DocumentDbPersistentProperty extends PersistentProperty<DocumentDbPersistentProperty> {
}
