/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.IndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.CosmosMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.domain.Role;
import com.microsoft.azure.spring.data.cosmosdb.domain.TimeToLiveSample;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class CosmosAnnotationIT {
    private static final Role TEST_ROLE = new Role(TestConstants.ID_1, TestConstants.LEVEL,
            TestConstants.ROLE_NAME);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private CosmosDBConfig dbConfig;

    private static CosmosTemplate cosmosTemplate;
    private static CosmosContainerProperties collectionRole;
    private static CosmosEntityInformation<Role, String> roleInfo;
    private static CosmosEntityInformation<TimeToLiveSample, String> sampleInfo;

    private static boolean initialized;

    @Before
    public void setUp() throws ClassNotFoundException {
        if (!initialized) {
            final CosmosDbFactory cosmosDbFactory = new CosmosDbFactory(dbConfig);

            roleInfo = new CosmosEntityInformation<>(Role.class);
            sampleInfo = new CosmosEntityInformation<>(TimeToLiveSample.class);
            final CosmosMappingContext dbContext = new CosmosMappingContext();

            dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

            final MappingCosmosConverter mappingConverter = new MappingCosmosConverter(dbContext, null);

            cosmosTemplate = new CosmosTemplate(cosmosDbFactory, mappingConverter, dbConfig.getDatabase());
            initialized = true;
        }
        collectionRole = cosmosTemplate.createCollectionIfNotExists(roleInfo);

        cosmosTemplate.createCollectionIfNotExists(sampleInfo);

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
}

