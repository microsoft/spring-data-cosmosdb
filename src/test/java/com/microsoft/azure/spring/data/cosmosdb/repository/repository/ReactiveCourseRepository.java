/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import com.microsoft.azure.spring.data.cosmosdb.domain.Course;
import com.microsoft.azure.spring.data.cosmosdb.repository.ReactiveCosmosRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface ReactiveCourseRepository extends ReactiveCosmosRepository<Course, String> {

    Flux<Course> findByDepartmentIn(Collection<String> departments);
    
    /**
     * Find Course list by (name and department) or (name2 and department2)
     * @param name name
     * @param department department
     * @param name2 name2
     * @param department2 department2
     * @return Course list
     */
    Flux<Course> findByNameAndDepartmentOrNameAndDepartment(String name,
        String department, String name2, String department2);

}
