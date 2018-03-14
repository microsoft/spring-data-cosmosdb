/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class DocumentDbFactoryUnitTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNullKey() throws Exception {
        new DocumentDbFactory(TestConstants.DOCUMENTDB_FAKE_HOST, null);
    }

    @Test
    public void testInvalidEndpoint() {
        final DocumentDbFactory factory = new DocumentDbFactory(TestConstants.DOCUMENTDB_FAKE_HOST,
                TestConstants.DOCUMENTDB_FAKE_KEY);
        assertThat(factory).isNotNull();
    }
}
