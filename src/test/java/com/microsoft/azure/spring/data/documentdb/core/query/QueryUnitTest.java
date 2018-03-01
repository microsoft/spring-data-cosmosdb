/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryUnitTest {

    @Test
    public void testAddCriteria() {
        final Criteria criteria = new Criteria(new ArrayList<>(), "name");
        criteria.is("test");

        final Query query = new Query().addCriteria(criteria);

        assertThat(query.getCriteria().size()).isEqualTo(1);
        assertThat(query.getCriteria().get("name")).isEqualTo("test");
    }

    @Test
    public void testWhere() {
        final Query query = new Query((Criteria.where("name").is("test")));
        assertThat(query.getCriteria().size()).isEqualTo(1);
        assertThat(query.getCriteria().get("name")).isEqualTo("test");
    }
}
