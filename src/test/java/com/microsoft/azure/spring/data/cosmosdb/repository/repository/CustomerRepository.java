/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import com.microsoft.azure.spring.data.cosmosdb.domain.Customer;
import com.microsoft.azure.spring.data.cosmosdb.repository.DocumentDbRepository;

import java.util.List;

public interface CustomerRepository extends DocumentDbRepository<Customer, String> {
    List<Customer> findByUser_Name(String name);
}
