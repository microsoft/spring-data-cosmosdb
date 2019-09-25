/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.config;

import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.sync.CosmosSyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractCosmosConfiguration extends CosmosConfigurationSupport {

    @Bean
    public CosmosClient cosmosClient(CosmosDBConfig config) {
        return this.cosmosDbFactory(config).getCosmosClient();
    }

    @Bean
    public CosmosSyncClient cosmosSyncClient(CosmosDBConfig config) {
        return this.cosmosDbFactory(config).getCosmosSyncClient();
    }
    
    @Qualifier(Constants.OBJECTMAPPER_BEAN_NAME)
    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Bean
    public CosmosDbFactory cosmosDbFactory(CosmosDBConfig config) {
        return new CosmosDbFactory(config);
    }

    @Bean
    public CosmosTemplate cosmosTemplate(CosmosDBConfig config) throws ClassNotFoundException {
        return new CosmosTemplate(this.cosmosDbFactory(config), this.mappingDocumentDbConverter(),
                config.getDatabase());
    }

    @Bean
    public ReactiveCosmosTemplate reactiveCosmosTemplate(CosmosDBConfig config) throws ClassNotFoundException {
        return new ReactiveCosmosTemplate(this.cosmosDbFactory(config), this.mappingDocumentDbConverter(),
            config.getDatabase());
    }

    @Bean
    public MappingCosmosConverter mappingDocumentDbConverter() throws ClassNotFoundException {
        return new MappingCosmosConverter(this.documentDbMappingContext(), objectMapper);
    }
}
