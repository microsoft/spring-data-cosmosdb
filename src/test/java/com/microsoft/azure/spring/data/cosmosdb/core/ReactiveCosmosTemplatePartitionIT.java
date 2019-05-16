/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.cosmos.CosmosContainer;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.PartitionPerson;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
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

import java.util.Arrays;
import java.util.UUID;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.ADDRESSES;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.DB_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.FIRST_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.HOBBIES;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.ID_1;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.ID_2;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.LAST_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.NEW_FIRST_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.NEW_LAST_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.PROPERTY_LAST_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.UPDATED_FIRST_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType.IS_EQUAL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class ReactiveCosmosTemplatePartitionIT {
    private static final PartitionPerson TEST_PERSON = new PartitionPerson(ID_1, FIRST_NAME, LAST_NAME,
            HOBBIES, ADDRESSES);

    private static final PartitionPerson TEST_PERSON_2 = new PartitionPerson(ID_2, NEW_FIRST_NAME,
            TEST_PERSON.getLastName(), HOBBIES, ADDRESSES);

    @Value("${cosmosdb.uri}")
    private String documentDbUri;
    @Value("${cosmosdb.key}")
    private String documentDbKey;

    private ReactiveCosmosTemplate dbTemplate;
    private MappingDocumentDbConverter dbConverter;
    private DocumentDbMappingContext mappingContext;
    private ObjectMapper objectMapper;
    private CosmosContainer cosmosContainer;
    private DocumentDbEntityInformation<PartitionPerson, String> personInfo;
    private String containerName;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws ClassNotFoundException {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(documentDbUri, documentDbKey, DB_NAME).build();
        final CosmosDbFactory dbFactory = new CosmosDbFactory(dbConfig);

        mappingContext = new DocumentDbMappingContext();
        objectMapper = new ObjectMapper();
        personInfo = new DocumentDbEntityInformation<>(PartitionPerson.class);
        containerName = personInfo.getCollectionName();

        mappingContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        dbConverter = new MappingDocumentDbConverter(mappingContext, objectMapper);
        dbTemplate = new ReactiveCosmosTemplate(dbFactory, dbConverter, DB_NAME);

        cosmosContainer = dbTemplate.createCollectionIfNotExists(this.personInfo).block().getContainer();
        dbTemplate.insert(TEST_PERSON).block();
    }

    @After
    public void cleanup() {
        dbTemplate.deleteContainer(PartitionPerson.class.getSimpleName());
    }

    @Test
    public void testFindWithPartition() {
        final Criteria criteria = Criteria.getInstance(IS_EQUAL, PROPERTY_LAST_NAME, Arrays.asList(LAST_NAME));
        final DocumentQuery query = new DocumentQuery(criteria);
        final Flux<PartitionPerson> partitionPersonFlux = dbTemplate.find(query, PartitionPerson.class,
                PartitionPerson.class.getSimpleName());
        StepVerifier.create(partitionPersonFlux).consumeNextWith(actual -> {
            Assert.assertThat(actual.getFirstName(), is(equalTo(TEST_PERSON.getFirstName())));
            Assert.assertThat(actual.getLastName(), is(equalTo(TEST_PERSON.getLastName())));
        }).verifyComplete();
    }

    @Test
    public void testFindByNonExistIdWithPartition() {

    }

    @Test
    public void testUpsertNewDocumentPartition() {
        final String firstName = NEW_FIRST_NAME + "_" + UUID.randomUUID().toString();
        final PartitionPerson newPerson = new PartitionPerson(UUID.randomUUID().toString(), firstName, NEW_LAST_NAME,
                null, null);
        final String partitionKeyValue = newPerson.getLastName();
        final Mono<PartitionPerson> upsert = dbTemplate.upsert(newPerson, new PartitionKey(partitionKeyValue));
        StepVerifier.create(upsert).expectNextCount(1).verifyComplete();
    }

    @Test
    public void testUpdateWithPartition() {
        final PartitionPerson updated = new PartitionPerson(TEST_PERSON.getId(), UPDATED_FIRST_NAME,
                TEST_PERSON.getLastName(), TEST_PERSON.getHobbies(), TEST_PERSON.getShippingAddresses());
        dbTemplate.upsert(updated, new PartitionKey(updated.getLastName())).block();

        final PartitionPerson person = dbTemplate.findAll(PartitionPerson.class.getSimpleName(), PartitionPerson.class)
                .toStream()
                .filter(p -> TEST_PERSON.getId().equals(p.getId())).findFirst().get();
        assertTrue(person.equals(updated));
    }

    @Test
    public void testDeleteByIdPartition() {
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName())).block();
        System.out.println("TEST_PERSON_2 = " + TEST_PERSON_2);
        StepVerifier.create(dbTemplate.findAll(PartitionPerson.class)).expectNextCount(2).verifyComplete();

        dbTemplate.deleteById(PartitionPerson.class.getSimpleName(),
                TEST_PERSON.getId(), new PartitionKey(TEST_PERSON.getLastName())).block();
        StepVerifier.create(dbTemplate.findAll(PartitionPerson.class))
                .expectNext(TEST_PERSON_2)
                .verifyComplete();
    }

    @Test
    public void testCountForPartitionedCollection() {
        StepVerifier.create(dbTemplate.count(containerName)).expectNext((long) 1).verifyComplete();
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName())).block();
        StepVerifier.create(dbTemplate.count(containerName)).expectNext((long) 2).verifyComplete();
    }

    @Test
    public void testCountForPartitionedCollectionByQuery() {
        dbTemplate.insert(TEST_PERSON_2, new PartitionKey(TEST_PERSON_2.getLastName())).block();
        final Criteria criteria = Criteria.getInstance(CriteriaType.IS_EQUAL, "firstName",
                Arrays.asList(TEST_PERSON_2.getFirstName()));
        final DocumentQuery query = new DocumentQuery(criteria);
        StepVerifier.create(dbTemplate.count(query, containerName)).expectNext((long) 1).verifyComplete();

    }
}

