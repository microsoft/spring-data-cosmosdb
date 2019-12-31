/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.AccessCondition;
import com.azure.data.cosmos.AccessConditionType;
import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.CosmosContainerResponse;
import com.azure.data.cosmos.CosmosItemProperties;
import com.azure.data.cosmos.CosmosItemRequestOptions;
import com.azure.data.cosmos.CosmosItemResponse;
import com.azure.data.cosmos.FeedOptions;
import com.azure.data.cosmos.FeedResponse;
import com.azure.data.cosmos.PartitionKey;
import com.azure.data.cosmos.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.Memoizer;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.CountQueryGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CosmosPageImpl;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CosmosPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.common.CosmosdbUtils.fillAndProcessResponseDiagnostics;
import static com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBExceptionUtils.exceptionHandler;
import static com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBExceptionUtils.findAPIExceptionHandler;

@Slf4j
public class CosmosTemplate implements CosmosOperations, ApplicationContextAware {

    private static final String COUNT_VALUE_KEY = "_aggregate";

    private final MappingCosmosConverter mappingCosmosConverter;
    private final String databaseName;
    private final ResponseDiagnosticsProcessor responseDiagnosticsProcessor;
    private final boolean isPopulateQueryMetrics;

    private final CosmosClient cosmosClient;
    private Function<Class<?>, CosmosEntityInformation<?, ?>> entityInfoCreator =
            Memoizer.memoize(this::getCosmosEntityInformation);

    public CosmosTemplate(CosmosDbFactory cosmosDbFactory,
                          MappingCosmosConverter mappingCosmosConverter,
                          String dbName) {
        Assert.notNull(cosmosDbFactory, "CosmosDbFactory must not be null!");
        Assert.notNull(mappingCosmosConverter, "MappingCosmosConverter must not be null!");

        this.mappingCosmosConverter = mappingCosmosConverter;

        this.databaseName = dbName;
        this.cosmosClient = cosmosDbFactory.getCosmosClient();
        this.responseDiagnosticsProcessor = cosmosDbFactory.getConfig().getResponseDiagnosticsProcessor();
        this.isPopulateQueryMetrics = cosmosDbFactory.getConfig().isPopulateQueryMetrics();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    public <T> T insert(T objectToSave, PartitionKey partitionKey) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getCollectionName(objectToSave.getClass()), objectToSave, partitionKey);
    }

    public <T> T insert(String collectionName, T objectToSave, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(objectToSave, "objectToSave should not be null");

        final CosmosItemProperties originalItem = mappingCosmosConverter.writeCosmosItemProperties(objectToSave);

        log.debug("execute createItem in database {} collection {}", this.databaseName, collectionName);

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        options.partitionKey(partitionKey);

        @SuppressWarnings("unchecked")
        final Class<T> domainClass = (Class<T>) objectToSave.getClass();

        final CosmosItemResponse response = cosmosClient
            .getDatabase(this.databaseName)
            .getContainer(collectionName)
            .createItem(originalItem, options)
            .doOnNext(cosmosItemResponse -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                cosmosItemResponse, null))
            .onErrorResume(throwable ->
                exceptionHandler("Failed to insert item", throwable))
            .block();

        assert response != null;
        return mappingCosmosConverter.read(domainClass, response.properties());
    }

    public <T> T findById(Object id, Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findById(getCollectionName(entityClass), id, entityClass);
    }

    @Override
    public <T> T findById(Object id, Class<T> entityClass, PartitionKey partitionKey) {
        Assert.notNull(entityClass, "entityClass should not be null");
        Assert.notNull(partitionKey, "partitionKey should not be null");
        assertValidId(id);

        final String collectionName = getCollectionName(entityClass);
        return cosmosClient
            .getDatabase(databaseName)
            .getContainer(collectionName)
            .getItem(id.toString(), partitionKey)
            .read()
            .flatMap(cosmosItemResponse -> {
                fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                    cosmosItemResponse, null);
                return Mono.justOrEmpty(toDomainObject(entityClass,
                    cosmosItemResponse.properties()));
            })
            .onErrorResume(throwable ->
                findAPIExceptionHandler("Failed to find item", throwable))
            .block();
    }

    public <T> T findById(String collectionName, Object id, Class<T> domainClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domainClass, "entityClass should not be null");
        assertValidId(id);

        final String query = String.format("select * from root where root.id = '%s'", id.toString());
        final FeedOptions options = new FeedOptions();
        options.enableCrossPartitionQuery(true);
        options.populateQueryMetrics(isPopulateQueryMetrics);
        return cosmosClient
            .getDatabase(databaseName)
            .getContainer(collectionName)
            .queryItems(query, options)
            .flatMap(cosmosItemFeedResponse -> {
                fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                    null, cosmosItemFeedResponse);
                return Mono.justOrEmpty(cosmosItemFeedResponse
                    .results()
                    .stream()
                    .map(cosmosItem -> mappingCosmosConverter.read(domainClass, cosmosItem))
                    .findFirst());
            })
            .onErrorResume(throwable ->
                findAPIExceptionHandler("Failed to find item", throwable))
            .blockFirst();
    }

    public <T> void upsert(T object, PartitionKey partitionKey) {
        Assert.notNull(object, "Upsert object should not be null");

        upsert(getCollectionName(object.getClass()), object, partitionKey);
    }

    public <T> void upsert(String collectionName, T object, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(object, "Upsert object should not be null");

        final CosmosItemProperties originalItem = mappingCosmosConverter.writeCosmosItemProperties(object);

        log.debug("execute upsert item in database {} collection {}", this.databaseName, collectionName);

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        options.partitionKey(partitionKey);
        applyVersioning(object.getClass(), originalItem, options);

        final CosmosItemResponse cosmosItemResponse = cosmosClient
            .getDatabase(this.databaseName)
            .getContainer(collectionName)
            .upsertItem(originalItem, options)
            .doOnNext(response -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                response, null))
            .onErrorResume(throwable -> exceptionHandler("Failed to upsert item", throwable))
            .block();

        assert cosmosItemResponse != null;
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findAll(getCollectionName(entityClass), entityClass);
    }

    public <T> List<T> findAll(String collectionName, final Class<T> domainClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(domainClass, "entityClass should not be null");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        final List<CosmosItemProperties> items = findItems(query, domainClass, collectionName);
        return items.stream()
                .map(d -> getConverter().read(domainClass, d))
                .collect(Collectors.toList());
    }

    public void deleteAll(@NonNull String collectionName, @NonNull Class<?> domainClass) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        this.delete(query, domainClass, collectionName);
    }

    @Override
    public void deleteCollection(@NonNull String collectionName) {
        Assert.hasText(collectionName, "collectionName should have text.");
        cosmosClient.getDatabase(this.databaseName)
                    .getContainer(collectionName)
                    .delete()
                    .doOnNext(response -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                        response, null))
                    .onErrorResume(throwable ->
                        exceptionHandler("Failed to delete collection",  throwable))
                    .block();
    }

    public String getCollectionName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return entityInfoCreator.apply(domainClass).getCollectionName();
    }

    @Override
    public CosmosContainerProperties createCollectionIfNotExists(@NonNull CosmosEntityInformation<?, ?> information) {
        final CosmosContainerResponse response = cosmosClient
                .createDatabaseIfNotExists(this.databaseName)
                .onErrorResume(throwable ->
                    exceptionHandler("Failed to create database", throwable))
                .flatMap(cosmosDatabaseResponse -> {
                    fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                        cosmosDatabaseResponse, null);
                    return cosmosDatabaseResponse
                        .database()
                        .createContainerIfNotExists(information.getCollectionName(),
                            "/" + information.getPartitionKeyFieldName(), information.getRequestUnit())
                        .onErrorResume(throwable ->
                            exceptionHandler("Failed to create container", throwable))
                        .doOnNext(cosmosContainerResponse ->
                            fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                cosmosContainerResponse, null));
                })
                .block();
        assert response != null;
        return response.properties();
    }

    public void deleteById(String collectionName, Object id, PartitionKey partitionKey) {
        Assert.hasText(collectionName, "collectionName should not be null, empty or only whitespaces");
        assertValidId(id);

        log.debug("execute deleteById in database {} container {}", this.databaseName, collectionName);

        if (partitionKey == null) {
            partitionKey = PartitionKey.None;
        }
        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        options.partitionKey(partitionKey);
        cosmosClient.getDatabase(this.databaseName)
                    .getContainer(collectionName)
                    .getItem(id.toString(), partitionKey)
                    .delete(options)
                    .doOnNext(response -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                        response, null))
                    .onErrorResume(throwable ->
                        exceptionHandler("Failed to delete item", throwable))
                    .block();
    }

    @Override
    public <T, ID> List<T> findByIds(Iterable<ID> ids, Class<T> entityClass, String collectionName) {
        Assert.notNull(ids, "Id list should not be null");
        Assert.notNull(entityClass, "entityClass should not be null.");
        Assert.hasText(collectionName, "collection should not be null, empty or only whitespaces");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.IN, "id",
                Collections.singletonList(ids)));
        return find(query, entityClass, collectionName);
    }

    public <T> List<T> find(@NonNull DocumentQuery query, @NonNull Class<T> domainClass, String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "container should not be null, empty or only whitespaces");

        return findItems(query, domainClass, collectionName)
            .stream()
            .map(cosmosItemProperties -> toDomainObject(domainClass, cosmosItemProperties))
            .collect(Collectors.toList());
    }

    public <T> Boolean exists(@NonNull DocumentQuery query, @NonNull Class<T> domainClass, String collectionName) {
        return this.find(query, domainClass, collectionName).size() > 0;
    }

    /**
     * Delete the DocumentQuery, need to query the domains at first, then delete the item
     * from the result.
     * The cosmosdb Sql API do _NOT_ support DELETE query, we cannot add one DeleteQueryGenerator.
     *
     * @param query          The representation for query method.
     * @param domainClass    Class of domain
     * @param collectionName Container name of database
     * @param <T>
     * @return All the deleted items as List.
     */
    @Override
    public <T> List<T> delete(@NonNull DocumentQuery query, @NonNull Class<T> domainClass,
            @NonNull String collectionName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(domainClass, "domainClass should not be null.");
        Assert.hasText(collectionName, "container should not be null, empty or only whitespaces");

        final List<CosmosItemProperties> results = findItems(query, domainClass, collectionName);
        final List<String> partitionKeyName = getPartitionKeyNames(domainClass);

        return results.stream().map(cosmosItemProperties -> {
            final CosmosItemResponse cosmosItemResponse = deleteItem(cosmosItemProperties,
                partitionKeyName, collectionName, domainClass);
            return getConverter().read(domainClass, cosmosItemResponse.properties());
        }).collect(Collectors.toList());
    }

    @Override
    public <T> Page<T> findAll(Pageable pageable, Class<T> domainClass, String collectionName) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL)).with(pageable);
        if (pageable.getSort().isSorted()) {
            query.with(pageable.getSort());
        }

        return paginationQuery(query, domainClass, collectionName);
    }

    @Override
    public <T> Page<T> paginationQuery(DocumentQuery query, Class<T> domainClass, String collectionName) {
        Assert.isTrue(query.getPageable().getPageSize() > 0, "pageable should have page size larger than 0");
        Assert.hasText(collectionName, "container should not be null, empty or only whitespaces");

        final Pageable pageable = query.getPageable();
        final FeedOptions feedOptions = new FeedOptions();
        if (pageable instanceof CosmosPageRequest) {
            feedOptions.requestContinuation(((CosmosPageRequest) pageable).getRequestContinuation());
        }

        feedOptions.maxItemCount(pageable.getPageSize());
        feedOptions.enableCrossPartitionQuery(query.isCrossPartitionQuery(getPartitionKeyNames(domainClass)));
        feedOptions.populateQueryMetrics(isPopulateQueryMetrics);

        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generateCosmos(query);
        final FeedResponse<CosmosItemProperties> feedResponse = cosmosClient
            .getDatabase(this.databaseName)
            .getContainer(collectionName)
            .queryItems(sqlQuerySpec, feedOptions)
            .doOnNext(propertiesFeedResponse -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                null, propertiesFeedResponse))
            .onErrorResume(throwable ->
                exceptionHandler("Failed to query items", throwable))
            .next()
            .block();

        assert feedResponse != null;
        final Iterator<CosmosItemProperties> it = feedResponse.results().iterator();

        final List<T> result = new ArrayList<>();
        for (int index = 0; it.hasNext() && index < pageable.getPageSize(); index++) {

            final CosmosItemProperties cosmosItemProperties = it.next();
            if (cosmosItemProperties == null) {
                continue;
            }

            final T entity = mappingCosmosConverter.read(domainClass, cosmosItemProperties);
            result.add(entity);
        }

        final long total = count(query, domainClass, collectionName);
        final int contentSize = result.size();

        int pageSize;

        if (contentSize < pageable.getPageSize() && contentSize > 0) {
            //  If the content size is less than page size,
            //  this means, cosmosDB is returning less items than page size,
            //  because of either RU limit, or payload limit

            //  Set the page size to content size.
            pageSize = contentSize;
        } else {
            pageSize = pageable.getPageSize();
        }

        final CosmosPageRequest pageRequest = CosmosPageRequest.of(pageable.getOffset(),
            pageable.getPageNumber(),
            pageSize,
            feedResponse.continuationToken(),
            query.getSort());

        return new CosmosPageImpl<>(result, pageRequest, total);
    }

    @Override
    public long count(String collectionName) {
        Assert.hasText(collectionName, "container name should not be empty");

        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        final Long count = getCountValue(query, true, collectionName);
        assert count != null;
        return count;
    }

    @Override
    public <T> long count(DocumentQuery query, Class<T> domainClass, String collectionName) {
        Assert.notNull(domainClass, "domainClass should not be null");
        Assert.hasText(collectionName, "container name should not be empty");

        final boolean isCrossPartitionQuery =
                query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        final Long count = getCountValue(query, isCrossPartitionQuery, collectionName);
        assert count != null;
        return count;
    }

    @Override
    public MappingCosmosConverter getConverter() {
        return this.mappingCosmosConverter;
    }

    private Long getCountValue(DocumentQuery query, boolean isCrossPartitionQuery, String containerName) {
        final SqlQuerySpec querySpec = new CountQueryGenerator().generateCosmos(query);
        final FeedOptions options = new FeedOptions();

        options.enableCrossPartitionQuery(isCrossPartitionQuery);
        options.populateQueryMetrics(isPopulateQueryMetrics);

        return executeQuery(querySpec, containerName, options)
                .onErrorResume(throwable ->
                    exceptionHandler("Failed to get count value", throwable))
                .doOnNext(response -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                    null, response))
                .next()
                .map(r -> r.results().get(0).getLong(COUNT_VALUE_KEY))
                .block();
    }

    private Flux<FeedResponse<CosmosItemProperties>> executeQuery(SqlQuerySpec sqlQuerySpec, String collectionName,
            FeedOptions options) {
        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(collectionName)
                           .queryItems(sqlQuerySpec, options)
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to execute query", throwable));
    }

    private List<String> getPartitionKeyNames(Class<?> domainClass) {
        final CosmosEntityInformation<?, ?> entityInfo = entityInfoCreator.apply(domainClass);

        if (entityInfo.getPartitionKeyFieldName() == null) {
            return new ArrayList<>();
        }

        return Collections.singletonList(entityInfo.getPartitionKeyFieldName());
    }

    private void assertValidId(Object id) {
        Assert.notNull(id, "id should not be null");
        if (id instanceof String) {
            Assert.hasText(id.toString(), "id should not be empty or only whitespaces.");
        }
    }

    private List<CosmosItemProperties> findItems(@NonNull DocumentQuery query,
                                                 @NonNull Class<?> domainClass,
                                                 @NonNull String containerName) {
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generateCosmos(query);
        final boolean isCrossPartitionQuery =
                query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.enableCrossPartitionQuery(isCrossPartitionQuery);
        feedOptions.populateQueryMetrics(isPopulateQueryMetrics);

        return cosmosClient
                .getDatabase(this.databaseName)
                .getContainer(containerName)
                .queryItems(sqlQuerySpec, feedOptions)
                .flatMap(cosmosItemFeedResponse -> {
                    fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                        null, cosmosItemFeedResponse);
                    return Flux.fromIterable(cosmosItemFeedResponse.results());
                })
                .onErrorResume(throwable ->
                    exceptionHandler("Failed to find items", throwable))
                .collectList()
                .block();
    }

    private CosmosItemResponse deleteItem(@NonNull CosmosItemProperties cosmosItemProperties,
                                          @NonNull List<String> partitionKeyNames,
                                          String containerName,
                                          @NonNull Class<?> domainClass) {
        Assert.isTrue(partitionKeyNames.size() <= 1, "Only one Partition is supported.");

        PartitionKey partitionKey = null;

        if (!partitionKeyNames.isEmpty() && StringUtils.hasText(partitionKeyNames.get(0))) {
            partitionKey = new PartitionKey(cosmosItemProperties.get(partitionKeyNames.get(0)));
        }

        if (partitionKey == null) {
            partitionKey = PartitionKey.None;
        }

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions(partitionKey);
        applyVersioning(domainClass, cosmosItemProperties, options);

        return cosmosClient
            .getDatabase(this.databaseName)
            .getContainer(containerName)
            .getItem(cosmosItemProperties.id(), partitionKey)
            .delete(options)
            .doOnNext(response -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                response, null))
            .onErrorResume(throwable ->
                exceptionHandler("Failed to delete item", throwable))
            .block();
    }

    private <T> T toDomainObject(@NonNull Class<T> domainClass, CosmosItemProperties cosmosItemProperties) {
        return mappingCosmosConverter.read(domainClass, cosmosItemProperties);
    }

    private void applyVersioning(Class<?> domainClass,
            CosmosItemProperties cosmosItemProperties,
            CosmosItemRequestOptions options) {

        if (entityInfoCreator.apply(domainClass).isVersioned()) {
            final AccessCondition accessCondition = new AccessCondition();
            accessCondition.type(AccessConditionType.IF_MATCH);
            accessCondition.condition(cosmosItemProperties.etag());
            options.accessCondition(accessCondition);
        }
    }

    private CosmosEntityInformation<?, ?> getCosmosEntityInformation(Class<?> domainClass) {
        return new CosmosEntityInformation<>(domainClass);
    }

}
