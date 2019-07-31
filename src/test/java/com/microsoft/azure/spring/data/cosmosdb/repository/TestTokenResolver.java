/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository;

import com.azure.data.cosmos.CosmosResourceType;
import com.azure.data.cosmos.TokenResolver;
import com.azure.data.cosmos.internal.BaseAuthorizationTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@PropertySource(value = {"classpath:application.properties"})
public class TestTokenResolver implements TokenResolver {

    @Value("${cosmosdb.key:}")
    private String documentDbKey;

    @Override
    public String getAuthorizationToken(String requestVerb, String resourceIdOrFullName,
                                        CosmosResourceType resourceType, Map<String, Object> properties) {
        final BaseAuthorizationTokenProvider baseAuthorizationTokenProvider =
            new BaseAuthorizationTokenProvider(documentDbKey);
        return baseAuthorizationTokenProvider.generateKeyAuthorizationSignature(requestVerb, resourceIdOrFullName,
            resourceType.name(), new HashMap<>());
    }
}
