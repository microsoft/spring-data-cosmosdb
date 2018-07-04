/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.telemetry.TelemetryTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractDocumentDbConfiguration extends DocumentDbConfigurationSupport {

    public abstract String getDatabase();

    public abstract DocumentClient documentClient();

    @Qualifier(Constants.OBJECTMAPPER_BEAN_NAME)
    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Bean
    @ConditionalOnProperty(name = "cosmosdb.telemetryAllowed", havingValue = "true", matchIfMissing = true)
    public TelemetryTracker getTelemetryTracker() {
        return new TelemetryTracker();
    }

    @Bean
    public DocumentDbFactory documentDbFactory() {
        return new DocumentDbFactory(this.documentClient());
    }

    @Bean
    public DocumentDbTemplate documentDbTemplate() throws ClassNotFoundException {
        return new DocumentDbTemplate(this.documentDbFactory(), this.mappingDocumentDbConverter(), this.getDatabase());
    }

    @Bean
    public MappingDocumentDbConverter mappingDocumentDbConverter() throws ClassNotFoundException {
        return new MappingDocumentDbConverter(this.documentDbMappingContext(), objectMapper);
    }

    protected String getMappingBasePackage() {
        final Package mappingBasePackage = getClass().getPackage();
        return mappingBasePackage == null ? null : mappingBasePackage.getName();
    }

}
