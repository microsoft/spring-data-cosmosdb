/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentDbTemplateUnitTest {

    DocumentDbTemplate dbTemplate;

    @Mock
    DocumentClient documentClient;

    @Mock
    MappingDocumentDbConverter dbConverter;

    @Before
    public void setUp() {
        this.dbTemplate = new DocumentDbTemplate(new DocumentDbFactory(documentClient), dbConverter,
                TestConstants.DB_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectNullDbFactory() throws Exception {
        new DocumentDbTemplate(documentClient, null, TestConstants.DB_NAME);
    }
}
