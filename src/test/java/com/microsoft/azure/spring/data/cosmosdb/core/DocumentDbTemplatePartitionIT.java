/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateNonLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.*;
import static com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType.IS_EQUAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class DocumentDbTemplatePartitionIT {
    private static final Person TEST_PERSON = new Person(TestConstants.ID_1, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_2 = new Person(TestConstants.ID_2, TestConstants.NEW_FIRST_NAME,
            TEST_PERSON.getLastName(), TestConstants.HOBBIES, TestConstants.ADDRESSES);

    @Value("${cosmosdb.uri}")
    private String documentDbUri;
    @Value("${cosmosdb.key}")
    private String documentDbKey;

    private DocumentClient documentClient;
    private DocumentDbTemplate dbTemplate;

    private MappingDocumentDbConverter dbConverter;
    private DocumentDbMappingContext mappingContext;
    private ObjectMapper objectMapper;
    private DocumentDbEntityInformation<Person, String> personInfo;
    private String collectionName;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        mappingContext = new DocumentDbMappingContext();
        objectMapper = new ObjectMapper();
        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        dbConverter = new MappingDocumentDbConverter(mappingContext, objectMapper);
        documentClient = new DocumentClient(documentDbUri, documentDbKey,
                ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);

        dbTemplate = new DocumentDbTemplate(documentClient, dbConverter, TestConstants.DB_NAME);
        personInfo = new DocumentDbEntityInformation<>(Person.class);
        collectionName = personInfo.getCollectionName();

        dbTemplate.createCollectionIfNotExists(this.personInfo, PROPERTY_LAST_NAME);
        dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON, new PartitionKey(TEST_PERSON.getLastName()));
    }

    @After
    public void cleanup() {
        dbTemplate.deleteCollection(Person.class.getSimpleName());
    }

    @Test
    public void testFindWithPartition() {
        Criteria criteria = Criteria.getUnaryInstance(IS_EQUAL, PROPERTY_LAST_NAME, Arrays.asList(LAST_NAME));
        DocumentQuery query = new DocumentQuery(criteria);
        List<Person> result = dbTemplate.find(query, Person.class, Person.class.getSimpleName());

        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0), TEST_PERSON);

        criteria = Criteria.getUnaryInstance(IS_EQUAL, PROPERTY_ID, Arrays.asList(ID_1));
        query = new DocumentQuery(criteria);
        result = dbTemplate.find(query, Person.class, Person.class.getSimpleName());

        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0), TEST_PERSON);
    }

    @Test
    public void testFindByNonExistIdWithPartition() {
        final Criteria criteria = Criteria.getUnaryInstance(IS_EQUAL, PROPERTY_ID, Arrays.asList(NOT_EXIST_ID));
        final DocumentQuery query = new DocumentQuery(criteria);

        final List<Person> result = dbTemplate.find(query, Person.class, Person.class.getSimpleName());
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testUpsertNewDocumentPartition() {
        final String firstName = TestConstants.NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final Person newPerson = new Person(null, firstName, TestConstants.NEW_LAST_NAME, null, null);

        final String partitionKeyValue = newPerson.getLastName();
        dbTemplate.upsert(Person.class.getSimpleName(), newPerson, new PartitionKey(partitionKeyValue));

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
        dbTemplate.upsert(Person.class.getSimpleName(), updated, new PartitionKey(updated.getLastName()));

        final List<Person> result = dbTemplate.findAll(Person.class);
        final Person person = result.stream().filter(p -> TEST_PERSON.getId().equals(p.getId())).findFirst().get();

        assertTrue(person.equals(updated));
    }

    @Test
    public void testDeleteByIdPartition() {
        // insert new document with same partition key
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName()));

        final List<Person> inserted = dbTemplate.findAll(Person.class);
        assertThat(inserted.size()).isEqualTo(2);
        assertThat(inserted.get(0).getLastName()).isEqualTo(TEST_PERSON.getLastName());
        assertThat(inserted.get(1).getLastName()).isEqualTo(TEST_PERSON.getLastName());

        dbTemplate.deleteById(Person.class.getSimpleName(),
                TEST_PERSON.getId(), new PartitionKey(TEST_PERSON.getLastName()));

        final List<Person> result = dbTemplate.findAll(Person.class);
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).equals(TEST_PERSON_2));
    }

    @Test
    public void testCountForPartitionedCollection() {
        final long prevCount = dbTemplate.count(collectionName);
        assertThat(prevCount).isEqualTo(1);

        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName()));

        final long newCount = dbTemplate.count(collectionName);
        assertThat(newCount).isEqualTo(2);
    }

    @Test
    public void testCountForPartitionedCollectionByQuery() {
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName()));

        final Criteria criteria = Criteria.getUnaryInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TEST_PERSON_2.getFirstName()));
        final DocumentQuery query = new DocumentQuery(criteria);

        final long count = dbTemplate.count(query, Person.class, collectionName);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testNonExistFieldValue() {
        final Criteria criteria = Criteria.getUnaryInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList("non-exist-first-name"));
        final DocumentQuery query = new DocumentQuery(criteria);

        final long count = dbTemplate.count(query, Person.class, collectionName);
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testPartitionedFindAllPageableMultiPages() {
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName()));

        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_1, null);
        final Page<Person> page1 = dbTemplate.findAll(pageRequest, Person.class, collectionName);

        assertThat(page1.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateNonLastPage(page1, PAGE_SIZE_1);

        final Page<Person> page2 = dbTemplate.findAll(page1.getPageable(), Person.class, collectionName);
        assertThat(page2.getContent().size()).isEqualTo(1);
        validateLastPage(page2, PAGE_SIZE_1);
    }

    @Test
    public void testPartitionedPaginationQuery() {
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName()));

        final Criteria criteria = Criteria.getUnaryInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TestConstants.FIRST_NAME));
        final PageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_2, null);
        final DocumentQuery query = new DocumentQuery(criteria).with(pageRequest);

        final Page<Person> page = dbTemplate.paginationQuery(query, Person.class, collectionName);
        assertThat(page.getContent().size()).isEqualTo(1);
        validateLastPage(page, PAGE_SIZE_2);
    }
}
