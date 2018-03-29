/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.repository.config;

import com.microsoft.azure.spring.data.documentdb.Constants;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.documentdb.repository.DocumentDbRepository;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbRepositoryFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;


public class DocumentDbRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    @Override
    public String getModuleName() {
        return Constants.DOCUMENTDB_MODULE_NAME;
    }

    @Override
    public String getModulePrefix() {
        return Constants.DOCUMENTDB_MODULE_PREFIX;
    }

    public String getRepositoryFactoryBeanClassName() {
        return DocumentDbRepositoryFactoryBean.class.getName();
    }

    @Override
    public String getRepositoryFactoryClassName() {
        return DocumentDbRepositoryFactoryBean.class.getName();
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.<Class<?>>singleton(DocumentDbRepository.class);
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.emptyList();
    }


    @Override
    public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {
        super.registerBeansForRoot(registry, config);

        if (!registry.containsBeanDefinition(Constants.DOCUMENTDB_MAPPING_CONTEXT_NAME)) {
            final RootBeanDefinition definition = new RootBeanDefinition(DocumentDbMappingContext.class);
            definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
            definition.setSource(config.getSource());

            registry.registerBeanDefinition(Constants.DOCUMENTDB_MAPPING_CONTEXT_NAME, definition);
        }
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        super.postProcess(builder, source);
    }
}
