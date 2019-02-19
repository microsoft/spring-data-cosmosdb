/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.config;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
@Builder(builderMethodName = "defaultBuilder")
public class DocumentDBConfig {
    private String uri;

    private String key;

    private String database;

    private ConnectionPolicy connectionPolicy;

    private ConsistencyLevel consistencyLevel;

    private boolean allowTelemetry;

    private RequestOptions requestOptions;

    public static DocumentDBConfigBuilder builder(String uri, String key, String database) {
        return defaultBuilder()
                .uri(uri)
                .key(key)
                .database(database)
                .connectionPolicy(ConnectionPolicy.GetDefault())
                .consistencyLevel(ConsistencyLevel.Session)
                .requestOptions(new RequestOptions());
    }

    public static DocumentDBConfigBuilder builder(String connectionString, String database) {
        Assert.hasText(connectionString, "connection string should have text!");
        try {
            final String uri = connectionString.split(";")[0].split("=")[1];
            final String key = connectionString.split(";")[1].split("=")[1];
            return builder(uri, key, database);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DocumentDBAccessException("could not parse connection string");
        }
    }
}
