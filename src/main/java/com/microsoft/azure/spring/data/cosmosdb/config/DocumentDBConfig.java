/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.config;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderMethodName = "defaultBuilder")
public class DocumentDBConfig {
    private String uri;

    private String key;

    private String database;

    private ConnectionPolicy connectionPolicy;

    private ConsistencyLevel consistencyLevel;

    private boolean allowTelemetry;

    public static DocumentDBConfigBuilder builder(String uri, String key, String database) {
        return defaultBuilder()
                .uri(uri)
                .key(key)
                .database(database)
                .connectionPolicy(ConnectionPolicy.GetDefault())
                .consistencyLevel(ConsistencyLevel.Session);
    }
}
