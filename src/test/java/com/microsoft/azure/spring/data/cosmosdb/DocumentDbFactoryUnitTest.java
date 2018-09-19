/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb;

import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class DocumentDbFactoryUnitTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullKey() {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder(DOCUMENTDB_FAKE_HOST, null, DB_NAME).build();
        new DocumentDbFactory(dbConfig);
    }

    @Test
    public void testInvalidEndpoint() {
        final DocumentDBConfig dbConfig =
                DocumentDBConfig.builder(DOCUMENTDB_FAKE_HOST, DOCUMENTDB_FAKE_KEY, DB_NAME).build();
        final DocumentDbFactory factory = new DocumentDbFactory(dbConfig);

        assertThat(factory).isNotNull();
    }

    @Test
    public void testConnectWithConnectionString() {
        final DocumentDBConfig dbConfig =
                DocumentDBConfig.builder(DOCUMENTDB_FAKE_CONNECTION_STRING, DB_NAME).build();
        final DocumentDbFactory factory = new DocumentDbFactory(dbConfig);

        assertThat(factory).isNotNull();
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testInvalidConnectionString() {
        final DocumentDBConfig dbConfig =
                DocumentDBConfig.builder(DOCUMENTDB_INVALID_FAKE_CONNECTION_STRING, DB_NAME).build();
    }

    @Test
    public void testConnectionPolicyUserAgentKept() {
        final DocumentDBConfig dbConfig =
                DocumentDBConfig.builder(DOCUMENTDB_FAKE_HOST, DOCUMENTDB_FAKE_KEY, DB_NAME).build();
        final DocumentDbFactory factory = new DocumentDbFactory(dbConfig);

        final String uaSuffix = factory.getDocumentClient().getConnectionPolicy().getUserAgentSuffix();
        assertThat(uaSuffix).contains("spring-data");
    }
}
