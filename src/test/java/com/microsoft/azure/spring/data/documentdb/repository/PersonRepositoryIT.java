/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository;

import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.TestUtils;
import com.microsoft.azure.spring.data.documentdb.domain.Address;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class PersonRepositoryIT {

    private static final Person PERSON_1 =
            new Person(TestConstants.ID, TestConstants.FIRST_NAME, TestConstants.LAST_NAME,
                    TestConstants.AGE_10, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person PERSON_2 =
            new Person(TestConstants.NEW_ID, TestConstants.FIRST_NAME, TestConstants.LAST_NAME,
                    TestConstants.AGE_20, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    @Autowired
    PersonRepository repository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        repository.save(PERSON_1);
        repository.save(PERSON_2);
    }

    @After
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll() {
        final List<Person> result = TestUtils.toList(repository.findAll());

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void testFindAgeGreaterThan() {
        final List<Person> result = TestUtils.toList(repository.findByAgeGreaterThan(TestConstants.AGE_10));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getAge()).isEqualTo(TestConstants.AGE_20);

        final List<Person> noMatchResult = TestUtils.toList(repository.findByAgeGreaterThan(TestConstants.AGE_20));
        assertThat(noMatchResult.size()).isEqualTo(0);

        final List<Person> allMatchResult = TestUtils.toList(repository.findByAgeGreaterThan(TestConstants.AGE_0));
        assertThat(allMatchResult.size()).isEqualTo(2);
        assertThat(result.get(0).getAge()).isGreaterThan(TestConstants.AGE_0);
        assertThat(result.get(1).getAge()).isGreaterThan(TestConstants.AGE_0);
    }

}
