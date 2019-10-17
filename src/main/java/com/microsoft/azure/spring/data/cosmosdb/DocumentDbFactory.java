/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.common.MacAddress;
import com.microsoft.azure.spring.data.cosmosdb.common.PropertyLoader;
import com.microsoft.azure.spring.data.cosmosdb.common.TelemetrySender;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * {@link DocumentDbFactory} is deprecated.
 * Instead use {@link CosmosDbFactory}
 */
@Deprecated
public class DocumentDbFactory {

    @Getter
    private final DocumentDBConfig config;

    private static final boolean IS_TELEMETRY_ALLOWED = PropertyLoader.isApplicationTelemetryAllowed();

    private static final String USER_AGENT_SUFFIX = Constants.USER_AGENT_SUFFIX + PropertyLoader.getProjectVersion();

    private String getUserAgentSuffix() {
        String suffix = ";" + USER_AGENT_SUFFIX;

        if (IS_TELEMETRY_ALLOWED) {
            suffix += ";" + MacAddress.getHashMac();
        }

        return suffix;
    }

    public DocumentDbFactory(@NonNull DocumentDBConfig config) {
        validateConfig(config);

        this.config = config;
    }

    public DocumentClient getDocumentClient() {
        final ConnectionPolicy policy = config.getConnectionPolicy();
        final String userAgent = getUserAgentSuffix() + ";" + policy.getUserAgentSuffix();

        policy.setUserAgentSuffix(userAgent);
        
        //  With introduction to com.azure.data.cosmos.CosmosKeyCredential,
        //  we are giving preference to config.getCosmosKeyCredential()
        if (config.getCosmosKeyCredential() != null &&
            !StringUtils.isEmpty(config.getCosmosKeyCredential().key())) {
            return new DocumentClient(config.getUri(), config.getCosmosKeyCredential().key(),
                policy, config.getConsistencyLevel());
        }
        return new DocumentClient(config.getUri(), config.getKey(), policy, config.getConsistencyLevel());
    }

    private void validateConfig(@NonNull DocumentDBConfig config) {
        Assert.hasText(config.getUri(), "cosmosdb host url should have text!");
        if (config.getCosmosKeyCredential() == null) {
            Assert.hasText(config.getKey(), "cosmosdb host key should have text!");
        } else if (StringUtils.isEmpty(config.getKey())) {
            Assert.hasText(config.getCosmosKeyCredential().key(),
                "cosmosdb credential host key should have text!");
        }
        Assert.hasText(config.getDatabase(), "cosmosdb database should have text!");
        Assert.notNull(config.getConnectionPolicy(), "cosmosdb connection policy should not be null!");
    }

    @PostConstruct
    private void sendTelemetry() {
        if (IS_TELEMETRY_ALLOWED) {
            final TelemetrySender sender = new TelemetrySender();

            sender.send(this.getClass().getSimpleName());
        }
    }
}
