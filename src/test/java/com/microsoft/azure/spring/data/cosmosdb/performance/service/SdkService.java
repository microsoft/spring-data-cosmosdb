/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.performance.service;

import com.google.gson.Gson;
import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.spring.data.cosmosdb.performance.domain.PerfPerson;
import com.microsoft.azure.spring.data.cosmosdb.performance.utils.DatabaseUtils;
import org.assertj.core.util.Lists;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.performance.utils.Constants.PERF_DATABASE_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.performance.utils.Constants.SDK_COLLECTION_NAME;

public class SdkService {
    private static Gson gson = new Gson();

    private final DocumentClient documentClient;
    private final String dbName;
    private final String collectionName;
    private final String collectionLink;

    public SdkService(DocumentClient client, String dbName, String collectionName) {
        this.documentClient = client;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.collectionLink = "dbs/" + PERF_DATABASE_NAME + "/colls/" + SDK_COLLECTION_NAME;
    }

    public PerfPerson save(PerfPerson person) {
        try {
            final String personJson = gson.toJson(person);
            final Document personDoc = new Document(personJson);

            final Document doc = documentClient.createDocument(collectionLink, personDoc,
                    null, false).getResource();

            return gson.fromJson(doc.toJson(), PerfPerson.class);
        } catch (DocumentClientException e) {
            throw new IllegalStateException(e); // Runtime exception to fail directly
        }
    }

    public List<PerfPerson> saveAll(Iterable<PerfPerson> personIterable) {
        final List<PerfPerson> result = new ArrayList<>();
        personIterable.forEach(person -> result.add(save(person)));

        return result;
    }

    public void delete(PerfPerson person) {
        try {
            final String docLink = DatabaseUtils.getDocumentLink(dbName, collectionName, person.getId());

            documentClient.deleteDocument(docLink, null);
        } catch (DocumentClientException e) {
            throw new IllegalStateException(e); // Runtime exception to fail directly
        }
    }

    public void deleteAll(Iterable<PerfPerson> personIterable) {
        personIterable.forEach(person -> delete(person));
    }

    public Document findById(String id) {
        return documentClient.queryDocuments(collectionLink, "SELECT * FROM " + collectionName + " WHERE " +
                collectionName + ".id='" + id + "'", new FeedOptions()).getQueryIterator().next();
    }

    public List<PerfPerson> findAllById(Iterable<String> ids) {
        final String idsInList = String.join(",",
                Arrays.asList(ids).stream().map(id -> "'" + id + "'").collect(Collectors.toList()));
        final String sql = "SELECT * FROM " + collectionName + " WHERE " + collectionName + ".id IN ("
                + idsInList + ")";

        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.setEnableCrossPartitionQuery(true);

        final List<Document> docs = documentClient.queryDocuments(collectionLink, sql, feedOptions)
                .getQueryIterable().toList();

        return fromDocuments(docs);
    }

    public List<PerfPerson> findAll() {
        final List<Document> docs = documentClient.queryDocuments(collectionLink,
                "SELECT * FROM  " + collectionName, new FeedOptions()).getQueryIterable().toList();

        return fromDocuments(docs);
    }

    public boolean deleteAll() {
        final List<Document> documents = documentClient.queryDocuments(collectionLink,
                "SELECT * FROM  " + collectionName, new FeedOptions()).getQueryIterable().toList();

        documents.forEach(document -> {
            try {
                documentClient.deleteDocument(document.getSelfLink(), null);
            } catch (DocumentClientException e) {
                throw new IllegalStateException(e);
            }
        });

        return true;
    }

    public List<PerfPerson> searchDocuments(Sort sort) {
        final Sort.Order order = sort.iterator().next(); // Only one Order supported
        final List<Document> docs = documentClient.queryDocuments(collectionLink,
                "SELECT * FROM  " + collectionName + " ORDER BY " + collectionName + "." + order.getProperty()
                        + " " + order.getDirection().name(), new FeedOptions()).getQueryIterable().toList();

        return fromDocuments(docs);
    }

    public long count() {
        final Object result = documentClient.queryDocuments(collectionLink,
                "SELECT VALUE COUNT(1) FROM " + collectionName, new FeedOptions())
                .getQueryIterable().toList().get(0).getHashMap().get("_aggregate");

        return result instanceof Integer ? Long.valueOf((Integer) result) : (Long) result;
    }

    public List<PerfPerson> findByName(String name) {
        final Iterator<Document> result = documentClient.queryDocuments(collectionLink,
                "SELECT * FROM " + collectionName + " WHERE " + collectionName + ".name='"
                        + name + "'", new FeedOptions()).getQueryIterator();

        return fromDocuments(Lists.newArrayList(result));
    }

    public void queryTwoPages(int pageSize) {
        final FeedOptions options = new FeedOptions();
        options.setPageSize(pageSize);
        options.setRequestContinuation(null);

        searchBySize(pageSize, options);
        searchBySize(pageSize, options);
    }

    private List<PerfPerson> searchBySize(int size, FeedOptions options) {
        final FeedResponse<Document> q = documentClient
                .queryDocuments(collectionLink, "SELECT * FROM " + collectionName, options);

        final Iterator<Document> it = q.getQueryIterator();
        final List<PerfPerson> entities = new ArrayList<>();
        int i = 0;
        while (it.hasNext() && i++ < size) {
            // This convert here is in order to mock data conversion in real use case, in order to compare with
            // Spring Data mapping
            final Document d = it.next();
            final PerfPerson entity = gson.fromJson(d.toJson(), PerfPerson.class);
            entities.add(entity);
        }

        options.setRequestContinuation(q.getResponseContinuation());

        count(); // Mock same behavior with Spring pageable query, requires total elements count

        return entities;
    }

    private List<PerfPerson> fromDocuments(List<Document> documents) {
        return documents.stream().map(d -> gson.fromJson(d.toJson(), PerfPerson.class))
                .collect(Collectors.toList());
    }
}
