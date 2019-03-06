/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.PageableAddressRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateNonLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.PAGE_SIZE_1;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.PAGE_SIZE_3;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class PageableAddressRepositoryIT {
    private static final Address TEST_ADDRESS1_PARTITION1 = new Address(
            TestConstants.POSTAL_CODE, TestConstants.STREET, TestConstants.CITY);
    private static final Address TEST_ADDRESS2_PARTITION1 = new Address(
            TestConstants.POSTAL_CODE_0, TestConstants.STREET, TestConstants.CITY);
    private static final Address TEST_ADDRESS1_PARTITION2 = new Address(
            TestConstants.POSTAL_CODE_1, TestConstants.STREET_0, TestConstants.CITY_0);
    private static final Address TEST_ADDRESS4_PARTITION3 = new Address(
            TestConstants.POSTAL_CODE, TestConstants.STREET_1, TestConstants.CITY_1);

    private final DocumentDbEntityInformation<Address, String> entityInformation =
            new DocumentDbEntityInformation<>(Address.class);

    @Autowired
    private DocumentDbTemplate template;

    @Autowired
    private PageableAddressRepository repository;

    @Before
    public void setup() {
        repository.save(TEST_ADDRESS1_PARTITION1);
        repository.save(TEST_ADDRESS1_PARTITION2);
        repository.save(TEST_ADDRESS2_PARTITION1);
        repository.save(TEST_ADDRESS4_PARTITION3);
    }

    @PreDestroy
    public void cleanUpCollection() {
        template.deleteCollection(entityInformation.getCollectionName());
    }

    @After
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll() {
        final List<Address> result = TestUtils.toList(repository.findAll());

        assertThat(result.size()).isEqualTo(4);
    }

    @Test
    public void testFindAllByPage() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_3, null);
        final Page<Address> page = repository.findAll(pageRequest);

        assertThat(page.getContent().size()).isEqualTo(PAGE_SIZE_3);
        validateNonLastPage(page, PAGE_SIZE_3);

        final Page<Address> nextPage = repository.findAll(page.getPageable());
        assertThat(nextPage.getContent().size()).isEqualTo(1);
        validateLastPage(nextPage, PAGE_SIZE_3);
    }

    @Test
    public void testFindWithParitionKeySinglePage() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_3, null);
        final Page<Address> page = repository.findByCity(TestConstants.CITY, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(2);
        validateResultCityMatch(page, TestConstants.CITY);
        validateLastPage(page, PAGE_SIZE_3);
    }

    @Test
    public void testFindWithParitionKeyMultiPages() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_1, null);
        final Page<Address> page = repository.findByCity(TestConstants.CITY, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateResultCityMatch(page, TestConstants.CITY);
        validateNonLastPage(page, PAGE_SIZE_1);

        final Page<Address> nextPage = repository.findByCity(TestConstants.CITY, page.getPageable());

        assertThat(nextPage.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateResultCityMatch(page, TestConstants.CITY);
        validateLastPage(nextPage, PAGE_SIZE_1);
    }

    @Test
    public void testFindWithoutPartitionKeySinglePage() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_3, null);
        final Page<Address> page = repository.findByStreet(TestConstants.STREET, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(2);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateLastPage(page, PAGE_SIZE_3);
    }

    @Test
    public void testFindWithoutPartitionKeyMultiPages() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE_1, null);
        final Page<Address> page = repository.findByStreet(TestConstants.STREET, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(1);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateNonLastPage(page, PAGE_SIZE_1);

        final Page<Address> nextPage = repository.findByStreet(TestConstants.STREET, page.getPageable());

        assertThat(nextPage.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateLastPage(nextPage, PAGE_SIZE_1);
    }

    private void validateResultCityMatch(Page<Address> page, String city) {
        assertThat(page.getContent().stream().filter(address -> address.getCity().equals(city))
                .collect(Collectors.toList()).size()).isEqualTo(page.getContent().size());
    }

    private void validateResultStreetMatch(Page<Address> page, String street) {
        assertThat(page.getContent().stream().filter(address -> address.getStreet().equals(street))
                .collect(Collectors.toList()).size()).isEqualTo(page.getContent().size());
    }
}
