/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.domain.IntegerIdDomain;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.IntegerIdDomainRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class IntegerIdDomainRepositoryIT {

    private static final Integer ID = 231234;
    private static final String NAME = "panli";
    private static final IntegerIdDomain DOMAIN = new IntegerIdDomain(ID, NAME);

    private static final CosmosEntityInformation<IntegerIdDomain, Integer> entityInformation =
            new CosmosEntityInformation<>(IntegerIdDomain.class);

    private static CosmosTemplate staticTemplate;
    private static boolean isSetupDone;

    @Autowired
    private CosmosTemplate template;

    @Autowired
    private IntegerIdDomainRepository repository;

    @Before
    public void setup() {
        if (!isSetupDone) {
            staticTemplate = template;
            template.createContainerIfNotExists(entityInformation);
        }
        this.repository.save(DOMAIN);
        isSetupDone = true;
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @AfterClass
    public static void afterClassCleanup() {
        staticTemplate.deleteContainer(entityInformation.getContainerName());
    }

    @Test
    public void testIntegerIdDomain() {
        this.repository.deleteAll();
        Assert.assertFalse(this.repository.findById(ID).isPresent());

        this.repository.save(DOMAIN);
        final Optional<IntegerIdDomain> foundOptional = this.repository.findById(ID);

        Assert.assertTrue(foundOptional.isPresent());
        Assert.assertEquals(DOMAIN.getNumber(), foundOptional.get().getNumber());
        Assert.assertEquals(DOMAIN.getName(), foundOptional.get().getName());

        this.repository.delete(DOMAIN);

        Assert.assertFalse(this.repository.findById(ID).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDomain() {
        new CosmosEntityInformation<InvalidDomain, Integer>(InvalidDomain.class);
    }

    @AllArgsConstructor
    @Data
    private static class InvalidDomain {

        private int count;

        private String location;
    }
}
