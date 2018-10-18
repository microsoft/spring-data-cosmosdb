/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.performance.utils;

import com.microsoft.azure.documentdb.*;

import java.util.Collections;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.ORDER_BY_STRING_PATH;

public class DatabaseUtils {
    public static void createDatabase(DocumentClient documentClient, String databaseName)
            throws DocumentClientException {
        try {
            documentClient.deleteDatabase("dbs/" + databaseName, null);
        } catch (DocumentClientException e) {
            // Ignore delete failure
        }

        final Database myDatabase = new Database();
        myDatabase.setId(databaseName);

        documentClient.createDatabase(myDatabase, null);
    }

    public static void deleteCollection(DocumentClient documentClient, String databaseName, String collectionName)
            throws DocumentClientException{
        final RequestOptions requestOptions = new RequestOptions();
        requestOptions.setOfferThroughput(1000);

        documentClient.deleteCollection("dbs/" + databaseName + "/colls/" + collectionName, requestOptions);
    }

    public static void createCollection(DocumentClient documentClient, String databaseName, String collectionName)
            throws DocumentClientException {
        final DocumentCollection myCollection = new DocumentCollection();
        myCollection.setId(collectionName);

        final IndexingPolicy policy = new IndexingPolicy();
        policy.setIncludedPaths(Collections.singletonList(new IncludedPath(ORDER_BY_STRING_PATH)));
        myCollection.setIndexingPolicy(policy);

        documentClient.createCollection("dbs/" + databaseName, myCollection, null);
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
