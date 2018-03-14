/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.core;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.documentdb.core.query.Criteria;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import com.microsoft.azure.spring.data.documentdb.domain.Address;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class DocumentDbTemplatePartitionIT {
    private static final String TEST_NOTEXIST_ID = "non_exist_id";

    private static final String TEST_DB_NAME = "template_it_db";
    private static final List<String> HOBBIES = TestConstants.HOBBIES;
    private static final List<Address> ADDRESSES = TestConstants.ADDRESSES;
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

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        mappingContext = new DocumentDbMappingContext();
        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        dbConverter = new MappingDocumentDbConverter(mappingContext);
        documentClient = new DocumentClient(documentDbUri, documentDbKey,
                ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);

        dbTemplate = new DocumentDbTemplate(documentClient, dbConverter, TestConstants.DB_NAME);

        dbTemplate.createCollectionIfNotExists(Person.class.getSimpleName(), TestConstants.PROPERTY_LAST_NAME,
                1000, null);
        dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON, new PartitionKey(TEST_PERSON.getLastName()));
    }

    @After
    public void cleanup() {
        dbTemplate.deleteAll(Person.class.getSimpleName());
    }

    @Test
    public void testFindAllByPartition() {
        final Criteria criteria = new Criteria(TestConstants.PROPERTY_LAST_NAME);
        criteria.is(TEST_PERSON.getLastName());
        final Query query = new Query(criteria);

        final List<Person> result = dbTemplate.find(query, Person.class, Person.class.getSimpleName());
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).equals(TEST_PERSON));
    }

    @Test
    public void testFindByIdWithPartition() {
        final Criteria criteria = new Criteria(TestConstants.PROPERTY_ID);
        criteria.is(TEST_PERSON.getId());
        criteria.and(TestConstants.PROPERTY_LAST_NAME).is(TEST_PERSON.getLastName());
        final Query query = new Query(criteria);

        final List<Person> result = dbTemplate.find(query, Person.class, Person.class.getSimpleName());
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).equals(TEST_PERSON));
    }

    @Test
    public void testFindByNonExistIdWithPartition() {
        final Criteria criteria = new Criteria(TestConstants.PROPERTY_ID);
        criteria.is(TestConstants.NOT_EXIST_ID);
        criteria.and(TestConstants.PROPERTY_LAST_NAME).is(TEST_PERSON.getLastName());
        final Query query = new Query(criteria);

        final List<Person> result = dbTemplate.find(query, Person.class, Person.class.getSimpleName());
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testUpsertNewDocumentPartition() {
        final String firstName = TestConstants.NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final Person newPerson = new Person(null, firstName, TestConstants.NEW_LAST_NAME, null, null);

        final String partitionKeyValue = newPerson.getLastName();
        dbTemplate.upsert(Person.class.getSimpleName(), newPerson, null, new PartitionKey(partitionKeyValue));

        final List<Person> result = dbTemplate.findAll(Person.class);

        assertThat(result.size()).isEqualTo(2);

        final Person person = result.stream()
                .filter(p -> p.getLastName().equals(partitionKeyValue)).findFirst().get();
        assertThat(person.getFirstName()).isEqualTo(firstName);
    }

    @Test
    public void testUpdatePartition() {
        final Person updated = new Person(TEST_PERSON.getId(), TestConstants.UPDATED_FIRST_NAME,
                TEST_PERSON.getLastName(), TEST_PERSON.getHobbies(), TEST_PERSON.getShippingAddresses());
        dbTemplate.upsert(Person.class.getSimpleName(), updated, updated.getId(),
                new PartitionKey(updated.getLastName()));

        final List<Person> result = dbTemplate.findAll(Person.class);
        final Person person = result.stream().filter(p -> TEST_PERSON.getId().equals(p.getId())).findFirst().get();

        assertTrue(person.equals(updated));
    }

    @Test
    public void testDeleteByIdPartition() {
        // insert new document with same partition key
        final Person person2 = new Person(TestConstants.NEW_ID, TestConstants.NEW_FIRST_NAME,
                TEST_PERSON.getLastName(), TestConstants.HOBBIES, TestConstants.ADDRESSES);
        dbTemplate.insert(Person.class.getSimpleName(), person2, new PartitionKey(person2.getLastName()));

        final List<Person> inserted = dbTemplate.findAll(Person.class);
        assertThat(inserted.size()).isEqualTo(2);
        assertThat(inserted.get(0).getLastName()).isEqualTo(TEST_PERSON.getLastName());
        assertThat(inserted.get(1).getLastName()).isEqualTo(TEST_PERSON.getLastName());

        dbTemplate.deleteById(Person.class.getSimpleName(),
                TEST_PERSON.getId(), Person.class, new PartitionKey(TEST_PERSON.getLastName()));

        final List<Person> result = dbTemplate.findAll(Person.class);
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).equals(person2));
    }
}
