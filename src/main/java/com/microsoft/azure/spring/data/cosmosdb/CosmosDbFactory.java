/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb;

import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.sync.CosmosSyncClient;
import com.microsoft.azure.spring.data.cosmosdb.common.MacAddress;
import com.microsoft.azure.spring.data.cosmosdb.common.PropertyLoader;
import com.microsoft.azure.spring.data.cosmosdb.common.TelemetrySender;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

//  TODO: Database Factory interface, which exposese these methods to get the databases
//  Configure the template using database factory.
//  Ask the database factory to give you the instance.
public class CosmosDbFactory {

    @Getter
    private final CosmosDBConfig config;

    private static final boolean IS_TELEMETRY_ALLOWED = PropertyLoader.isApplicationTelemetryAllowed();

    private static final String USER_AGENT_SUFFIX = Constants.USER_AGENT_SUFFIX + PropertyLoader.getProjectVersion();

    private String getUserAgentSuffix() {
        String suffix = ";" + USER_AGENT_SUFFIX;

        if (IS_TELEMETRY_ALLOWED) {
            suffix += ";" + MacAddress.getHashMac();
        }

        return suffix;
    }

    public CosmosDbFactory(@NonNull CosmosDBConfig config) {
        validateConfig(config);

        this.config = config;
    }

    //  TODO: Stick with the driver, do not need to expose auxillary APIs.
    //  spring-data-mongo, cassandra configuration.
    //  Only for spring-boot -> Builder customizer pattern, which can customize the cosmos client.
    //  spring-boot transactionManager customizer, mongo client customizer.
    public CosmosClient getCosmosClient() {
        final ConnectionPolicy policy = config.getConnectionPolicy();
        final String userAgent = getUserAgentSuffix() + ";" + policy.userAgentSuffix();

        policy.userAgentSuffix(userAgent);
        return CosmosClient.builder()
                           .endpoint(config.getUri())
                           .key(config.getKey())
                           .cosmosKeyCredential(config.getCosmosKeyCredential())
                           .build();
    }

    public CosmosSyncClient getCosmosSyncClient() {
        final ConnectionPolicy policy = config.getConnectionPolicy();
        final String userAgent = getUserAgentSuffix() + ";" + policy.userAgentSuffix();

        policy.userAgentSuffix(userAgent);
        return CosmosClient.builder()
                       .endpoint(config.getUri())
                       .key(config.getKey())
                       .cosmosKeyCredential(config.getCosmosKeyCredential())
                       .buildSyncClient();
    }

    private void validateConfig(@NonNull CosmosDBConfig config) {
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
