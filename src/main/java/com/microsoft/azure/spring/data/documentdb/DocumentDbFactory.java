/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.documentdb.common.GetHashMac;
import com.microsoft.azure.spring.data.documentdb.common.PropertyLoader;
import com.microsoft.azure.spring.data.documentdb.common.TelemetryProperties;
import com.microsoft.azure.spring.data.documentdb.common.TelemetryProxy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

public class DocumentDbFactory {

    private DocumentClient documentClient;
    private final TelemetryProxy telemetryProxy;
    private static final String USER_AGENT_SUFFIX = Constants.USER_AGENT_SUFFIX + PropertyLoader.getProjectVersion();

    private String getUserAgentSuffix(boolean isBiEnabled) {
        String suffix = ";" + USER_AGENT_SUFFIX;

        if (isBiEnabled && GetHashMac.getHashMac() != null) {
            suffix += ";" + GetHashMac.getHashMac();
        }

        return suffix;
    }

    public DocumentDbFactory(String host, String key) {
        Assert.hasText(host, "host must not be empty!");
        Assert.hasText(key, "key must not be empty!");

        final boolean isAllowed = this.isTelemetryAllowed();
        final ConnectionPolicy policy = ConnectionPolicy.GetDefault();

        policy.setUserAgentSuffix(getUserAgentSuffix(isAllowed));

        this.documentClient = new DocumentClient(host, key, policy, ConsistencyLevel.Session);
        this.telemetryProxy = new TelemetryProxy(isAllowed);

        this.trackCustomEvent();
    }

    public DocumentDbFactory(DocumentClient client) {
        final boolean isAllowed = this.isTelemetryAllowed();

        if (client != null && client.getConnectionPolicy() != null) {
            client.getConnectionPolicy().setUserAgentSuffix(this.getUserAgentSuffix(isAllowed));
        }

        this.documentClient = client;
        this.telemetryProxy = new TelemetryProxy(this.isTelemetryAllowed());
        this.trackCustomEvent();
    }

    private boolean isTelemetryAllowed() {
        // panli: will allow user to disable this from application.properties in another PR.
        return true;
    }

    public DocumentClient getDocumentClient() {
        return documentClient;
    }

    private void trackCustomEvent() {
        final Map<String, String> customProperties = new HashMap<>();

        customProperties.put(TelemetryProperties.PROPERTY_SERVICE_NAME, "cosmosdb");

        this.telemetryProxy.trackEvent(ClassUtils.getUserClass(this.getClass()).getSimpleName(), customProperties);
    }
}

