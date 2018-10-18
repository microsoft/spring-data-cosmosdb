/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.performance;

import com.microsoft.azure.spring.data.cosmosdb.config.AbstractDocumentDbConfiguration;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.performance.utils.Constants;
import com.microsoft.azure.spring.data.cosmosdb.repository.config.EnableDocumentDbRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"classpath:application.properties"})
@EnableDocumentDbRepositories
public class PerfConfiguration extends AbstractDocumentDbConfiguration {
    @Value("${cosmosdb.uri:}")
    private String documentDbUri;

    @Value("${cosmosdb.key:}")
    private String documentDbKey;

    @Override
    public DocumentDBConfig getConfig() {
        return DocumentDBConfig.builder(documentDbUri, documentDbKey, Constants.PERF_DATABASE_NAME).build();
    }
}
