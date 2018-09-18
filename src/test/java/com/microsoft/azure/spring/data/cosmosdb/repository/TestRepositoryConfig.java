/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.AbstractDocumentDbConfiguration;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.config.EnableDocumentDbRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringUtils;

@Configuration
@PropertySource(value = {"classpath:application.properties"})
@EnableDocumentDbRepositories
public class TestRepositoryConfig extends AbstractDocumentDbConfiguration {
    @Value("${cosmosdb.uri:}")
    private String documentDbUri;

    @Value("${cosmosdb.key:}")
    private String documentDbKey;

    @Value("${cosmosdb.connection-string:}")
    private String connectionString;

    @Value("${cosmosdb.database:}")
    private String database;

    @Override
    public DocumentDBConfig getConfig() {
        final String dbName = StringUtils.hasText(this.database) ? this.database : TestConstants.DB_NAME;

        if (StringUtils.hasText(this.documentDbUri) && StringUtils.hasText(this.documentDbKey)) {
            return DocumentDBConfig.builder(documentDbUri, documentDbKey, dbName).build();
        }
        return DocumentDBConfig.builder(connectionString, dbName).build();
    }
}
