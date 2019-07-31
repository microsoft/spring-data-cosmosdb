/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.config;

import com.azure.data.cosmos.TokenResolver;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

    private TokenResolver tokenResolver;

    public static DocumentDBConfigBuilder builder(String uri, String key, String database) {
        //  Cannot call overloads with null value, so calling with empty path value.
        return builderWithTokenResolver(uri, key, database, "");
    }

    public static DocumentDBConfigBuilder builderWithTokenResolver(String uri, String key, String database,
                                                                   final TokenResolver tokenResolver) {
        return internalBuilder(uri, key, database, tokenResolver);
    }

    public static DocumentDBConfigBuilder builderWithTokenResolver(String uri, String key, String database,
                                                                   String tokenResolverClassPath) {
        final TokenResolver tokenResolver = getTokenResolverInstance(tokenResolverClassPath);
        return internalBuilder(uri, key, database, tokenResolver);
    }

    public static DocumentDBConfigBuilder builder(String connectionString, String database) {
        return builderWithTokenResolver(connectionString, database, "");
    }

    public static DocumentDBConfigBuilder builderWithTokenResolver(String connectionString, String database,
                                                                   String tokenResolverClassPath) {
        final TokenResolver tokenResolver = getTokenResolverInstance(tokenResolverClassPath);
        return builderWithTokenResolver(connectionString, database, tokenResolver);
    }

    public static DocumentDBConfigBuilder builderWithTokenResolver(String connectionString, String database,
                                                                   final TokenResolver tokenResolver) {
        Assert.hasText(connectionString, "connection string should have text!");
        try {
            final String uri = connectionString.split(";")[0].split("=")[1];
            final String key = connectionString.split(";")[1].split("=")[1];
            return builderWithTokenResolver(uri, key, database, tokenResolver);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DocumentDBAccessException("could not parse connection string");
        }
    }

    private static DocumentDBConfigBuilder internalBuilder(String uri, String key, String database,
                                                           final TokenResolver tokenResolver) {
        return defaultBuilder()
            .uri(uri)
            .key(key)
            .database(database)
            .tokenResolver(tokenResolver)
            .connectionPolicy(ConnectionPolicy.GetDefault())
            .consistencyLevel(ConsistencyLevel.Session)
            .requestOptions(new RequestOptions());
    }

    private static TokenResolver getTokenResolverInstance(String tokenResolverClassPath) {
        try {
            if (StringUtils.isEmpty(tokenResolverClassPath)) {
                return null;
            }
            return (TokenResolver) Class.forName(tokenResolverClassPath).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException("token resolver class with path {"
                + tokenResolverClassPath + "} does not exist");
        }
    }
}
