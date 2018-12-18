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
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TelemetryEventTracker {

    private static final String PROPERTY_INSTALLATION_ID = "installationId";

    private static final String PROPERTY_VERSION = "version";

    private static final String PROPERTY_SERVICE_NAME = "serviceName";

    private static final String PROJECT_INFO = "spring-data-cosmosdb/" + PropertyLoader.getProjectVersion();

    @Getter(AccessLevel.PRIVATE)
    private final boolean isTelemetryAllowed;

    @Getter(AccessLevel.PRIVATE)
    private final TelemetryClient client;


    public TelemetryEventTracker(boolean isTelemetryAllowed, String instrumentationKey) {
        this.client = new TelemetryClient();
        this.isTelemetryAllowed = isTelemetryAllowed;

        if (StringUtils.hasText(instrumentationKey)) {
            this.client.getContext().setInstrumentationKey(instrumentationKey);
        }
    }

    public void trackEvent(@NonNull String eventName) {
        if (isTelemetryAllowed()) {
            getClient().trackEvent(eventName, getDefaultProperties(), null);
            getClient().flush();
        }
    }

    private Map<String, String> getDefaultProperties() {
        final Map<String, String> properties = new HashMap<>();

        properties.put(PROPERTY_VERSION, PROJECT_INFO);
        properties.put(PROPERTY_SERVICE_NAME, "cosmosdb");
        properties.put(PROPERTY_INSTALLATION_ID, MacAddress.getHashMac());

        return properties;
    }
}

