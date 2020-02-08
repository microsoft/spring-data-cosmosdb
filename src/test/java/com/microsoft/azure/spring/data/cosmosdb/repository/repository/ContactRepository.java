/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import com.microsoft.azure.spring.data.cosmosdb.domain.Contact;
import com.microsoft.azure.spring.data.cosmosdb.repository.CosmosRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends CosmosRepository<Contact, String> {
    List<Contact> findByTitle(String title);
    Iterable<Contact> findByLogicId(String title);

    Contact findOneByTitle(String title);

    Optional<Contact> findOptionallyByTitle(String title);

}
