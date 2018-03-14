/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb;

import com.microsoft.azure.spring.data.documentdb.common.PropertyLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertyLoader.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.crypto.*"})
public class UserAgentTest {
    @Test
    public void testUserAgentSuffixAppended() {
        PowerMockito.mockStatic(PropertyLoader.class);
        BDDMockito.given(PropertyLoader.getProjectVersion()).willReturn(TestConstants.VERSION);

        assertThat(PropertyLoader.getProjectVersion()).isEqualTo(TestConstants.VERSION);

        final DocumentDbFactory factory = new DocumentDbFactory(TestConstants.DOCUMENTDB_FAKE_HOST,
                TestConstants.DOCUMENTDB_FAKE_KEY);
        assertThat(factory.getDocumentClient().getConnectionPolicy().getUserAgentSuffix())
                .contains(TestConstants.VERSION);
    }

}
