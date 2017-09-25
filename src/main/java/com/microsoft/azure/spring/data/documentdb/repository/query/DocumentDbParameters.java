/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;

import java.lang.reflect.Method;
import java.util.List;

public class DocumentDbParameters extends Parameters<DocumentDbParameters, DocumentDbParameter> {

    public DocumentDbParameters(Method method) {
        super(method);
    }

    private DocumentDbParameters(List<DocumentDbParameter> parameters) {
        super(parameters);
    }

    @Override
    protected DocumentDbParameters createFrom(List<DocumentDbParameter> parameters) {
        return new DocumentDbParameters(parameters);
    }

    @Override
    protected DocumentDbParameter createParameter(MethodParameter parameter) {
        return new DocumentDbParameter(parameter);
    }
}
