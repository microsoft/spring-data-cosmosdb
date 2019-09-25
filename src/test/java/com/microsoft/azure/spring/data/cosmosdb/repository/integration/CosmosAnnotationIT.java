/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.ConsistencyLevel;
import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosClientBuilder;
import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.IndexingPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.CosmosMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.domain.Role;
import com.microsoft.azure.spring.data.cosmosdb.domain.TimeToLiveSample;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class CosmosAnnotationIT {
    private static final Role TEST_ROLE = new Role(TestConstants.ID_1, TestConstants.LEVEL,
            TestConstants.ROLE_NAME);

    @Value("${cosmosdb.uri}")
    private String dbUri;

    @Value("${cosmosdb.key}")
    private String dbKey;

    @Autowired
    private ApplicationContext applicationContext;

    private static CosmosClient cosmosClient;
    private static CosmosTemplate cosmosTemplate;
    private static CosmosContainerProperties collectionRole;
    private static CosmosContainerProperties collectionExample;
    private static CosmosMappingContext dbContext;
    private static MappingCosmosConverter mappingConverter;
    private static ObjectMapper objectMapper;
    private static CosmosEntityInformation<Role, String> roleInfo;
    private static CosmosEntityInformation<TimeToLiveSample, String> sampleInfo;

    private static boolean initialized;

    @Before
    public void setUp() throws ClassNotFoundException {
        if (!initialized) {
            final CosmosDBConfig dbConfig = CosmosDBConfig.builder(dbUri, dbKey, TestConstants.DB_NAME).build();
            final CosmosDbFactory cosmosDbFactory = new CosmosDbFactory(dbConfig);

            roleInfo = new CosmosEntityInformation<>(Role.class);
            sampleInfo = new CosmosEntityInformation<>(TimeToLiveSample.class);
            dbContext = new CosmosMappingContext();
            final ObjectMapper objectMapper = new ObjectMapper();

            dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

            mappingConverter =
                new MappingCosmosConverter(dbContext, objectMapper);
            cosmosClient = new CosmosClientBuilder()
                           .endpoint(dbUri)
                           .key(dbKey)
                           .connectionPolicy(ConnectionPolicy.defaultPolicy())
                           .consistencyLevel(ConsistencyLevel.SESSION)
                           .build();

            cosmosTemplate = new CosmosTemplate(cosmosDbFactory, mappingConverter, TestConstants.DB_NAME);
            initialized = true;
        }
        collectionRole = cosmosTemplate.createCollectionIfNotExists(roleInfo);
        collectionExample = cosmosTemplate.createCollectionIfNotExists(sampleInfo);

        cosmosTemplate.insert(roleInfo.getCollectionName(), TEST_ROLE, null);
    }

    @After
    public void cleanUp() {
        cosmosTemplate.deleteCollection(roleInfo.getCollectionName());
        cosmosTemplate.deleteCollection(sampleInfo.getCollectionName());
    }

    @Test
    @Ignore // TODO(pan): Ignore this test case for now, will update this from service update.
    public void testIndexingPolicyAnnotation() {
        final IndexingPolicy policy = collectionRole.indexingPolicy();

        Assert.isTrue(policy.automatic() == TestConstants.INDEXINGPOLICY_AUTOMATIC,
                "unmatched collection policy automatic of class Role");
        Assert.isTrue(policy.indexingMode() == TestConstants.INDEXINGPOLICY_MODE,
                "unmatched collection policy indexing mode of class Role");

        TestUtils.testIndexingPolicyPathsEquals(policy.includedPaths(), TestConstants.INCLUDEDPATHS);
        TestUtils.testIndexingPolicyPathsEquals(policy.excludedPaths(), TestConstants.EXCLUDEDPATHS);
    }

    @Test
    @SneakyThrows
    @Ignore //  TODO(kuthapar): time to live is not supported by v3 SDK.
    public void testDocumentAnnotationTimeToLive() {
        final TimeToLiveSample sample = new TimeToLiveSample(TestConstants.ID_1);
        // TODO: getDefaultTimeToLive is not available so should we delete the below?
//        final Integer timeToLive = collectionExample.getDefaultTimeToLive();
//        Assert.notNull(timeToLive, "timeToLive should not be null");
//        Assert.isTrue(timeToLive == TestConstants.TIME_TO_LIVE, "should be the same timeToLive");

        cosmosTemplate.insert(sampleInfo.getCollectionName(), sample, null);

        // Take care of following test, breakpoint may exhaust the time of TIME_TO_LIVE seconds.
        TimeToLiveSample found = cosmosTemplate.findById(sample.getId(), TimeToLiveSample.class);
        Assert.notNull(found, "Address should not be null");

        TimeUnit.SECONDS.sleep(TestConstants.TIME_TO_LIVE);
        TimeUnit.SECONDS.sleep(1); // make sure the time exhaust, the timing may not very precise.

        found = cosmosTemplate.findById(sample.getId(), TimeToLiveSample.class);
        Assert.isNull(found, "Timeout Address should be null");
    }
}

