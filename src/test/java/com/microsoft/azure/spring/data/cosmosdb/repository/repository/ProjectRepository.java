/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import com.microsoft.azure.spring.data.cosmosdb.domain.Project;
import com.microsoft.azure.spring.data.cosmosdb.repository.DocumentDbRepository;

import java.util.List;

public interface ProjectRepository extends DocumentDbRepository<Project, String> {

    List<Project> findByNameAndCreator(String name, String creator);

    List<Project> findByNameOrCreator(String name, String creator);
}
