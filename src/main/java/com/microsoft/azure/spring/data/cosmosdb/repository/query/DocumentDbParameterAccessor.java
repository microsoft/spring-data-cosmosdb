/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import org.springframework.data.repository.query.ParameterAccessor;

/**
 * {@link DocumentDbParameterAccessor} is deprecated.
 * Instead use CosmosParameterAccessor, which is introduced in 2.2.0 version.
 */
@Deprecated
public interface DocumentDbParameterAccessor extends ParameterAccessor {
    Object[] getValues();
}
