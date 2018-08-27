/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository;

import com.microsoft.azure.documentdb.IndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentIndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.domain.DefaultDomain;
import com.microsoft.azure.spring.data.cosmosdb.domain.Role;
import com.microsoft.azure.spring.data.cosmosdb.domain.TimeToLiveSample;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;


public class DocumentDBAnnotationUnitTest {
    private DocumentDbEntityInformation<DefaultDomain, String> defaultDomainInfo;
    private DocumentDbEntityInformation<Role, String> roleInfo;

    @Before
    public void setUp() {
        defaultDomainInfo = new DocumentDbEntityInformation<>(DefaultDomain.class);
        roleInfo = new DocumentDbEntityInformation<>(Role.class);
    }

    @Test
    public void testDefaultIndexingPolicyAnnotation() {
        final IndexingPolicy policy = defaultDomainInfo.getIndexingPolicy();
        final Document documentAnnotation = DefaultDomain.class.getAnnotation(Document.class);
        final DocumentIndexingPolicy policyAnnotation = DefaultDomain.class.getAnnotation(DocumentIndexingPolicy.class);

        Assert.isNull(documentAnnotation, "DefaultDomain class should not have Document annotation");
        Assert.isNull(policyAnnotation, "DefaultDomain class should not have DocumentIndexingPolicy annotation");
        Assert.notNull(policy, "DefaultDomain class collection policy should not be null");

        // CollectionName, RequestUnit, Automatic and IndexingMode
        Assert.isTrue(defaultDomainInfo.getCollectionName().equals(TestConstants.DEFAULT_COLLECTION_NAME),
                "should be default collection name");
        Assert.isTrue(defaultDomainInfo.getRequestUnit() == TestConstants.DEFAULT_REQUEST_UNIT,
                "should be default request unit");
        Assert.isTrue(policy.getAutomatic() == TestConstants.DEFAULT_INDEXINGPOLICY_AUTOMATIC,
                "should be default indexing policy automatic");
        Assert.isTrue(policy.getIndexingMode() == TestConstants.DEFAULT_INDEXINGPOLICY_MODE,
                "should be default indexing policy mode");

        // IncludedPaths and ExcludedPaths
        // We do not use testIndexingPolicyPathsEquals generic here, for unit test do not create cosmosdb instance,
        // and the paths of policy will never be set from azure service.
        // testIndexingPolicyPathsEquals(policy.getIncludedPaths(), TestConstants.DEFAULT_INCLUDEDPATHS);
        // testIndexingPolicyPathsEquals(policy.getExcludedPaths(), TestConstants.DEFAULT_EXCLUDEDPATHS);
        Assert.isTrue(policy.getIncludedPaths().size() == 0, "default includedpaths size must be 0");
        Assert.isTrue(policy.getExcludedPaths().size() == 0, "default excludedpaths size must be 0");
    }

    @Test
    public void testIndexingPolicyAnnotation() {
        final IndexingPolicy policy = roleInfo.getIndexingPolicy();
        final Document documentAnnotation = Role.class.getAnnotation(Document.class);
        final DocumentIndexingPolicy policyAnnotation = Role.class.getAnnotation(DocumentIndexingPolicy.class);

        // CollectionName, RequestUnit, Automatic and IndexingMode
        Assert.notNull(documentAnnotation, "Role class should have Document annotation");
        Assert.notNull(policyAnnotation, "Role class should have DocumentIndexingPolicy annotation");
        Assert.notNull(policy, "Role class collection policy should not be null");

        Assert.isTrue(roleInfo.getCollectionName().equals(TestConstants.ROLE_COLLECTION_NAME),
                "should be Role(class) collection name");
        Assert.isTrue(roleInfo.getRequestUnit() == TestConstants.REQUEST_UNIT,
                "should be Role(class) request unit");
        Assert.isTrue(policy.getAutomatic() == TestConstants.INDEXINGPOLICY_AUTOMATIC,
                "should be Role(class) indexing policy automatic");
        Assert.isTrue(policy.getIndexingMode() == TestConstants.INDEXINGPOLICY_MODE,
                "should be Role(class) indexing policy mode");

        // IncludedPaths and ExcludedPaths
        TestUtils.testIndexingPolicyPathsEquals(policy.getIncludedPaths(), TestConstants.INCLUDEDPATHS);
        TestUtils.testIndexingPolicyPathsEquals(policy.getExcludedPaths(), TestConstants.EXCLUDEDPATHS);
    }

    @Test
    public void testDefaultDocumentAnnotationTimeToLive() {
        final Integer timeToLive = defaultDomainInfo.getTimeToLive();

        Assert.notNull(timeToLive, "timeToLive should not be null");
        Assert.isTrue(timeToLive == TestConstants.DEFAULT_TIME_TO_LIVE, "should be default time to live");
    }

    @Test
    public void testDocumentAnnotationTimeToLive() {
        final DocumentDbEntityInformation<TimeToLiveSample, String> info =
                new DocumentDbEntityInformation<>(TimeToLiveSample.class);
        final Integer timeToLive = info.getTimeToLive();

        Assert.notNull(timeToLive, "timeToLive should not be null");
        Assert.isTrue(timeToLive == TestConstants.TIME_TO_LIVE, "should be the same time to live");
    }
}

