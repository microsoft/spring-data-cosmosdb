/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.common;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class TelemetryProxy {

    private static final String PROJECT_INFO = "spring-data-cosmosdb/" + PropertyLoader.getProjectVersion();

    @Getter(AccessLevel.PRIVATE)
    private final boolean isTelemetryAllowed;

    @Getter(AccessLevel.PRIVATE)
    private final TelemetryClient client;


    public TelemetryProxy(boolean isTelemetryAllowed) {
        this.client = new TelemetryClient();
        this.isTelemetryAllowed = isTelemetryAllowed;
    }

    public void trackEvent(@NonNull String eventName) {
        if (isTelemetryAllowed()) {
            getClient().trackEvent(eventName, getDefaultProperties(), null);
            getClient().flush();
        }
    }

    private Map<String, String> getDefaultProperties() {
        final Map<String, String> properties = new HashMap<>();

        properties.put(TelemetryProperties.PROPERTY_INSTALLATION_ID, MacAddress.getHashMac());
        properties.put(TelemetryProperties.PROPERTY_VERSION, PROJECT_INFO);
        properties.put(TelemetryProperties.PROPERTY_SERVICE_NAME, "cosmosdb");

        return properties;
    }
}

