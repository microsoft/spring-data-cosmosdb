/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class DocumentDbFactoryUnitTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNullKey() {
        new DocumentDbFactory(TestConstants.DOCUMENTDB_FAKE_HOST, null);
    }

    @Test
    public void testInvalidEndpoint() {
        final DocumentDbFactory factory = new DocumentDbFactory(TestConstants.DOCUMENTDB_FAKE_HOST,
                TestConstants.DOCUMENTDB_FAKE_KEY);
        assertThat(factory).isNotNull();
    }

    @Test
    public void testConnectionPolicyUserAgentKept() {
        final String testUserAgentSuffix = "test-user-agent-suffix";

        final ConnectionPolicy policy = ConnectionPolicy.GetDefault();
        policy.setUserAgentSuffix(testUserAgentSuffix);
        final DocumentClient documentClient = new DocumentClient(TestConstants.DOCUMENTDB_FAKE_HOST,
                TestConstants.DOCUMENTDB_FAKE_HOST, policy, ConsistencyLevel.Session);
        final DocumentDbFactory factory = new DocumentDbFactory(documentClient);

        final String uaSuffix = factory.getDocumentClient().getConnectionPolicy().getUserAgentSuffix();
        assertThat(uaSuffix).contains(testUserAgentSuffix);
    }
}
