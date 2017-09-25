/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends DocumentDbRepository<Contact, String> {
    List<Contact> findByTitle(String title);
}
