/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;

public class DocumentDbParameter extends Parameter {

    public DocumentDbParameter(MethodParameter parameter) {
        super(parameter);
    }

    @Override
    public boolean isSpecialParameter() {
        return super.isSpecialParameter();
    }
}
