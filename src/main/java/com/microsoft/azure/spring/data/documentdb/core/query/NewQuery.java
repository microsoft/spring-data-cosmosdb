/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
public class NewQuery {

    @Getter
    private Criteria criteria;

    public NewQuery(@NonNull Criteria criteria) {
        this.criteria = criteria;
    }
}


