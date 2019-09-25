/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb;

import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.CosmosClient;
import com.microsoft.azure.spring.data.cosmosdb.common.PropertyLoader;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertyLoader.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.crypto.*"})
public class UserAgentTest {
    private static final String TEST_VERSION = "1.0.0-FOR-TEST";

    // TODO: Enable test after finding a workaround for the todo below
    @Ignore
    @Test
    public void testUserAgentSuffixAppended() {
        PowerMockito.mockStatic(PropertyLoader.class);
        BDDMockito.given(PropertyLoader.getProjectVersion()).willReturn(TEST_VERSION);
        
        assertThat(PropertyLoader.getProjectVersion()).isEqualTo(TEST_VERSION);

        final CosmosDBConfig dbConfig = CosmosDBConfig.builder("https://uri/",
                "key", 
                TestConstants.DB_NAME).build();

        final CosmosDbFactory factory = new CosmosDbFactory(dbConfig);
        // TODO: Using reflection as getConnectionPolicy is not public
        final Class<? extends CosmosClient> aClass = factory.getCosmosClient().getClass();
        String userAgent = "what?";
        try {
            System.out.println("Using reflection to get details");
            final Method aMethod = aClass.getDeclaredMethod("getConnectionPolicy");
            aMethod.setAccessible(true);
                final ConnectionPolicy policy = (ConnectionPolicy) aMethod.invoke(factory.getCosmosClient());
            System.out.println("policy = " + policy);
            userAgent = policy.userAgentSuffix();

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        assertThat(userAgent).contains(TEST_VERSION);
    }

}
