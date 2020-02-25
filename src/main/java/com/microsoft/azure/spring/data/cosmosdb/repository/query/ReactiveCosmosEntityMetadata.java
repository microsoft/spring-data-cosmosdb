/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import org.springframework.data.repository.core.EntityMetadata;

public interface ReactiveCosmosEntityMetadata<T> extends EntityMetadata {
    @Deprecated
    String getCollectionName();

    String getContainerName();
}
