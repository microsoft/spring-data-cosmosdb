/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.domain.Project;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.PersonRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class PersonRepositoryIT {
    public static final String ID_0 = "id-0";
    public static final String ID_1 = "id-1";
    public static final String ID_2 = "id-2";
    public static final String ID_3 = "id-3";

    public static final String FIRST_NAME_0 = "Mary";
    public static final String FIRST_NAME_1 = "Cheng";
    public static final String FIRST_NAME_2 = "Zheng";
    public static final String FIRST_NAME_3 = "Zhen";

    public static final String LAST_NAME_0 = "Chen";
    public static final String LAST_NAME_1 = "Ch";
    public static final String LAST_NAME_2 = "N";
    public static final String LAST_NAME_3 = "H";

    public static final String SUB_FIRST_NAME = "eng";

    private static final Person PERSON_0 = new Person(ID_0, FIRST_NAME_0, LAST_NAME_0, null, null);
    private static final Person PERSON_1 = new Person(ID_1, FIRST_NAME_1, LAST_NAME_1, null, null);
    private static final Person PERSON_2 = new Person(ID_2, FIRST_NAME_2, LAST_NAME_2, null, null);
    private static final Person PERSON_3 = new Person(ID_3, FIRST_NAME_3, LAST_NAME_3, null, null);
    private static final List<Person> PEOPLE = Arrays.asList(PERSON_0, PERSON_1, PERSON_2, PERSON_3);

    @Autowired
    private PersonRepository repository;

    @Before
    public void setup() {
        this.repository.saveAll(PEOPLE);
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @Test
    public void testFindByContaining() {
        final List<Person> people = repository.findByFirstNameContaining(SUB_FIRST_NAME);
        final List<Person> reference = Arrays.asList(PERSON_1, PERSON_2);

        assertPeopleEquals(people, reference);
    }

    @Test
    public void testFindByContainingWithAnd() {
        final List<Person> people = repository.findByFirstNameContainingAndLastNameContaining("eng", "h");
        final List<Person> reference = Arrays.asList(PERSON_1);

        assertPeopleEquals(people, reference);
    }

    @Test
    public void testFindByEndsWith() {
        final List<Person> people = repository.findByFirstNameEndsWith("en");
        final List<Person> reference = Arrays.asList(PERSON_3);

        assertPeopleEquals(people, reference);
    }

    @Test
    public void testFindByNot() {
        final List<Person> people = repository.findByFirstNameNot("Mary");
        final List<Person> reference = Arrays.asList(PERSON_1, PERSON_2, PERSON_3);

        assertPeopleEquals(people, reference);
    }

    @Test
    public void testFindByStartsWith() {
        List<Person> people = repository.findByFirstNameStartsWith("Z");

        assertPeopleEquals(people, Arrays.asList(PERSON_2, PERSON_3));

        people = repository.findByLastNameStartsWith("C");

        assertPeopleEquals(people, Arrays.asList(PERSON_0, PERSON_1));
    }

    @Test
    public void testFindByStartsWithAndEndsWith() {
        List<Person> people = repository.findByFirstNameStartsWithAndLastNameEndingWith("Z", "H");

        assertPeopleEquals(people, Arrays.asList(PERSON_3));

        people = repository.findByFirstNameStartsWithAndLastNameEndingWith("Z", "en");

        assertPeopleEquals(people, Arrays.asList());
    }

    @Test
    public void testFindByStartsWithOrContaining() {
        List<Person> people = repository.findByFirstNameStartsWithOrLastNameContaining("Zhen", "C");

        assertPeopleEquals(people, PEOPLE);

        people = repository.findByFirstNameStartsWithOrLastNameContaining("M", "N");

        assertPeopleEquals(people, Arrays.asList(PERSON_0, PERSON_2));
    }

    @Test
    public void testFindByContainingAndNot() {
        final List<Person> people = repository.findByFirstNameContainingAndLastNameNot("Zhe", "N");

        assertPeopleEquals(people, Arrays.asList(PERSON_3));
    }

    private void assertPeopleEquals(List<Person> people, List<Person> reference) {
        people.sort(Comparator.comparing(Person::getId));
        reference.sort(Comparator.comparing(Person::getId));

        Assert.assertEquals(people, reference);
    }
}
