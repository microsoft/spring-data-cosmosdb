/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.telemetry.TelemetryTracker;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;


public class AbstractDocumentDbConfiguratinUnitTest {
    private static final String OBJECTMAPPER_BEAN_NAME = Constants.OBJECTMAPPER_BEAN_NAME;

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestDocumentDbConfiguration.class));

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void containsDocumentDbFactory() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(
                TestDocumentDbConfiguration.class);

        Assertions.assertThat(context.getBean(DocumentDbFactory.class)).isNotNull();
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void defaultObjectMapperBeanNotExists() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(
                TestDocumentDbConfiguration.class);

        context.getBean(ObjectMapper.class);
    }

    @Test
    public void objectMapperIsConfigurable() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(
                ObjectMapperConfiguration.class);

        Assertions.assertThat(context.getBean(ObjectMapper.class)).isNotNull();
        Assertions.assertThat(context.getBean(OBJECTMAPPER_BEAN_NAME)).isNotNull();
    }
    @Test
    public void testTelemetryTrackerBean() {
        this.contextRunner
                .withPropertyValues("cosmosdb.telemetryAllowed=true")
                .run(context -> Assert.assertNotNull(context.getBean(TelemetryTracker.class)));

        this.contextRunner
                .run(context -> Assert.assertNotNull(context.getBean(TelemetryTracker.class)));
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testTelemetryTrackerBeanException() {
        this.contextRunner
                .withPropertyValues("cosmosdb.telemetryAllowed=false")
                .run(context -> Assert.assertNull(context.getBean(TelemetryTracker.class)));
    }

    @Configuration
    static class TestDocumentDbConfiguration extends AbstractDocumentDbConfiguration {
        @Mock
        private DocumentClient mockClient;

        @Override
        public String getDatabase() {
            return TestConstants.DB_NAME;
        }

        @Override
        public DocumentClient documentClient() {
            return mockClient;
        }
    }

    @Configuration
    static class ObjectMapperConfiguration extends TestDocumentDbConfiguration {
        @Bean(name = OBJECTMAPPER_BEAN_NAME)
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
