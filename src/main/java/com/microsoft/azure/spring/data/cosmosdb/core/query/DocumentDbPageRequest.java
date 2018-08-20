/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import org.springframework.data.domain.PageRequest;

public class DocumentDbPageRequest extends PageRequest {
    // Request continuation token used to resume query
    private String requestContinuation;

    public DocumentDbPageRequest(int page, int size, String requestContinuation) {
        super(page, size);
        this.requestContinuation = requestContinuation;
    }

    public static DocumentDbPageRequest of(int page, int size, String requestContinuation) {
        return new DocumentDbPageRequest(page, size, requestContinuation);
    }

    public String getRequestContinuation() {
        return this.requestContinuation;
    }
}
