/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.config;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationInsightConfiguration {

    @Bean
    public TelemetryConfiguration telemetryConfiguration() {
        return TelemetryConfiguration.getActive();
    }
}
