/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.core;

import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.TestUtils;
import com.microsoft.azure.spring.data.documentdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
import com.microsoft.azure.spring.data.documentdb.exception.DocumentDBAccessException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.annotation.Persistent;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class DocumentDbTemplateIT {
    private static final Person TEST_PERSON = new Person(TestConstants.ID, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    @Value("${documentdb.uri}")
    private String documentDbUri;
    @Value("${documentdb.key}")
    private String documentDbKey;

    private DocumentClient documentClient;
    private DocumentDbTemplate dbTemplate;

    private MappingDocumentDbConverter dbConverter;
    private DocumentDbMappingContext mappingContext;
    private DocumentCollection collectionPerson;
    private DocumentDbEntityInformation<Person, String> personInfo;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        mappingContext = new DocumentDbMappingContext();
        personInfo = new DocumentDbEntityInformation<>(Person.class);

        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        dbConverter = new MappingDocumentDbConverter(mappingContext);
        documentClient = new DocumentClient(documentDbUri, documentDbKey,
                ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);

        dbTemplate = new DocumentDbTemplate(documentClient, dbConverter, TestConstants.DB_NAME);

        final IndexingPolicy policy = personInfo.getIndexingPolicy();

        collectionPerson = dbTemplate.createCollectionIfNotExists(Person.class.getSimpleName(), null, null, policy);
        dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON, null);
    }

    @After
    public void cleanup() {
        dbTemplate.deleteAll(Person.class.getSimpleName());
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testInsertDuplicateId() {
        dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON, null);
    }

    @Test
    public void testFindAll() {
        final List<Person> result = dbTemplate.findAll(Person.class.getSimpleName(), Person.class);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(TEST_PERSON);
    }

    @Test
    public void testFindById() {
        final Person result = dbTemplate.findById(Person.class.getSimpleName(),
                TEST_PERSON.getId(), Person.class);
        assertTrue(result.equals(TEST_PERSON));

        final Person nullResult = dbTemplate.findById(Person.class.getSimpleName(),
                TestConstants.NOT_EXIST_ID, Person.class);
        assertThat(nullResult).isNull();
    }

    @Test
    public void testUpsertNewDocument() {
        // Delete first as was inserted in setup
        dbTemplate.deleteById(Person.class.getSimpleName(), TEST_PERSON.getId(), Person.class, null);

        final String firstName = TestConstants.NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final Person newPerson = new Person(null, firstName, TestConstants.NEW_FIRST_NAME, null, null);

        dbTemplate.upsert(Person.class.getSimpleName(), newPerson, null, null);

        final List<Person> result = dbTemplate.findAll(Person.class);

        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).getFirstName().equals(firstName));
    }

    @Test
    public void testUpdate() {
        final Person updated = new Person(TEST_PERSON.getId(), TestConstants.UPDATED_FIRST_NAME,
                TEST_PERSON.getLastName(), TEST_PERSON.getHobbies(), TEST_PERSON.getShippingAddresses());
        dbTemplate.upsert(Person.class.getSimpleName(), updated, updated.getId(), null);

        final Person result = dbTemplate.findById(Person.class.getSimpleName(),
                updated.getId(), Person.class);

        assertTrue(result.equals(updated));
    }

    @Test
    public void testDeleteById() {
        final Person person2 = new Person(TestConstants.NEW_ID, TestConstants.NEW_FIRST_NAME,
                TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);
        dbTemplate.insert(person2, null);
        assertThat(dbTemplate.findAll(Person.class).size()).isEqualTo(2);

        dbTemplate.deleteById(Person.class.getSimpleName(), TEST_PERSON.getId(), null, null);

        final List<Person> result = dbTemplate.findAll(Person.class);
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).equals(person2));
    }


    @Test
    public void testDocumentDBAnnotation() {
        final IndexingPolicy policy = collectionPerson.getIndexingPolicy();

        Assert.isTrue(policy.getAutomatic() == TestConstants.DEFAULT_INDEXINGPOLICY_AUTOMATIC,
                "class Person collection policy should be default automatic");
        Assert.isTrue(policy.getIndexingMode() == TestConstants.DEFAULT_INDEXINGPOLICY_MODE,
                "class Person collection policy should be default indexing mode");

        TestUtils.testIndexingPolicyPathsEquals(policy.getIncludedPaths(), TestConstants.DEFAULT_INCLUDEDPATHS);
        TestUtils.testIndexingPolicyPathsEquals(policy.getExcludedPaths(), TestConstants.DEFAULT_EXCLUDEDPATHS);
    }
}
