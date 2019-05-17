/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.microsoft.azure.cosmos.CosmosContainer;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.After;
import org.junit.Assert;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.DB_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class ReactiveCosmosTemplateIT {
    private static final Person TEST_PERSON = new Person(TestConstants.ID_1, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_2 = new Person(TestConstants.ID_2, TestConstants.NEW_FIRST_NAME,
            TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_3 = new Person(TestConstants.ID_3, TestConstants.NEW_FIRST_NAME,
            TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final Person TEST_PERSON_4 = new Person(TestConstants.ID_4, TestConstants.NEW_FIRST_NAME,
            TestConstants.NEW_LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);

    @Value("${cosmosdb.uri}")
    private String documentDbUri;
    @Value("${cosmosdb.key}")
    private String documentDbKey;

    private ReactiveCosmosTemplate dbTemplate;
    private MappingDocumentDbConverter dbConverter;
    private DocumentDbMappingContext mappingContext;
    private ObjectMapper objectMapper;
    private CosmosContainer cosmosContainer;
    private DocumentDbEntityInformation<Person, String> personInfo;
    private String containerName;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(documentDbUri, documentDbKey, DB_NAME).build();
        final CosmosDbFactory dbFactory = new CosmosDbFactory(dbConfig);

        mappingContext = new DocumentDbMappingContext();
        objectMapper = new ObjectMapper();
        personInfo = new DocumentDbEntityInformation<>(Person.class);
        containerName = personInfo.getCollectionName();

        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        dbConverter = new MappingDocumentDbConverter(mappingContext, objectMapper);
        
        try {
            Base64.getDecoder().decode(documentDbKey.getBytes());
            System.out.println("valid dbkey");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid dbkey!! ");
        }

        try {
            org.apache.commons.codec.binary.Base64.decodeBase64(documentDbKey.getBytes());
            System.out.println("apache base64: Valid dbkey");
        } catch (IllegalArgumentException e) {
            System.out.println("apache base64: Invalid dbkey!");
        }

        try {
            Base64.getDecoder().decode(documentDbKey.getBytes(Charsets.UTF_8));
            System.out.println("utf8: Valid dbkey!");
        } catch (IllegalArgumentException e) {
            System.out.println("utf8: Invalid dbkey!");
        }

        System.out.println("docdb: .substring(40) = " + documentDbKey.substring(40));
        
        System.out.println("cosmos: .substring(40) = " + documentDbKey.substring(25));
        dbTemplate = new ReactiveCosmosTemplate(dbFactory, dbConverter, DB_NAME);
        cosmosContainer = dbTemplate.createCollectionIfNotExists(this.personInfo).block().getContainer();
        dbTemplate.insert(TEST_PERSON).block();
    }

    @After
    public void cleanup() {
        dbTemplate.deleteContainer(Person.class.getSimpleName());
    }

//    @Test
//    public void testInsertDuplicateId() {
//        final Mono<Person> insertMono = dbTemplate.insert(TEST_PERSON);
//        final TestSubscriber testSubscriber = new TestSubscriber();
//        insertMono.subscribe(testSubscriber);
//        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertNotComplete();
//        testSubscriber.assertTerminated();
//        assertThat(testSubscriber.errors()).hasSize(1);
//        assertThat(((List) testSubscriber.getEvents().get(1)).get(0)).isInstanceOf(DocumentClientException.class);
//    }

    @Test
    public void testFindByID() {
        final Mono<Person> findById = dbTemplate.findById(Person.class.getSimpleName(), TEST_PERSON.getId(),
                Person.class);
        StepVerifier.create(findById).consumeNextWith(actual -> {
            Assert.assertThat(actual.getFirstName(), is(equalTo(TEST_PERSON.getFirstName())));
            Assert.assertThat(actual.getLastName(), is(equalTo(TEST_PERSON.getLastName())));
        }).verifyComplete();
    }

//    @Test
//    public void testFindAll() {
//        final Flux<Person> flux = dbTemplate.findAll(Person.class.getSimpleName(), Person.class);
//        StepVerifier.create(flux).expectNextCount(1).verifyComplete();
//    }
//
//    @Test
//    public void testFindByIdWithContainerName() {
//        StepVerifier.create(dbTemplate.findById(Person.class.getSimpleName(), TEST_PERSON.getId(), Person.class))
//                .consumeNextWith(actual -> {
//                    Assert.assertThat(actual.getFirstName(), is(equalTo(TEST_PERSON.getFirstName())));
//                    Assert.assertThat(actual.getLastName(), is(equalTo(TEST_PERSON.getLastName())));
//                }).verifyComplete();
//    }
//
//    @Test
//    public void testInsert() {
//        StepVerifier.create(dbTemplate.insert(TEST_PERSON_3))
//                .expectNext(TEST_PERSON_3).verifyComplete();
//    }
//
//    @Test
//    public void testInsertWithCollectionName() {
//        StepVerifier.create(dbTemplate.insert(Person.class.getSimpleName(), TEST_PERSON_2, null))
//                .expectNext(TEST_PERSON_2).verifyComplete();
//    }
//
//    @Test
//    public void testUpsert() {
//        final Person p = TEST_PERSON_2;
//        final ArrayList<String> hobbies = new ArrayList<>(p.getHobbies());
//        hobbies.add("more code");
//        p.setHobbies(hobbies);
//        final Mono<Person> upsert = dbTemplate.upsert(p, null);
//        StepVerifier.create(upsert).expectNextCount(1).verifyComplete();
//    }
//
//    @Test
//    public void testUpsertWithCollectionName() {
//        final Person p = TEST_PERSON_2;
//        final ArrayList<String> hobbies = new ArrayList<>(p.getHobbies());
//        hobbies.add("more code");
//        p.setHobbies(hobbies);
//        final Mono<Person> upsert = dbTemplate.upsert(Person.class.getSimpleName(), p, null);
//        StepVerifier.create(upsert).expectNextCount(1).verifyComplete();
//    }
//
//    @Test
//    public void testDeleteById() {
//        dbTemplate.insert(TEST_PERSON_4).block();
//        final Mono<Void> voidMono = dbTemplate.deleteById(Person.class.getSimpleName(), TEST_PERSON_4.getId(),
//                new PartitionKey(TEST_PERSON_4.getId()));
//        StepVerifier.create(voidMono).verifyComplete();
//    }
//
//    @Test
//    public void testFind() {
//        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
//                Arrays.asList(TEST_PERSON.getFirstName()));
//        final DocumentQuery query = new DocumentQuery(criteria);
//        final Flux<Person> personFlux = dbTemplate.find(query, Person.class, Person.class.getSimpleName());
//        StepVerifier.create(personFlux).expectNextCount(1).verifyComplete();
//    }
//
//    @Test
//    public void testExists() {
//        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
//                Arrays.asList(TEST_PERSON.getFirstName()));
//        final DocumentQuery query = new DocumentQuery(criteria);
//        final Mono<Boolean> exists = dbTemplate.exists(query, Person.class, containerName);
//        StepVerifier.create(exists).expectNext(true).verifyComplete();
//    }
//
//    @Test
//    public void testCount() {
//        final Mono<Long> count = dbTemplate.count(containerName);
//        StepVerifier.create(count).expectNext((long) 1).verifyComplete();
//    }

}
