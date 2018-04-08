/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository;

import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.TestUtils;
import com.microsoft.azure.spring.data.documentdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.documentdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.documentdb.domain.Address;
import com.microsoft.azure.spring.data.documentdb.domain.Role;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
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
import org.springframework.util.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class DocumentDBAnnotationIT {
    private static final Role TEST_ROLE = new Role(TestConstants.ID, TestConstants.LEVEL,
            TestConstants.ROLE_NAME);
    private static final Address TEST_ADDRESS = new Address(TestConstants.POSTAL_CODE, TestConstants.STREET,
            TestConstants.CITY);

    @Value("${documentdb.uri}")
    private String dbUri;

    @Value("${documentdb.key}")
    private String dbKey;

    @Autowired
    private ApplicationContext applicationContext;

    private DocumentClient dbClient;
    private DocumentDbTemplate dbTemplate;
    private DocumentCollection collectionRole;
    private DocumentCollection collectionAddress;
    private DocumentDbMappingContext dbContext;
    private MappingDocumentDbConverter mappingConverter;
    private DocumentDbEntityInformation<Role, String> roleInfo;
    private DocumentDbEntityInformation<Address, String> addressInfo;

    @Before
    public void setUp() throws ClassNotFoundException {
        roleInfo = new DocumentDbEntityInformation<>(Role.class);
        addressInfo = new DocumentDbEntityInformation<>(Address.class);
        dbContext = new DocumentDbMappingContext();

        dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        mappingConverter = new MappingDocumentDbConverter(dbContext);
        dbClient = new DocumentClient(dbUri, dbKey, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
        dbTemplate = new DocumentDbTemplate(dbClient, mappingConverter, TestConstants.DB_NAME);

        collectionRole = dbTemplate.createCollectionIfNotExists(roleInfo, null);
        collectionAddress = dbTemplate.createCollectionIfNotExists(addressInfo, null);

        dbTemplate.insert(roleInfo.getCollectionName(), TEST_ROLE, null);
        dbTemplate.insert(addressInfo.getCollectionName(), TEST_ADDRESS, null);
    }

    @After
    public void cleanUp() {
        dbTemplate.deleteAll(roleInfo.getCollectionName());
        dbTemplate.deleteAll(addressInfo.getCollectionName());
    }

    @Test
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
    public void testDocumentAnnotationTimeToLive() {
        final Integer timeToLive = collectionAddress.getDefaultTimeToLive();

        Assert.notNull(timeToLive, "timeToLive should not be null");
        Assert.isTrue(timeToLive == TestConstants.TIME_TO_LIVE, "should be the same timeToLive");
    }
}

