/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.repository;

import com.microsoft.azure.spring.data.documentdb.domain.Person;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends DocumentDbRepository<Person, String> {
    List<Person> findByAgeGreaterThan(int age);
}
