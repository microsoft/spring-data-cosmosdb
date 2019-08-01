/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.domain.Role;
import com.microsoft.azure.spring.data.cosmosdb.domain.TimeToLiveSample;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
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
public class DocumentDBAnnotationIT {
    private static final Role TEST_ROLE = new Role(TestConstants.ID_1, TestConstants.LEVEL,
            TestConstants.ROLE_NAME);

    @Value("${cosmosdb.uri}")
    private String dbUri;

    @Value("${cosmosdb.key}")
    private String dbKey;

    @Autowired
    private ApplicationContext applicationContext;

    private DocumentClient dbClient;
    private DocumentDbTemplate dbTemplate;
    private DocumentCollection collectionRole;
    private DocumentCollection collectionExample;
    private DocumentDbMappingContext dbContext;
    private MappingDocumentDbConverter mappingConverter;
    private ObjectMapper objectMapper;
    private DocumentDbEntityInformation<Role, String> roleInfo;
    private DocumentDbEntityInformation<TimeToLiveSample, String> sampleInfo;

    @Before
    public void setUp() throws ClassNotFoundException {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(dbUri, dbKey, TestConstants.DB_NAME).build();
        final DocumentDbFactory dbFactory = new DocumentDbFactory(dbConfig);

        roleInfo = new DocumentDbEntityInformation<>(Role.class);
        sampleInfo = new DocumentDbEntityInformation<>(TimeToLiveSample.class);
        dbContext = new DocumentDbMappingContext();
        objectMapper = new ObjectMapper();

        dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        mappingConverter = new MappingDocumentDbConverter(dbContext, objectMapper);
        dbClient = new DocumentClient(dbUri, dbKey, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
        dbTemplate = new DocumentDbTemplate(dbFactory, mappingConverter, TestConstants.DB_NAME);

        collectionRole = dbTemplate.createCollectionIfNotExists(roleInfo);
        collectionExample = dbTemplate.createCollectionIfNotExists(sampleInfo);

        dbTemplate.insert(roleInfo.getCollectionName(), TEST_ROLE, null);
    }

    @After
    public void cleanUp() {
        dbTemplate.deleteCollection(roleInfo.getCollectionName());
        dbTemplate.deleteCollection(sampleInfo.getCollectionName());
    }

    @Test
    @Ignore // TODO(pan): Ignore this test case for now, will update this from service update.
    public void testIndexingPolicyAnnotation() {
        final IndexingPolicy policy = collectionRole.getIndexingPolicy();

        Assert.isTrue(policy.getAutomatic() == TestConstants.INDEXINGPOLICY_AUTOMATIC,
                "unmatched collection policy automatic of class Role");
        Assert.isTrue(policy.getIndexingMode() == TestConstants.INDEXINGPOLICY_MODE,
                "unmatched collection policy indexing mode of class Role");

        TestUtils.testIndexingPolicyPathsEquals(policy.getIncludedPaths(), TestConstants.INCLUDEDPATHS);
        TestUtils.testIndexingPolicyPathsEquals(policy.getExcludedPaths(), TestConstants.EXCLUDEDPATHS);
    }

    @Test
    @SneakyThrows
    public void testDocumentAnnotationTimeToLive() {
        final TimeToLiveSample sample = new TimeToLiveSample(TestConstants.ID_1);
        final Integer timeToLive = this.collectionExample.getDefaultTimeToLive();

        Assert.notNull(timeToLive, "timeToLive should not be null");
        Assert.isTrue(timeToLive == TestConstants.TIME_TO_LIVE, "should be the same timeToLive");

        dbTemplate.insert(sampleInfo.getCollectionName(), sample, null);

        // Take care of following test, breakpoint may exhaust the time of TIME_TO_LIVE seconds.
        TimeToLiveSample found = dbTemplate.findById(sample.getId(), TimeToLiveSample.class);
        Assert.notNull(found, "Address should not be null");

        TimeUnit.SECONDS.sleep(TestConstants.TIME_TO_LIVE);
        TimeUnit.SECONDS.sleep(1); // make sure the time exhaust, the timing may not very precise.

        found = dbTemplate.findById(sample.getId(), TimeToLiveSample.class);
        Assert.isNull(found, "Timeout Address should be null");
    }
}

