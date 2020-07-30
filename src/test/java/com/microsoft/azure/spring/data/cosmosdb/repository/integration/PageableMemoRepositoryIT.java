/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosItemProperties;
import com.azure.data.cosmos.FeedOptions;
import com.azure.data.cosmos.FeedResponse;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CosmosPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.domain.Importance;
import com.microsoft.azure.spring.data.cosmosdb.domain.PageableMemo;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.PageableMemoRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
@Slf4j
public class PageableMemoRepositoryIT {

    private static final int TOTAL_CONTENT_SIZE = 10000;

    private static final CosmosEntityInformation<PageableMemo, String> entityInformation =
        new CosmosEntityInformation<>(PageableMemo.class);

    private static CosmosTemplate staticTemplate;

    @Autowired
    private CosmosTemplate template;

    @Autowired
    private PageableMemoRepository repository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CosmosDBConfig dbConfig;

    private static Set<PageableMemo> memoSet;

    private static boolean isSetupDone;
    private static Date startDate = new Date();
    private static Date endDate = null;

    @Before
    public void setUp() {
        if (isSetupDone) {
            return;
        }
        template.createContainerIfNotExists(entityInformation);
        staticTemplate = template;
        memoSet = new HashSet<>();
        final Random random = new Random();
        final Importance[] importanceValues = Importance.values();
        final Date date = new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli());
        log.info("Content date is : {}", date);

        //  Create larger documents with size more than 10 kb
        for (int i = 0; i < TOTAL_CONTENT_SIZE; i++) {
            final String id = UUID.randomUUID().toString();
            final String message = "partitionKey";
            final int randomIndex = random.nextInt(3);
            final PageableMemo memo = new PageableMemo(id, message, startDate, importanceValues[randomIndex]);
            repository.save(memo);
            memoSet.add(memo);
        }
        isSetupDone = true;
    }

    @AfterClass
    public static void afterClassCleanup() {
        staticTemplate.deleteContainer(entityInformation.getContainerName());
    }

    @Test
    public void testFindAllPages() {
        final Set<PageableMemo> memos = findAllWithPageSize(100);
        final boolean equals = memos.equals(memoSet);
        assertThat(equals).isTrue();
    }

    @Test
    public void testFindAllUsingBetween() {
        final Set<PageableMemo> memos = findAllUsingBetweenWithPageSize(100);
        final boolean equals = memos.equals(memoSet);
        assertThat(equals).isTrue();
    }

    @Test
    public void testFindAllWithPageSizeLessThanReturned() {
        final Set<PageableMemo> memos = findAllWithPageSize(20);
        assertThat(memos).isEqualTo(memoSet);
    }

    @Test
    public void testFindAllWithPageSizeLessThanTotal() {
        final Set<PageableMemo> memos = findAllWithPageSize(200);
        assertThat(memos).isEqualTo(memoSet);
    }

    @Test
    public void testFindAllWithPageSizeGreaterThanTotal() {
        final Set<PageableMemo> memos = findAllWithPageSize(10000);
        assertThat(memos).isEqualTo(memoSet);
    }

    @Test
    public void testOffsetAndLimitLessThanTotal() {
        final int skipCount = 50;
        final int takeCount = 200;
        verifyItemsWithOffsetAndLimit(skipCount, takeCount, takeCount);
    }

    @Test
    public void testOffsetAndLimitEqualToTotal() {
        final int skipCount = 100;
        final int takeCount = 300;
        verifyItemsWithOffsetAndLimit(skipCount, takeCount, takeCount);
    }


    @Test
    public void testOffsetAndLimitGreaterThanTotal() {
        final int skipCount = 300;
        final int takeCount = 300;
        verifyItemsWithOffsetAndLimit(skipCount, takeCount, TOTAL_CONTENT_SIZE - skipCount);
    }

    private Flux<FeedResponse<CosmosItemProperties>> getItemsWithOffsetAndLimit(int skipCount, int takeCount) {
        final FeedOptions options = new FeedOptions();
        options.enableCrossPartitionQuery(true);
        options.maxDegreeOfParallelism(2);

        final String query = "SELECT * from c OFFSET " + skipCount + " LIMIT " + takeCount;

        final CosmosClient cosmosClient = applicationContext.getBean(CosmosClient.class);
        return cosmosClient.getDatabase(dbConfig.getDatabase())
                    .getContainer(entityInformation.getContainerName())
                    .queryItems(query, options);
    }

    private void verifyItemsWithOffsetAndLimit(int skipCount, int takeCount, int verifyCount) {
        final List<CosmosItemProperties> itemsWithOffsetAndLimit = new ArrayList<>();
        final Flux<FeedResponse<CosmosItemProperties>> itemsWithOffsetAndLimitFlux =
            getItemsWithOffsetAndLimit(skipCount, takeCount);
        StepVerifier.create(itemsWithOffsetAndLimitFlux)
                    .thenConsumeWhile(cosmosItemPropertiesFeedResponse -> {
                        itemsWithOffsetAndLimit.addAll(cosmosItemPropertiesFeedResponse.results());
                        return true;
                    })
                    .verifyComplete();
        assertThat(itemsWithOffsetAndLimit.size()).isEqualTo(verifyCount);
    }

    private Set<PageableMemo> findAllWithPageSize(int pageSize) {
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, pageSize, null);
        Page<PageableMemo> page = repository.findAll(pageRequest);
        List<PageableMemo> content = page.getContent();
        log.info("Total elements are : {}", page.getTotalElements());
        log.info("Content size is : {}", content.size());
        final Set<PageableMemo> outputSet = new HashSet<>(content);
        while (page.hasNext()) {
            final Pageable pageable = page.nextPageable();
            log.info("Offset is  : {}", pageable.getOffset());
            page = repository.findAll(pageable);
            content = page.getContent();
            log.info("Content size is : {}", content.size());
            outputSet.addAll(content);
        }
        return outputSet;
    }

    private Set<PageableMemo> findAllUsingBetweenWithPageSize(int pageSize) {
        endDate = new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        log.info("Start Date is : {}", startDate);
        log.info("End Date is : {}", endDate);
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, pageSize, null);
        Date date = new Date();
        Page<PageableMemo> page = repository.findByMessageAndDateBetween("partitionKey", startDate, endDate, pageRequest);
        List<PageableMemo> content = page.getContent();
        log.info("Total elements are : {}", page.getTotalElements());
        log.info("Content size is : {}", content.size());
        final Set<PageableMemo> outputSet = new HashSet<>(content);
        while (page.hasNext()) {
            final Pageable pageable = page.nextPageable();
            log.info("Offset is  : {}", pageable.getOffset());
            page = repository.findByMessageAndDateBetween("partitionKey", startDate, endDate, pageable);
            content = page.getContent();
            log.info("Content size is : {}", content.size());
            outputSet.addAll(content);
        }
        return outputSet;
    }
}
