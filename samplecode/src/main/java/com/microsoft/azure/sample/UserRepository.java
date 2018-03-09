/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.sample;

import com.microsoft.azure.spring.data.documentdb.repository.DocumentDbRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends DocumentDbRepository<User, String> {
    List<User> findByFirstName(String firstName);

    List<User> findByLastName(String lastName);

    List<User> findByEmailAddressAndLastName(String emailAddress, String lastName);
}

