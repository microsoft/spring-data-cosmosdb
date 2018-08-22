/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.config;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import lombok.Getter;

public class DocumentDBConfig {
    @Getter
    private String uri;

    @Getter
    private String key;

    @Getter
    private String database;

    @Getter
    private ConnectionPolicy connectionPolicy;

    @Getter
    private ConsistencyLevel consistencyLevel;

    @Getter
    private boolean allowTelemetry;

    public static class Builder {
        private final String uri;
        private final String key;
        private final String database;

        private ConnectionPolicy connectionPolicy = ConnectionPolicy.GetDefault();
        private ConsistencyLevel consistencyLevel = ConsistencyLevel.Session;
        private boolean allowTelemetry = true;

        public Builder(String uri, String key, String database) {
            this.uri = uri;
            this.key = key;
            this.database = database;
        }

        public Builder connectionPolicy(ConnectionPolicy policy) {
            this.connectionPolicy = policy;
            return this;
        }

        public Builder consistencyLevel(ConsistencyLevel level) {
            this.consistencyLevel = level;
            return this;
        }

        public Builder allowTelemetry(boolean allowTelemetry) {
            this.allowTelemetry = allowTelemetry;
            return this;
        }

        public DocumentDBConfig build() {
            return new DocumentDBConfig(this);
        }
    }

    private DocumentDBConfig(Builder builder) {
        this.uri = builder.uri;
        this.key = builder.key;
        this.database = builder.database;
        this.connectionPolicy = builder.connectionPolicy;
        this.consistencyLevel = builder.consistencyLevel;
        this.allowTelemetry = builder.allowTelemetry;
    }
}
