/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.performance.utils;

import com.azure.data.cosmos.CosmosClientException;
import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.IncludedPath;
import com.azure.data.cosmos.IndexingPolicy;
import com.azure.data.cosmos.PartitionKeyDefinition;
import com.azure.data.cosmos.internal.RequestOptions;
import com.azure.data.cosmos.sync.CosmosSyncClient;

import java.util.Collections;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.ORDER_BY_STRING_PATH;

public class DatabaseUtils {
    public static void createDatabase(CosmosSyncClient documentClient, String databaseName)
            throws CosmosClientException {
        try {
            // Can use sync api once ready
            documentClient.getDatabase(databaseName).delete();
        } catch (Exception e) {
            // Ignore delete failure
        }

        documentClient.createDatabase(databaseName);
    }

    public static void deleteCollection(CosmosSyncClient documentClient, String databaseName, String collectionName)
            throws CosmosClientException{
        final RequestOptions requestOptions = new RequestOptions();
        requestOptions.setOfferThroughput(1000);

        documentClient.getDatabase(databaseName).getContainer(collectionName).delete();
    }

    public static void createCollection(CosmosSyncClient documentClient, String databaseName, String collectionName)
            throws CosmosClientException {
        final CosmosContainerProperties containerProperties = new CosmosContainerProperties(collectionName,
                new PartitionKeyDefinition().paths(Collections.singletonList("/mypk")));

        final IndexingPolicy policy = new IndexingPolicy();
        policy.setIncludedPaths(Collections.singletonList(new IncludedPath(ORDER_BY_STRING_PATH)));
        containerProperties.indexingPolicy(policy);

        documentClient.getDatabase(databaseName).createContainer(containerProperties);
    }

    public static String getDocumentLink(String databaseName, String collectionName, Object documentId) {
        return getCollectionLink(databaseName, collectionName) + "/docs/" + documentId;
    }

    public static String getDatabaseLink(String databaseName) {
        return "dbs/" + databaseName;
    }

    public static String getCollectionLink(String databaseName, String collectionName) {
        return getDatabaseLink(databaseName) + "/colls/" + collectionName;
    }
}
