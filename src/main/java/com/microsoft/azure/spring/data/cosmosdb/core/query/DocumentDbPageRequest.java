/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import org.springframework.data.domain.PageRequest;

/**
 * DocumentDbPageRequest representing page request during pagination query, field
 * {@link com.microsoft.azure.documentdb.FeedResponse#getResponseContinuation response continuation token} is saved
 * to help query next page.
 * <p>
 * The requestContinuation token should be saved after each request and reused in later queries.
 */
public class DocumentDbPageRequest extends PageRequest {
    private static final long serialVersionUID = 6093304300037688375L;

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

    @Override
    public int hashCode() {
        int result = super.hashCode();

        result = 31 * result + (requestContinuation != null ? requestContinuation.hashCode() : 0);

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DocumentDbPageRequest)) {
            return false;
        }

        final DocumentDbPageRequest that = (DocumentDbPageRequest) obj;

        return super.equals(that) && (requestContinuation != null ?
                requestContinuation.equals(that.requestContinuation) : that.requestContinuation == null);
    }
}
