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
import com.microsoft.azure.spring.data.cosmosdb.common.TelemetryEventTracker;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import lombok.NonNull;
import org.springframework.util.Assert;

public class DocumentDbFactory {

    private final DocumentDBConfig config;

    private final TelemetryEventTracker telemetryEventTracker;

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
        this.telemetryEventTracker = new TelemetryEventTracker(IS_TELEMETRY_ALLOWED);
        this.telemetryEventTracker.trackEvent(this.getClass().getSimpleName());
    }

    public DocumentClient getDocumentClient() {
        final ConnectionPolicy policy = config.getConnectionPolicy();
        final String userAgent = getUserAgentSuffix() + ";" + policy.getUserAgentSuffix();

        policy.setUserAgentSuffix(userAgent);

        return new DocumentClient(config.getUri(), config.getKey(), policy, config.getConsistencyLevel());
    }

    private void validateConfig(@NonNull DocumentDBConfig config) {
        Assert.hasText(config.getUri(), "cosmosdb host url should have text!");
        Assert.hasText(config.getKey(), "cosmosdb host key should have text!");
        Assert.hasText(config.getDatabase(), "cosmosdb database should have text!");
        Assert.notNull(config.getConnectionPolicy(), "cosmosdb connection policy should not be null!");
    }
}
