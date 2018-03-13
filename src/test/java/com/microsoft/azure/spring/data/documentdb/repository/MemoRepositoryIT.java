/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository;

import com.microsoft.azure.spring.data.documentdb.TestUtils;
import com.microsoft.azure.spring.data.documentdb.domain.Memo;
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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private static final String ID_1 = "id_1";
    private static final String MESSAGE_1 = "first message";
    private static final String DATE_STR_1 = "1/1/2000";

    private static final String ID_2 = "id_2";
    private static final String MESSAGE_2 = "second message";
    private static final String DATE_STR_2 = "1/1/2001";

    private static Date date1;
    private static Date date2;
    private static Memo testMemo1;
    private static Memo testMemo2;

    @Autowired
    MemoRepository repository;

    @BeforeClass
    public static void init() throws ParseException {
        date1 = DATE_FORMAT.parse(DATE_STR_1);
        date2 = DATE_FORMAT.parse(DATE_STR_2);
        testMemo1 = new Memo(ID_1, MESSAGE_1, date1);
        testMemo2 = new Memo(ID_2, MESSAGE_2, date2);
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
    public void testFindByDate() throws ParseException {
        final List<Memo> result = repository.findMemoByDate(DATE_FORMAT.parse(DATE_STR_1));

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(ID_1);
        assertThat(result.get(0).getMessage()).isEqualTo(MESSAGE_1);
        assertThat(result.get(0).getDate()).isEqualTo(date1);
    }
}
