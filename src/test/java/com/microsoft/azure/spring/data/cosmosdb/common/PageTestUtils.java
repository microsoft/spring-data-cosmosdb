/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.common;

import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class PageTestUtils {
    public static void validateLastPage(Page page, int pageSize) {
        final Pageable pageable = page.getPageable();

        assertThat(pageable).isInstanceOf(DocumentDbPageRequest.class);
        assertTrue(continuationTokenIsNull((DocumentDbPageRequest) pageable));
        assertThat(pageable.getPageSize()).isEqualTo(pageSize);
    }

    public static void validateNonLastPage(Page page, int pageSize) {
        final Pageable pageable = page.getPageable();

        assertThat(pageable).isInstanceOf(DocumentDbPageRequest.class);
        assertThat(((DocumentDbPageRequest) pageable).getRequestContinuation()).isNotNull();
        assertThat(((DocumentDbPageRequest) pageable).getRequestContinuation()).isNotBlank();
        assertThat(pageable.getPageSize()).isEqualTo(pageSize);
    }

    private static boolean continuationTokenIsNull(DocumentDbPageRequest pageRequest) {
        final String tokenJson = pageRequest.getRequestContinuation();
        if (tokenJson == null) {
            return true;
        }

        final JSONObject jsonObject = new JSONObject(tokenJson);

        return jsonObject.isNull("token");
    }
}
