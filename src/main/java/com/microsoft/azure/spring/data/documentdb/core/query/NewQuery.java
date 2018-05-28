/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import org.springframework.data.domain.Sort;

public class NewQuery {

    NewCriteria criteria;
    Sort sort;

    public NewQuery(NewCriteria criteria, Sort sort) {
        
        this.criteria = criteria;
        this.sort = sort;
    }

    public NewCriteria getCriteria() {
        return criteria;
    }

    public Sort getSort() {
        return sort;
    }
}
