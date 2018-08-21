/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import org.springframework.stereotype.Repository;

@Repository
public interface PageableAddressRepository extends PagingAndSortingRepository<Address, String>{
    Page<Address> findByStreet(String street, Pageable pageable);

    Page<Address> findByCity(String city, Pageable pageable);
}
