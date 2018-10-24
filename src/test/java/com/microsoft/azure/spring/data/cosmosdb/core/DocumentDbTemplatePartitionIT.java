/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.PartitionPerson;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
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
    private static final PartitionPerson TEST_PERSON_0 = new PartitionPerson(ID_1, FIRST_NAME, LAST_NAME,
            HOBBIES, ADDRESSES);

    private static final PartitionPerson TEST_PERSON_1 = new PartitionPerson(ID_2, NEW_FIRST_NAME,
            TEST_PERSON_0.getLastName(), HOBBIES, ADDRESSES);

    @Value("${cosmosdb.uri}")
    private String documentDbUri;
    @Value("${cosmosdb.key}")
    private String documentDbKey;

    private DocumentDbTemplate dbTemplate;
    private String collectionName;
    private DocumentDbEntityInformation<PartitionPerson, String> personInfo;
    private String partitionKeyName;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(documentDbUri, documentDbKey, DB_NAME).build();
        final DocumentDbFactory dbFactory = new DocumentDbFactory(dbConfig);
        final ObjectMapper objectMapper = new ObjectMapper();
        final DocumentDbMappingContext mappingContext = new DocumentDbMappingContext();

        personInfo = new DocumentDbEntityInformation<>(PartitionPerson.class);
        partitionKeyName = personInfo.getPartitionKeyFieldName();
        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        final MappingDocumentDbConverter dbConverter = new MappingDocumentDbConverter(mappingContext, objectMapper);

        dbTemplate = new DocumentDbTemplate(dbFactory, dbConverter, DB_NAME);
        collectionName = personInfo.getCollectionName();

        dbTemplate.createCollectionIfNotExists(personInfo);
        dbTemplate.insert(PartitionPerson.class.getSimpleName(), TEST_PERSON_0,
                new PartitionKey(TEST_PERSON_0.getLastName()));
    }

    @After
    public void cleanup() {
        dbTemplate.deleteAll(personInfo.getCollectionName(), personInfo.getPartitionKeyNames());
    }

    @Test
    public void testFindWithPartition() {
        Criteria criteria = Criteria.getInstance(IS_EQUAL, PROPERTY_LAST_NAME, Arrays.asList(LAST_NAME));
        DocumentQuery query = new DocumentQuery(criteria);
        List<PartitionPerson> result = dbTemplate.find(query, PartitionPerson.class,
                PartitionPerson.class.getSimpleName());

        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0), TEST_PERSON_0);

        criteria = Criteria.getInstance(IS_EQUAL, PROPERTY_ID, Arrays.asList(ID_1));
        query = new DocumentQuery(criteria);
        result = dbTemplate.find(query, PartitionPerson.class, PartitionPerson.class.getSimpleName());

        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0), TEST_PERSON_0);
    }

    @Test
    public void testFindByNonExistIdWithPartition() {
        final Criteria criteria = Criteria.getInstance(IS_EQUAL, PROPERTY_ID, Arrays.asList(NOT_EXIST_ID));
        final DocumentQuery query = new DocumentQuery(criteria);

        final List<PartitionPerson> result = dbTemplate.find(query, PartitionPerson.class,
                PartitionPerson.class.getSimpleName());
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testUpsertNewDocumentPartition() {
        final String firstName = NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final PartitionPerson newPerson = new PartitionPerson(null, firstName, NEW_LAST_NAME, null, null);

        final String partitionKeyValue = newPerson.getLastName();
        dbTemplate.upsert(personInfo.getCollectionName(), newPerson, new PartitionKey(partitionKeyValue));

        final List<PartitionPerson> result = dbTemplate.findAll(collectionName, PartitionPerson.class,
                partitionKeyName);

        assertThat(result.size()).isEqualTo(2);

        final PartitionPerson person = result.stream()
                .filter(p -> p.getLastName().equals(partitionKeyValue)).findFirst().get();
        assertThat(person.getFirstName()).isEqualTo(firstName);
    }

    @Test
    public void testUpdatePartition() {
        final PartitionPerson updated = new PartitionPerson(TEST_PERSON_0.getPersonId(), UPDATED_FIRST_NAME,
                TEST_PERSON_0.getLastName(), TEST_PERSON_0.getHobbies(), TEST_PERSON_0.getShippingAddresses());
        dbTemplate.upsert(personInfo.getCollectionName(), updated, new PartitionKey(updated.getLastName()));

        final List<PartitionPerson> result = dbTemplate.findAll(collectionName, PartitionPerson.class,
                partitionKeyName);
        final PartitionPerson person = result.stream().filter(
                p -> TEST_PERSON_0.getPersonId().equals(p.getPersonId())).findFirst().get();

        assertTrue(person.equals(updated));
    }

    @Test
    public void testDeleteByIdPartition() {
        // insert new document with same partition key
        dbTemplate.insert(personInfo.getCollectionName(), TEST_PERSON_1, new PartitionKey(TEST_PERSON_1.getLastName()));

        final List<PartitionPerson> inserted = dbTemplate.findAll(collectionName, PartitionPerson.class,
                partitionKeyName);
        assertThat(inserted.size()).isEqualTo(2);
        assertThat(inserted.get(0).getLastName()).isEqualTo(TEST_PERSON_0.getLastName());
        assertThat(inserted.get(1).getLastName()).isEqualTo(TEST_PERSON_0.getLastName());

        dbTemplate.deleteById(PartitionPerson.class.getSimpleName(), TEST_PERSON_0.getPersonId(),
                new PartitionKey(TEST_PERSON_0.getLastName()));

        final List<PartitionPerson> result = dbTemplate.findAll(collectionName, PartitionPerson.class,
                partitionKeyName);
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).equals(TEST_PERSON_1));
    }

    @Test
    public void testCountForPartitionedCollection() {
        final long prevCount = dbTemplate.count(collectionName);
        assertThat(prevCount).isEqualTo(1);

        dbTemplate.insert(personInfo.getCollectionName(), TEST_PERSON_1, new PartitionKey(TEST_PERSON_1.getLastName()));

        final long newCount = dbTemplate.count(collectionName);
        assertThat(newCount).isEqualTo(2);
    }

    @Test
    public void testCountForPartitionedCollectionByQuery() {
        dbTemplate.insert(personInfo.getCollectionName(), TEST_PERSON_1, new PartitionKey(TEST_PERSON_1.getLastName()));

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TEST_PERSON_1.getFirstName()));
        final DocumentQuery query = new DocumentQuery(criteria);

        final long count = dbTemplate.count(query, PartitionPerson.class, collectionName);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testNonExistFieldValue() {
        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList("non-exist-first-name"));
        final DocumentQuery query = new DocumentQuery(criteria);

        final long count = dbTemplate.count(query, PartitionPerson.class, collectionName);
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testPartitionedFindAllPageableMultiPages() {
        dbTemplate.insert(personInfo.getCollectionName(), TEST_PERSON_1, new PartitionKey(TEST_PERSON_1.getLastName()));

        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_1, null);
        final Page<PartitionPerson> page1 = dbTemplate.findAll(pageRequest, PartitionPerson.class, collectionName);

        assertThat(page1.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateNonLastPage(page1, PAGE_SIZE_1);

        final Page<PartitionPerson> page2 = dbTemplate.findAll(page1.getPageable(),
                PartitionPerson.class, collectionName);
        assertThat(page2.getContent().size()).isEqualTo(1);
        validateLastPage(page2, PAGE_SIZE_1);
    }

    @Test
    public void testPartitionedPaginationQuery() {
        dbTemplate.insert(personInfo.getCollectionName(), TEST_PERSON_1, new PartitionKey(TEST_PERSON_1.getLastName()));

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(FIRST_NAME));
        final PageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_2, null);
        final DocumentQuery query = new DocumentQuery(criteria).with(pageRequest);

        final Page<PartitionPerson> page = dbTemplate.paginationQuery(query, PartitionPerson.class, collectionName);
        assertThat(page.getContent().size()).isEqualTo(1);
        validateLastPage(page, PAGE_SIZE_2);
    }

    @Test
    public void testInsertAsync() {
        dbTemplate.deleteAll(personInfo.getCollectionName(), personInfo.getPartitionKeyNames());

        this.dbTemplate.insertAsync(personInfo.getCollectionName(), TEST_PERSON_0, null)
                .subscribe(p -> assertThat(p).isEqualTo(TEST_PERSON_0));
        this.dbTemplate.insertAsync(personInfo.getCollectionName(), TEST_PERSON_1, null)
                .subscribe(p -> assertThat(p).isEqualTo(TEST_PERSON_1));
    }

    @Test
    public void testInsertAsyncException() {
        final PartitionKey key0 = new PartitionKey(TEST_PERSON_0.getLastName());
        final PartitionKey key1 = new PartitionKey(TEST_PERSON_1.getLastName());

        dbTemplate.deleteAll(personInfo.getCollectionName(), personInfo.getPartitionKeyNames());

        this.dbTemplate.insertAsync(personInfo.getCollectionName(), TEST_PERSON_0, key0)
                .subscribe(p -> assertThat(p).isEqualTo(TEST_PERSON_0));
        this.dbTemplate.insertAsync(personInfo.getCollectionName(), TEST_PERSON_0, key1)
                .subscribe(
                        p -> assertThat(p).isEqualTo(TEST_PERSON_0),
                        e -> assertThat(e).isInstanceOf(DocumentDBAccessException.class)
                );
    }
}
