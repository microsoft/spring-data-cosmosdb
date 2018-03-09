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
import com.microsoft.azure.spring.data.documentdb.domain.PersonRole;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

public class DocumentDBAnnotationUnitTest {
    private DocumentDbEntityInformation<Person, String> personInfo;
    private DocumentDbEntityInformation<PersonRole, String> personRoleInfo;

    @Before
    public void setUp() {
        personInfo = new DocumentDbEntityInformation<>(Person.class);
        personRoleInfo = new DocumentDbEntityInformation<>(PersonRole.class);
    }

    @Test
    public void testNoDocumentDBAnnotation() {
        final IndexingPolicy policy = personInfo.getIndexingPolicy();
        final Document documentAnnotation = Person.class.getAnnotation(Document.class);
        final DocumentIndexingPolicy policyAnnotation = Person.class.getAnnotation(DocumentIndexingPolicy.class);

        Assert.isNull(documentAnnotation, "Person class should not have Document annotation");
        Assert.isNull(policyAnnotation, "Person class should not have DocumentIndexingPolicy annotation");

        Assert.isTrue(personInfo.getCollectionName().equals(Constants.DEFAULT_COLLECTION_NAME),
                "should be default collection name");
        Assert.isTrue(personInfo.getRequestUnit() == Constants.DEFAULT_REQUEST_UNIT,
                "should be default request unit");
        Assert.isTrue(policy.getAutomatic() == Constants.DEFAULT_INDEXINGPOLICY_AUTOMATIC,
                "should be default indexing policy automatic");
        Assert.isTrue(policy.getIndexingMode() == Constants.DEFAULT_INDEXINGPOLICY_MODE,
                "should be default indexing policy mode");
    }

    @Test
    public void testDocumentDBAnnotation() {
        final IndexingPolicy policy = personRoleInfo.getIndexingPolicy();
        final Document documentAnnotation = PersonRole.class.getAnnotation(Document.class);
        final DocumentIndexingPolicy policyAnnotation = PersonRole.class.getAnnotation(DocumentIndexingPolicy.class);

        Assert.notNull(documentAnnotation, "Person class should have Document annotation");
        Assert.notNull(policyAnnotation, "Person class should have DocumentIndexingPolicy annotation");

        Assert.isTrue(personRoleInfo.getCollectionName().equals(Constants.COLLECTION_NAME),
                "should be PersonRole(class) collection name");
        Assert.isTrue(personRoleInfo.getRequestUnit() == Constants.REQUEST_UNIT,
                "should be PersonRole(class) request unit");
        Assert.isTrue(policy.getAutomatic() == Constants.INDEXINGPOLICY_AUTOMATIC,
                "should be PersonRole(class) indexing policy automatic");
        Assert.isTrue(policy.getIndexingMode() == Constants.INDEXINGPOLICY_MODE,
                "should be PersonRole(class) indexing policy mode");
    }
}

