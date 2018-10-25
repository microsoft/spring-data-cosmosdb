/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.IndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateNonLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class DocumentDbTemplateIT {
    private static final Person TEST_PERSON_0 = new Person(TestConstants.ID_1, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_1 = new Person(TestConstants.ID_2, TestConstants.NEW_FIRST_NAME,
            TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    @Value("${cosmosdb.uri}")
    private String documentDbUri;
    @Value("${cosmosdb.key}")
    private String documentDbKey;

    private DocumentDbTemplate dbTemplate;
    private MappingDocumentDbConverter dbConverter;
    private DocumentDbMappingContext mappingContext;
    private ObjectMapper objectMapper;
    private DocumentCollection collectionPerson;
    private DocumentDbEntityInformation<Person, String> personInfo;
    private String collectionName;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(documentDbUri, documentDbKey, DB_NAME).build();
        final DocumentDbFactory dbFactory = new DocumentDbFactory(dbConfig);

        mappingContext = new DocumentDbMappingContext();
        objectMapper = new ObjectMapper();
        personInfo = new DocumentDbEntityInformation<>(Person.class);
        collectionName = personInfo.getCollectionName();

        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        dbConverter = new MappingDocumentDbConverter(mappingContext, objectMapper);
        dbTemplate = new DocumentDbTemplate(dbFactory, dbConverter, DB_NAME);

        collectionPerson = dbTemplate.createCollectionIfNotExists(this.personInfo);
        dbTemplate.insert(collectionName, TEST_PERSON_0, null);
    }

    @After
    public void cleanup() {
        dbTemplate.deleteCollection(collectionName);
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testInsertDuplicateId() {
        dbTemplate.insert(collectionName, TEST_PERSON_0, null);
    }

    @Test
    public void testFindAll() {
        dbTemplate.deleteAll(collectionName, Lists.newArrayList());
        final List<Person> insertedList = insertMultiPerson(5);
        final List<Person> result = dbTemplate.findAll(collectionName, Person.class, null);
        assertThat(result.size()).isEqualTo(insertedList.size());
        assertThat(result).containsAll(insertedList);
    }

    @Test
    public void findAllAsync() {
        dbTemplate.deleteAll(collectionName, Lists.newArrayList());
        final List<Person> insertedList = insertMultiPerson(5);
        dbTemplate.findAllAsync(collectionName, Person.class, null).toList()
                .subscribe(result -> {
                    assertThat(result.size()).isEqualTo(insertedList.size());
                    assertThat(result).containsAll(insertedList);
                });
    }

    private List<Person> insertMultiPerson(int count) {
        final List<Person> personList = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            final Person person = new Person(UUID.randomUUID().toString(), FIRST_NAME, LAST_NAME, HOBBIES, ADDRESSES);
            dbTemplate.insert(collectionName, person, null);
            personList.add(person);
        }

        return personList;
    }

    @Test
    public void testFindById() {
        Optional<Person> optional = dbTemplate.findById(collectionName, TEST_PERSON_0.getId(),
                Person.class, null);
        assertTrue(optional.isPresent());
        assertThat(optional.get()).isEqualTo(TEST_PERSON_0);

        optional = dbTemplate.findById(collectionName, TestConstants.NOT_EXIST_ID, Person.class, null);

        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    public void testUpsertNewDocument() {
        // Delete first as was inserted in setup
        dbTemplate.deleteById(collectionName, TEST_PERSON_0.getId(), null);

        final String firstName = TestConstants.NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final Person newPerson = new Person(null, firstName, TestConstants.NEW_FIRST_NAME, null, null);

        dbTemplate.upsert(collectionName, newPerson, null);

        final List<Person> result = dbTemplate.findAll(collectionName, Person.class, null);

        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).getFirstName().equals(firstName));
    }

    @Test
    public void testUpdate() {
        final Person updated = new Person(TEST_PERSON_0.getId(), TestConstants.UPDATED_FIRST_NAME,
                TEST_PERSON_0.getLastName(), TEST_PERSON_0.getHobbies(), TEST_PERSON_0.getShippingAddresses());
        dbTemplate.upsert(collectionName, updated, null);

        final Optional<Person> optional =
                dbTemplate.findById(collectionName, updated.getId(), Person.class, null);

        assertTrue(optional.isPresent());
        assertThat(optional.get()).isEqualTo(updated);
    }

    @Test
    public void testDeleteById() {
        dbTemplate.insert(collectionName, TEST_PERSON_1, null);
        assertThat(dbTemplate.findAll(collectionName, Person.class, null).size()).isEqualTo(2);

        dbTemplate.deleteById(collectionName, TEST_PERSON_0.getId(), null);

        final List<Person> result = dbTemplate.findAll(collectionName, Person.class, null);
        assertThat(result.size()).isEqualTo(1);
        assertEquals(result.get(0), TEST_PERSON_1);
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

    @Test
    public void testCountByCollection() {
        final long prevCount = dbTemplate.count(collectionName);
        assertThat(prevCount).isEqualTo(1);

        dbTemplate.insert(collectionName, TEST_PERSON_1, null);

        final long newCount = dbTemplate.count(collectionName);
        assertThat(newCount).isEqualTo(2);
    }

    @Test
    public void testCountAsyncByCollection() {
        dbTemplate.countAsync(collectionName).subscribe(prevCount -> assertThat(prevCount).isEqualTo(1));

        dbTemplate.insert(this.personInfo.getCollectionName(), TEST_PERSON_1, null);

        dbTemplate.countAsync(collectionName).subscribe(newCount -> assertThat(newCount).isEqualTo(2));
    }

    @Test
    public void testCountByQuery() {
        dbTemplate.insert(collectionName, TEST_PERSON_1, null);

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TEST_PERSON_1.getFirstName()));
        final DocumentQuery query = new DocumentQuery(criteria);

        final long count = dbTemplate.count(query, Person.class, collectionName);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testCountAsyncByQuery() {
        dbTemplate.insert(this.personInfo.getCollectionName(), TEST_PERSON_1, null);

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TEST_PERSON_1.getFirstName()));
        final DocumentQuery query = new DocumentQuery(criteria);

        dbTemplate.countAsync(query, collectionName, Person.class).subscribe(count -> assertThat(count).isEqualTo(1));
    }

    @Test
    public void testFindAllPageableMultiPages() {
        dbTemplate.insert(collectionName, TEST_PERSON_1, null);

        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_1, null);
        final Page<Person> page1 = dbTemplate.findAll(pageRequest, Person.class, collectionName);

        assertThat(page1.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateNonLastPage(page1, PAGE_SIZE_1);

        final Page<Person> page2 = dbTemplate.findAll(page1.getPageable(), Person.class, collectionName);
        assertThat(page2.getContent().size()).isEqualTo(1);
        validateLastPage(page2, PAGE_SIZE_1);
    }

    @Test
    public void testFindAllAsyncPageableMultiPages() {
        dbTemplate.insert(collectionName, TEST_PERSON_1, null);

        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_1, null);

        dbTemplate.findAllAsync(pageRequest, Person.class, collectionName).subscribe(page1 -> {
                    assertThat(page1.getContent().size()).isEqualTo(PAGE_SIZE_1);
                    validateNonLastPage(page1, PAGE_SIZE_1);

                    dbTemplate.findAllAsync(page1.getPageable(), Person.class, collectionName)
                            .subscribe(page2 -> {
                                assertThat(page2.getContent().size()).isEqualTo(1);
                                validateLastPage(page2, PAGE_SIZE_1);
                            });
                });
    }

    @Test
    public void testPaginationQuery() {
        dbTemplate.insert(collectionName, TEST_PERSON_1, null);

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TestConstants.FIRST_NAME));
        final PageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_2, null);
        final DocumentQuery query = new DocumentQuery(criteria).with(pageRequest);

        final Page<Person> page = dbTemplate.paginationQuery(query, Person.class, collectionName);
        assertThat(page.getContent().size()).isEqualTo(1);
        validateLastPage(page, PAGE_SIZE_2);
    }

    @Test
    public void testPaginationAsync() {
        dbTemplate.insert(collectionName, TEST_PERSON_1, null);

        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TestConstants.FIRST_NAME));
        final PageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_2, null);
        final DocumentQuery query = new DocumentQuery(criteria).with(pageRequest);

        dbTemplate.paginationQueryAsync(query, Person.class, collectionName)
                .subscribe(page -> {
                    assertThat(page.getContent().size()).isEqualTo(1);
                    validateLastPage(page, PAGE_SIZE_2);
                });
    }

    @Test
    public void testInsertAsync() {
        this.dbTemplate.deleteAll(personInfo.getCollectionName(), personInfo.getPartitionKeyNames());
        Person inserted = this.dbTemplate.insertAsync(this.personInfo.getCollectionName(), TEST_PERSON_0, null)
                .toBlocking().single();

        assertThat(inserted).isEqualTo(TEST_PERSON_0);

        inserted = this.dbTemplate.insertAsync(this.personInfo.getCollectionName(), TEST_PERSON_1, null)
                .toBlocking().single();

        assertThat(inserted).isEqualTo(TEST_PERSON_1);
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testInsertAsyncException() {
        this.dbTemplate.deleteAll(personInfo.getCollectionName(), personInfo.getPartitionKeyNames());
        this.dbTemplate.insertAsync(personInfo.getCollectionName(), TEST_PERSON_0, null).toCompletable().await();
        this.dbTemplate.insertAsync(personInfo.getCollectionName(), TEST_PERSON_0, null).toCompletable().await();
    }
}
