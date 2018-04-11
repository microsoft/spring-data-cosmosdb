/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import com.microsoft.azure.spring.data.documentdb.TestConstants;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryUnitTest {

    @Test
    public void testConstruction() {

        final Criteria c = Criteria.value(TestConstants.CRITERIA_KEY,
                CriteriaType.IS_EQUAL, Arrays.asList(new Object[] {TestConstants.CRITERIA_OBJECT}));

        final Query query = new Query(c, null);

        assertThat(query.getCriteria()).isEqualTo(c);
        assertThat(query.getSort()).isNull();
    }
}
