/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.query;

import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.exception.IllegalQueryException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.CRITERIA_KEY;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.CRITERIA_OBJECT;

public class CriteriaUnitTest {

    @Test
    public void testUnaryCriteria() {
        final List<Object> values = Arrays.asList(CRITERIA_OBJECT);
        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, CRITERIA_KEY, values);

        Assert.assertTrue(criteria.getSubCriteria().isEmpty());
        Assert.assertEquals(criteria.getSubjectValues(), values);
        Assert.assertEquals(criteria.getType(), CriteriaType.IS_EQUAL);
        Assert.assertEquals(criteria.getSubject(), CRITERIA_KEY);
        Assert.assertTrue(CriteriaType.isBinary(criteria.getType()));
    }

    @Test
    public void testBinaryCriteria() {
        final List<Object> values = Arrays.asList(CRITERIA_OBJECT);
        final Criteria leftCriteria = Criteria.getInstance(CriteriaType.IS_EQUAL, CRITERIA_KEY, values);
        final Criteria rightCriteria = Criteria.getInstance(CriteriaType.IS_EQUAL, CRITERIA_OBJECT, values);
        final Criteria criteria = Criteria.getInstance(CriteriaType.AND, leftCriteria, rightCriteria);

        Assert.assertNotNull(criteria.getSubCriteria());
        Assert.assertNull(criteria.getSubjectValues());
        Assert.assertNull(criteria.getSubject());
        Assert.assertEquals(criteria.getType(), CriteriaType.AND);
        Assert.assertTrue(CriteriaType.isClosed(criteria.getType()));

        Assert.assertEquals(criteria.getSubCriteria().size(), 2);
        Assert.assertEquals(criteria.getSubCriteria().get(0), leftCriteria);
        Assert.assertEquals(criteria.getSubCriteria().get(1), rightCriteria);
    }

    @Test(expected = IllegalQueryException.class)
    public void testInvalidInKeywordParameter() {
        final List<Object> values = Collections.singletonList(CRITERIA_OBJECT);
        final Criteria criteria = Criteria.getInstance(CriteriaType.IN, CRITERIA_KEY, values);
        final DocumentQuery query = new DocumentQuery(criteria);

        new FindQuerySpecGenerator().generate(query);
    }

    @Test(expected = IllegalQueryException.class)
    public void testInvalidInKeywordType() {
        final List<Object> values = Collections.singletonList(new IllegalQueryException(""));
        final Criteria criteria = Criteria.getInstance(CriteriaType.IN, CRITERIA_KEY, values);
        final DocumentQuery query = new DocumentQuery(criteria);

        new FindQuerySpecGenerator().generate(query);
    }
}
