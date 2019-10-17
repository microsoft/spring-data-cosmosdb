/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository.config;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * {@link DocumentDbRepositoriesRegistrar} is deprecated.
 * Instead use CosmosRepositoriesRegistrar, which is introduced in 2.2.0 version.
 */
@Deprecated
public class DocumentDbRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableDocumentDbRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new DocumentDbRepositoryConfigurationExtension();
    }


}
