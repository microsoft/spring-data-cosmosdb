/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.IndexingPolicy;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.annotation.Persistent;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Assert;

import java.util.Optional;
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

    @Value("${cosmosdb.database:testdb}")
    private String dbName;

    @Autowired
    private ApplicationContext applicationContext;

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
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(dbUri, dbKey, dbName).build();
        final DocumentDbFactory dbFactory = new DocumentDbFactory(dbConfig);

        roleInfo = new DocumentDbEntityInformation<>(Role.class);
        sampleInfo = new DocumentDbEntityInformation<>(TimeToLiveSample.class);
        dbContext = new DocumentDbMappingContext();
        objectMapper = new ObjectMapper();

        dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        mappingConverter = new MappingDocumentDbConverter(dbContext, objectMapper);
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
    public void testIndexingPolicyAnnotation() {
        final IndexingPolicy policy = collectionRole.getIndexingPolicy();

        Assert.assertEquals(policy.getAutomatic(), TestConstants.INDEXINGPOLICY_AUTOMATIC);
        Assert.assertEquals(policy.getIndexingMode(), TestConstants.INDEXINGPOLICY_MODE);

        TestUtils.testIndexingPolicyPathsEquals(policy.getIncludedPaths(), TestConstants.INCLUDEDPATHS);
        TestUtils.testIndexingPolicyPathsEquals(policy.getExcludedPaths(), TestConstants.EXCLUDEDPATHS);
    }

    @Test
    @SneakyThrows
    public void testDocumentAnnotationTimeToLive() {
        final TimeToLiveSample sample = new TimeToLiveSample(TestConstants.ID_1);
        final Integer timeToLive = this.collectionExample.getDefaultTimeToLive();

        Assert.assertNotNull(timeToLive);
        Assert.assertEquals(timeToLive, Integer.valueOf(TestConstants.TIME_TO_LIVE));

        dbTemplate.insert(sampleInfo.getCollectionName(), sample, null);

        // Take care of following test, breakpoint may exhaust the time of TIME_TO_LIVE seconds.
        Optional<TimeToLiveSample> optional = dbTemplate.findById(sampleInfo.getCollectionName(), sample.getId(),
                TimeToLiveSample.class, null);
        Assert.assertTrue(optional.isPresent());

        TimeUnit.SECONDS.sleep(TestConstants.TIME_TO_LIVE);
        TimeUnit.SECONDS.sleep(1); // make sure the time exhaust, the timing may not very precise.

        optional = dbTemplate.findById(sampleInfo.getCollectionName(), sample.getId(), TimeToLiveSample.class, null);
        Assert.assertFalse(optional.isPresent());
    }
}

