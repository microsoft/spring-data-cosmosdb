/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import com.microsoft.azure.spring.data.documentdb.TestConstants;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryUnitTest {

    @Test
    public void testAddCriteria() {
        final Criteria criteria = new Criteria(new ArrayList<>(), TestConstants.CRITERIA_KEY);
        criteria.is(TestConstants.CRITERIA_OBJECT);

        final Query query = new Query().addCriteria(criteria);

        assertThat(query.getCriteria().size()).isEqualTo(1);
        assertThat(query.getCriteria().get(TestConstants.CRITERIA_KEY)).isEqualTo(TestConstants.CRITERIA_OBJECT);
    }

    @Test
    public void testWhere() {
        final Query query = new Query((Criteria.where(TestConstants.CRITERIA_KEY).is(TestConstants.CRITERIA_OBJECT)));
        assertThat(query.getCriteria().size()).isEqualTo(1);
        assertThat(query.getCriteria().get(TestConstants.CRITERIA_KEY)).isEqualTo(TestConstants.CRITERIA_OBJECT);
    }
}
