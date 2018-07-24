/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;

import org.junit.Test;

import java.util.Arrays;


import static org.assertj.core.api.Assertions.assertThat;

public class NewCriteriaUnitTest {

    @Test
    public void testSimpleValueCriteria() {

    final NewCriteria c = NewCriteria.value(TestConstants.CRITERIA_KEY,
            CriteriaType.IS_EQUAL, Arrays.asList(new Object[] {TestConstants.CRITERIA_OBJECT}));
        
        assertThat(c.getCriteriaSubject()).isEqualTo(TestConstants.CRITERIA_KEY);
        assertThat(c.getCriteriaType()).isEqualTo(CriteriaType.IS_EQUAL);
        assertThat(c.getCriteriaValues().size()).isEqualTo(1);
        assertThat(c.getCriteriaValues().get(0)).isEqualTo(TestConstants.CRITERIA_OBJECT);
        assertThat(c.getCriteriaList().size()).isEqualTo(0);
    }

  
    @Test
    public void testAndCriteria() {

        final NewCriteria c = NewCriteria.and(
                NewCriteria.value(TestConstants.CRITERIA_KEY,
                        CriteriaType.IS_EQUAL, Arrays.asList(new Object[] {TestConstants.CRITERIA_OBJECT})),
                NewCriteria.value(TestConstants.CRITERIA_KEY,
                        CriteriaType.IS_EQUAL, Arrays.asList(new Object[] {TestConstants.CRITERIA_OBJECT}))
                );
        
        assertThat(c.getCriteriaType()).isEqualTo(CriteriaType.AND_CONDITION);
        assertThat(c.getCriteriaValues().size()).isEqualTo(0);
        assertThat(c.getCriteriaList().size()).isEqualTo(2);
        assertThat(c.getCriteriaList().get(0).getClass()).isEqualTo(NewCriteria.class);
        assertThat(c.getCriteriaList().get(1).getClass()).isEqualTo(NewCriteria.class);
    }  

    @Test
    public void testOrCriteria() {

        final NewCriteria c = NewCriteria.or(
                NewCriteria.value(TestConstants.CRITERIA_KEY,
                        CriteriaType.IS_EQUAL, Arrays.asList(new Object[] {TestConstants.CRITERIA_OBJECT})),
                NewCriteria.value(TestConstants.CRITERIA_KEY,
                        CriteriaType.IS_EQUAL, Arrays.asList(new Object[] {TestConstants.CRITERIA_OBJECT}))
                );
        
        assertThat(c.getCriteriaType()).isEqualTo(CriteriaType.OR_CONDITION);
        assertThat(c.getCriteriaValues().size()).isEqualTo(0);
        assertThat(c.getCriteriaList().size()).isEqualTo(2);
        assertThat(c.getCriteriaList().get(0).getClass()).isEqualTo(NewCriteria.class);
        assertThat(c.getCriteriaList().get(1).getClass()).isEqualTo(NewCriteria.class);
    }  
}
