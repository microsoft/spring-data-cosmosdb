/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb;

import com.azure.data.cosmos.ConnectionMode;
import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.ConsistencyLevel;
import com.azure.data.cosmos.CosmosClient;
import com.microsoft.azure.spring.data.cosmosdb.common.MacAddress;
import com.microsoft.azure.spring.data.cosmosdb.common.PropertyLoader;
import com.microsoft.azure.spring.data.cosmosdb.common.TelemetrySender;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Locale;

public class CosmosDbFactory {

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

    public CosmosDbFactory(@NonNull DocumentDBConfig config) {
        validateConfig(config);

        this.config = config;
    }

    public CosmosClient getCosmosClient() {
        final ConnectionPolicy policy = ConnectionPolicy.defaultPolicy();
        final String userAgent = getUserAgentSuffix() + ";" + config.getConnectionPolicy().getUserAgentSuffix();

        policy.userAgentSuffix(userAgent);
        policy.connectionMode(ConnectionMode.DIRECT);

        String consistencyLevelString = null;
        if (config.getConsistencyLevel() != null) {
             consistencyLevelString = config.getConsistencyLevel()
                                            .name()
                                            .toUpperCase(Locale.getDefault());
        }

        //  Setting Direct Mode Protocol as Https
        //  There is some version issue with Tcp protocol
        System.setProperty(Constants.AZURE_COSMOS_DIRECT_MODE_PROTOCOL_PROPERTY, "Https");
        return CosmosClient.builder()
                           .endpoint(config.getUri())
                           .key(config.getKey())
                           .cosmosKeyCredential(config.getCosmosKeyCredential())
                           .connectionPolicy(policy)
                           .consistencyLevel(consistencyLevelString != null ?
                               ConsistencyLevel.valueOf(consistencyLevelString) : ConsistencyLevel.SESSION)
                           .build();
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
