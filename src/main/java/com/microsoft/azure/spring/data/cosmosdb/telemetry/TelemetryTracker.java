/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.spring.data.cosmosdb.telemetry.TelemetryProperties.PROPERTY_INSTALLATION_ID;
import static com.microsoft.azure.spring.data.cosmosdb.telemetry.TelemetryProperties.PROPERTY_VERSION;

public class TelemetryTracker {

    private static final String PROJECT_VERSION = TelemetryTracker.class.getPackage().getImplementationVersion();

    private static final String PROJECT_INFO = "spring-data-cosmosdb" + "/" + PROJECT_VERSION;

    private final TelemetryClient client;

    private final Map<String, String> defaultProperties;

    public TelemetryTracker() {
        this.client = new TelemetryClient();
        this.defaultProperties = new HashMap<>();

        this.defaultProperties.put(PROPERTY_VERSION, PROJECT_INFO);
        this.defaultProperties.put(PROPERTY_INSTALLATION_ID, TelemetryUtils.getHashMac());
    }

    public void trackEvent(@NonNull String name) {
        this.client.trackEvent(name, this.defaultProperties, null);
        this.client.flush();
    }
}
