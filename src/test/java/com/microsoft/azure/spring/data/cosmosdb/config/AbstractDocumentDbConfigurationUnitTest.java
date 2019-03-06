/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

public class AbstractDocumentDbConfigurationUnitTest {
    private static final String OBJECTMAPPER_BEAN_NAME = Constants.OBJECTMAPPER_BEAN_NAME;

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
    public void testRequestOptionsConfigurable() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(
                RequestOptionsConfiguration.class);
        final DocumentDbFactory factory = context.getBean(DocumentDbFactory.class);

        Assertions.assertThat(factory).isNotNull();

        final RequestOptions options = factory.getConfig().getRequestOptions();

        Assertions.assertThat(options).isNotNull();
        Assertions.assertThat(options.getConsistencyLevel()).isEqualTo(ConsistencyLevel.ConsistentPrefix);
        Assertions.assertThat(options.getDisableRUPerMinuteUsage()).isTrue();
        Assertions.assertThat(options.isScriptLoggingEnabled()).isTrue();
    }

    @Configuration
    static class TestDocumentDbConfiguration extends AbstractDocumentDbConfiguration {
        @Mock
        private DocumentClient mockClient;

        @Override
        public DocumentDBConfig getConfig() {
            return DocumentDBConfig.builder("http://fake-uri", "fake-key", TestConstants.DB_NAME).build();
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

    @Configuration
    static class RequestOptionsConfiguration extends AbstractDocumentDbConfiguration {

        private RequestOptions getRequestOptions() {
            final RequestOptions options = new RequestOptions();

            options.setConsistencyLevel(ConsistencyLevel.ConsistentPrefix);
            options.setDisableRUPerMinuteUsage(true);
            options.setScriptLoggingEnabled(true);

            return options;
        }

        @Override
        public DocumentDBConfig getConfig() {
            final RequestOptions options = getRequestOptions();
            return DocumentDBConfig.builder("http://fake-uri", "fake-key", TestConstants.DB_NAME)
                    .requestOptions(options)
                    .build();
        }

    }
}
