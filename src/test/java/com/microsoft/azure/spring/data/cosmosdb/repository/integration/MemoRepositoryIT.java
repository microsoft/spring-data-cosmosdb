/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.domain.Importance;
import com.microsoft.azure.spring.data.cosmosdb.domain.Memo;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.MemoRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class MemoRepositoryIT {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(TestConstants.DATE_FORMAT);

    private static Date date1;
    private static Date date2;
    private static Memo testMemo1;
    private static Memo testMemo2;

    @Autowired
    MemoRepository repository;

    @BeforeClass
    public static void init() throws ParseException {
        date1 = DATE_FORMAT.parse(TestConstants.DATE_STRING);
        date2 = DATE_FORMAT.parse(TestConstants.NEW_DATE_STRING);
        testMemo1 = new Memo(TestConstants.ID, TestConstants.MESSAGE, date1, Importance.HIGH);
        testMemo2 = new Memo(TestConstants.NEW_ID, TestConstants.NEW_MESSAGE, date2, Importance.LOW);
    }

    @Before
    public void setup() {
        repository.save(testMemo1);
        repository.save(testMemo2);
    }

    @After
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll() {
        final List<Memo> result = TestUtils.toList(repository.findAll());

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void testFindByDate() {
        final List<Memo> result = repository.findMemoByDate(date1);

        assertThat(result.size()).isEqualTo(1);
        assertMemoEquals(result.get(0), testMemo1);
    }

    @Test
    public void testFindByEnum() {
        final List<Memo> result = repository.findMemoByImportance(testMemo1.getImportance());

        assertThat(result.size()).isEqualTo(1);
        assertMemoEquals(result.get(0), testMemo1);
    }

    private void assertMemoEquals(Memo actual, Memo expected) {
        assertThat(actual.getId().equals(expected.getId()));
        assertThat(actual.getMessage().equals(expected.getMessage()));
        assertThat(actual.getDate().equals(expected.getDate()));
        assertThat(actual.getImportance().equals(expected.getImportance()));
    }
}
