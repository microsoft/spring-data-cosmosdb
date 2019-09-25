/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.CosmosContainerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.CosmosMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CosmosPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import org.assertj.core.util.Lists;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateNonLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class CosmosTemplateIT {
    private static final Person TEST_PERSON = new Person(TestConstants.ID_1, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_2 = new Person(TestConstants.ID_2, TestConstants.NEW_FIRST_NAME,
            TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_3 = new Person(TestConstants.ID_3, TestConstants.NEW_FIRST_NAME,
            TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    @Value("${cosmosdb.uri}")
    private String documentDbUri;
    @Value("${cosmosdb.key}")
    private String documentDbKey;

    private static CosmosTemplate dbTemplate;
    private static MappingCosmosConverter dbConverter;
    private static CosmosMappingContext mappingContext;
    private static ObjectMapper objectMapper;
    private static CosmosContainerProperties collectionPerson;
    private static CosmosEntityInformation<Person, String> personInfo;
    private static String collectionName;
    private static boolean initialized;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
//        final CosmosDBConfig dbConfig = CosmosDBConfig.builder(documentDbUri, documentDbKey, DB_NAME).build();
//        final CosmosDbFactory cosmosDbFactory = new CosmosDbFactory(dbConfig);

//        mappingContext = new CosmosMappingContext();
//        objectMapper = new ObjectMapper();
//        personInfo = new CosmosEntityInformation<>(Person.class);
//        collectionName = personInfo.getCollectionName();
//
//        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));
//        
//        dbConverter = new MappingCosmosConverter(mappingContext, objectMapper);
//        dbTemplate = new CosmosTemplate(cosmosDbFactory, dbConverter, DB_NAME);
//
//        collectionPerson = dbTemplate.createCollectionIfNotExists(this.personInfo);
//        dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON, null);

        if (!initialized) {
            final CosmosDBConfig dbConfig = CosmosDBConfig.builder(documentDbUri, documentDbKey, DB_NAME).build();
            final CosmosDbFactory cosmosDbFactory = new CosmosDbFactory(dbConfig);
            mappingContext = new CosmosMappingContext();
            personInfo = new CosmosEntityInformation<>(Person.class);
            collectionName = personInfo.getCollectionName();
            mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));
            dbConverter = new MappingCosmosConverter(mappingContext, null);
            dbTemplate = new CosmosTemplate(cosmosDbFactory, dbConverter, DB_NAME);
            initialized = true;
        }
        dbTemplate.createCollectionIfNotExists(personInfo);
        dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON, null);
    }

    @After
    public void cleanup() {
        dbTemplate.deleteCollection(Person.class.getSimpleName());
    }

    @Test(expected = CosmosDBAccessException.class)
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
        assertEquals(result, TEST_PERSON);

        final Person nullResult = dbTemplate.findById(Person.class.getSimpleName(),
                TestConstants.NOT_EXIST_ID, Person.class);
        assertThat(nullResult).isNull();
    }

    @Test
    public void testFindByMultiIds() {
        dbTemplate.insert(TEST_PERSON_2, null);
        dbTemplate.insert(TEST_PERSON_3, null);

        final List<Object> ids = Lists.newArrayList(ID_1, ID_2, ID_3);
        final List<Person> result = dbTemplate.findByIds(ids, Person.class, collectionName);

        final List<Person> expected = Lists.newArrayList(TEST_PERSON, TEST_PERSON_2, TEST_PERSON_3);
        assertThat(result.size()).isEqualTo(expected.size());
        assertThat(result).containsAll(expected);
    }

    @Test
    public void testUpsertNewDocument() {
        // Delete first as was inserted in setup
        dbTemplate.deleteById(Person.class.getSimpleName(), TEST_PERSON.getId(), null);

        final String firstName = TestConstants.NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final Person newPerson = new Person(TEST_PERSON.getId(), firstName, TestConstants.NEW_FIRST_NAME, null, null);

        dbTemplate.upsert(Person.class.getSimpleName(), newPerson, null);

        final List<Person> result = dbTemplate.findAll(Person.class);

        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0).getFirstName(), firstName);
    }

    @Test
    public void testUpdate() {
        final Person updated = new Person(TEST_PERSON.getId(), TestConstants.UPDATED_FIRST_NAME,
                TEST_PERSON.getLastName(), TEST_PERSON.getHobbies(), TEST_PERSON.getShippingAddresses());
        dbTemplate.upsert(Person.class.getSimpleName(), updated, null);

        final Person result = dbTemplate.findById(Person.class.getSimpleName(),
                updated.getId(), Person.class);

        assertEquals(result, updated);
    }

    @Test
    public void testDeleteById() {
        dbTemplate.insert(TEST_PERSON_2, null);
        assertThat(dbTemplate.findAll(Person.class).size()).isEqualTo(2);

        dbTemplate.deleteById(Person.class.getSimpleName(), TEST_PERSON.getId(), null);

        final List<Person> result = dbTemplate.findAll(Person.class);
        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0), TEST_PERSON_2);
    }

    @Test
    public void testCountByCollection() {
        final long prevCount = dbTemplate.count(collectionName);
        assertThat(prevCount).isEqualTo(1);

        dbTemplate.insert(TEST_PERSON_2, null);

        final long newCount = dbTemplate.count(collectionName);
        assertThat(newCount).isEqualTo(2);
    }

    @Test
    public void testCountByQuery() {
        dbTemplate.insert(TEST_PERSON_2, null);

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
            Collections.singletonList(TEST_PERSON_2.getFirstName()));
        final DocumentQuery query = new DocumentQuery(criteria);

        final long count = dbTemplate.count(query, Person.class, collectionName);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testFindAllPageableMultiPages() {
        dbTemplate.insert(TEST_PERSON_2, null);

        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_1, null);
        final Page<Person> page1 = dbTemplate.findAll(pageRequest, Person.class, collectionName);

        assertThat(page1.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateNonLastPage(page1, PAGE_SIZE_1);

        final Page<Person> page2 = dbTemplate.findAll(page1.getPageable(), Person.class, collectionName);
        assertThat(page2.getContent().size()).isEqualTo(1);
        validateLastPage(page2, PAGE_SIZE_1);
    }

    @Test
    public void testPaginationQuery() {
        dbTemplate.insert(TEST_PERSON_2, null);

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
            Collections.singletonList(FIRST_NAME));
        final PageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_2, null);
        final DocumentQuery query = new DocumentQuery(criteria).with(pageRequest);

        final Page<Person> page = dbTemplate.paginationQuery(query, Person.class, collectionName);
        assertThat(page.getContent().size()).isEqualTo(1);
        validateLastPage(page, PAGE_SIZE_2);
    }

    @Test
    public void testFindAllWithPageableAndSort() {
        dbTemplate.insert(TEST_PERSON_2, null);
        dbTemplate.insert(TEST_PERSON_3, null);

        final Sort sort = new Sort(Sort.Direction.DESC, "firstName");
        final PageRequest pageRequest = CosmosPageRequest.of(0, PAGE_SIZE_3, null, sort);

        final Page<Person> page = dbTemplate.findAll(pageRequest, Person.class, collectionName);
        assertThat(page.getContent().size()).isEqualTo(3);
        validateLastPage(page, PAGE_SIZE_3);

        final List<Person> result = page.getContent();
        assertThat(result.get(0).getFirstName()).isEqualTo(NEW_FIRST_NAME);
        assertThat(result.get(1).getFirstName()).isEqualTo(NEW_FIRST_NAME);
        assertThat(result.get(2).getFirstName()).isEqualTo(FIRST_NAME);

    }

    @Test
    public void testFindAllWithTwoPagesAndVerifySortOrder() {
        final Person testPerson4 = new Person("id_4", "barney", NEW_LAST_NAME, HOBBIES, ADDRESSES);
        final Person testPerson5 = new Person("id_5", "fred", NEW_LAST_NAME, HOBBIES, ADDRESSES);

        dbTemplate.insert(TEST_PERSON_2, null);
        dbTemplate.insert(TEST_PERSON_3, null);
        dbTemplate.insert(testPerson4, null);
        dbTemplate.insert(testPerson5, null);

        final Sort sort = new Sort(Sort.Direction.ASC, "firstName");
        final PageRequest pageRequest = CosmosPageRequest.of(0, PAGE_SIZE_3, null, sort);

        final Page<Person> firstPage = dbTemplate.findAll(pageRequest, Person.class, collectionName);

        assertThat(firstPage.getContent().size()).isEqualTo(3);
        validateNonLastPage(firstPage, PAGE_SIZE_3);

        final List<Person> firstPageResults = firstPage.getContent();
        assertThat(firstPageResults.get(0).getFirstName()).isEqualTo(testPerson4.getFirstName());
        assertThat(firstPageResults.get(1).getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(firstPageResults.get(2).getFirstName()).isEqualTo(testPerson5.getFirstName());

        final Page<Person> secondPage = dbTemplate.findAll(firstPage.getPageable(), Person.class, collectionName);

        assertThat(secondPage.getContent().size()).isEqualTo(2);
        validateLastPage(secondPage, PAGE_SIZE_3);

        final List<Person> secondPageResults = secondPage.getContent();
        assertThat(secondPageResults.get(0).getFirstName()).isEqualTo(NEW_FIRST_NAME);
        assertThat(secondPageResults.get(1).getFirstName()).isEqualTo(NEW_FIRST_NAME);
    }
}
