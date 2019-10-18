/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link AbstractDocumentDbConfiguration} is deprecated.
 * Instead use AbstractCosmosConfiguration, which is introduced in 2.2.0 version.
 */
@Deprecated
@Configuration
public abstract class AbstractDocumentDbConfiguration extends DocumentDbConfigurationSupport {
    @Bean
    public DocumentClient documentClient(DocumentDBConfig config) {
        return this.documentDbFactory(config).getDocumentClient();
    }

    @Qualifier(Constants.OBJECTMAPPER_BEAN_NAME)
    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Bean
    public DocumentDbFactory documentDbFactory(DocumentDBConfig config) {
        return new DocumentDbFactory(config);
    }

    @Bean
    public CosmosDbFactory cosmosDbFactory(DocumentDBConfig config) {
        return new CosmosDbFactory(config);
    }

    @Bean
    public DocumentDbTemplate documentDbTemplate(DocumentDBConfig config) throws ClassNotFoundException {
        return new DocumentDbTemplate(this.cosmosDbFactory(config), this.mappingDocumentDbConverter(),
                config.getDatabase());
    }

    @Bean
    public ReactiveCosmosTemplate cosmosDbTemplate(DocumentDBConfig config) throws ClassNotFoundException {
        return new ReactiveCosmosTemplate(this.cosmosDbFactory(config), this.mappingDocumentDbConverter(),
            config.getDatabase());
    }

    @Bean
    public MappingDocumentDbConverter mappingDocumentDbConverter() throws ClassNotFoundException {
        return new MappingDocumentDbConverter(this.documentDbMappingContext(), objectMapper);
    }
}
