/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import org.springframework.data.repository.query.ParametersParameterAccessor;

import java.util.Arrays;
import java.util.List;

/**
 * {@link DocumentDbParameterParameterAccessor} is deprecated.
 * Instead use CosmosParameterParameterAccessor, which is introduced in 2.2.0 version.
 */
@Deprecated
public class DocumentDbParameterParameterAccessor extends ParametersParameterAccessor
        implements DocumentDbParameterAccessor {

    private final List<Object> values;

    public DocumentDbParameterParameterAccessor(DocumentDbQueryMethod method, Object[] values) {
        super(method.getParameters(), values);

        this.values = Arrays.asList(values);
    }

    @Override
    public Object[] getValues() {
        return values.toArray();
    }
}
