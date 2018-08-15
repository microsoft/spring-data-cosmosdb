/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import com.microsoft.azure.spring.data.cosmosdb.repository.DocumentDbRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends DocumentDbRepository<Address, String> {
    void deleteByPostalCodeAndCity(String postalCode, String city);

    void deleteByCity(String city);

    List<Address> findByPostalCodeAndCity(String postalCode, String city);

    List<Address> findByCity(String city);

    List<Address> findByPostalCode(String postalCode);

    List<Address> findByStreetOrCity(String street, String city);

}
