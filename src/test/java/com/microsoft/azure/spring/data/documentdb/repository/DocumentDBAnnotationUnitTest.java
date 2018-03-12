/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository;

import com.microsoft.azure.documentdb.IndexingPolicy;
import com.microsoft.azure.spring.data.documentdb.Constants;
import com.microsoft.azure.spring.data.documentdb.core.mapping.Document;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentIndexingPolicy;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
import com.microsoft.azure.spring.data.documentdb.domain.Role;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;


public class DocumentDBAnnotationUnitTest {
    private DocumentDbEntityInformation<Person, String> personInfo;
    private DocumentDbEntityInformation<Role, String> roleInfo;

    @Before
    public void setUp() {
        personInfo = new DocumentDbEntityInformation<>(Person.class);
        roleInfo = new DocumentDbEntityInformation<>(Role.class);
    }

    @Test
    public void testNoDocumentDBAnnotation() {
        final IndexingPolicy policy = personInfo.getIndexingPolicy();
        final Document documentAnnotation = Person.class.getAnnotation(Document.class);
        final DocumentIndexingPolicy policyAnnotation = Person.class.getAnnotation(DocumentIndexingPolicy.class);

        Assert.isNull(documentAnnotation, "Person class should not have Document annotation");
        Assert.isNull(policyAnnotation, "Person class should not have DocumentIndexingPolicy annotation");
        Assert.notNull(policy, "Person class collection policy should not be null");

        // CollectionName, RequestUnit, Automatic and IndexingMode
        Assert.isTrue(personInfo.getCollectionName().equals(Constants.DEFAULT_COLLECTION_NAME),
                "should be default collection name");
        Assert.isTrue(personInfo.getRequestUnit() == Constants.DEFAULT_REQUEST_UNIT,
                "should be default request unit");
        Assert.isTrue(policy.getAutomatic() == Constants.DEFAULT_INDEXINGPOLICY_AUTOMATIC,
                "should be default indexing policy automatic");
        Assert.isTrue(policy.getIndexingMode() == Constants.DEFAULT_INDEXINGPOLICY_MODE,
                "should be default indexing policy mode");

        // IncludedPaths and ExcludedPaths
        DocumentDBTestUtils.testIndexingPolicyPaths(policy.getIncludedPaths(), Constants.DEFAULT_INCLUDEDPATHS);
        DocumentDBTestUtils.testIndexingPolicyPaths(policy.getExcludedPaths(), Constants.DEFAULT_EXCLUDEDPATHS);
    }

    @Test
    public void testDocumentDBAnnotation() {
        final IndexingPolicy policy = roleInfo.getIndexingPolicy();
        final Document documentAnnotation = Role.class.getAnnotation(Document.class);
        final DocumentIndexingPolicy policyAnnotation = Role.class.getAnnotation(DocumentIndexingPolicy.class);

        // CollectionName, RequestUnit, Automatic and IndexingMode
        Assert.notNull(documentAnnotation, "Person class should have Document annotation");
        Assert.notNull(policyAnnotation, "Person class should have DocumentIndexingPolicy annotation");
        Assert.notNull(policy, "Person class collection policy should not be null");

        Assert.isTrue(roleInfo.getCollectionName().equals(Constants.TEST_COLLECTION_NAME),
                "should be Role(class) collection name");
        Assert.isTrue(roleInfo.getRequestUnit() == Constants.TEST_REQUEST_UNIT,
                "should be Role(class) request unit");
        Assert.isTrue(policy.getAutomatic() == Constants.TEST_INDEXINGPOLICY_AUTOMATIC,
                "should be Role(class) indexing policy automatic");
        Assert.isTrue(policy.getIndexingMode() == Constants.TEST_INDEXINGPOLICY_MODE,
                "should be Role(class) indexing policy mode");

        // IncludedPaths and ExcludedPaths
        DocumentDBTestUtils.testIndexingPolicyPaths(policy.getIncludedPaths(), Constants.TEST_INCLUDEDPATHS);
        DocumentDBTestUtils.testIndexingPolicyPaths(policy.getExcludedPaths(), Constants.TEST_EXCLUDEDPATHS);
    }
}

