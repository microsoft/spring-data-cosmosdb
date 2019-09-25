/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.config;

import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.ConsistencyLevel;
import com.azure.data.cosmos.CosmosKeyCredential;
import com.azure.data.cosmos.internal.RequestOptions;
import com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBAccessException;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
@Builder(builderMethodName = "defaultBuilder")
public class CosmosDBConfig {
    private String uri;

    private String key;

    //  TODO: template API should be responsible to manage the global database account.
    private String database;

    private ConnectionPolicy connectionPolicy;

    private ConsistencyLevel consistencyLevel;

    private boolean allowTelemetry;

    private RequestOptions requestOptions;

    private CosmosKeyCredential cosmosKeyCredential;

    public static CosmosDBConfigBuilder builder(String uri, CosmosKeyCredential cosmosKeyCredential,
                                                  String database) {
        return defaultBuilder()
            .uri(uri)
            .cosmosKeyCredential(cosmosKeyCredential)
            .database(database)
            .connectionPolicy(ConnectionPolicy.defaultPolicy())
            .consistencyLevel(ConsistencyLevel.SESSION)
            .requestOptions(new RequestOptions());
    }

    public static CosmosDBConfigBuilder builder(String uri, String key, String database) {
        return defaultBuilder()
                .uri(uri)
                .key(key)
                .database(database)
                .connectionPolicy(ConnectionPolicy.defaultPolicy())
                .consistencyLevel(ConsistencyLevel.SESSION)
                .requestOptions(new RequestOptions());
    }

    public static CosmosDBConfigBuilder builder(String connectionString, String database) {
        Assert.hasText(connectionString, "connection string should have text!");
        try {
            final String uri = connectionString.split(";")[0].split("=")[1];
            final String key = connectionString.split(";")[1].split("=")[1];
            return builder(uri, key, database);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new CosmosDBAccessException("could not parse connection string");
        }
    }
}
