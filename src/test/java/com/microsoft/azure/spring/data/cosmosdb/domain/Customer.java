/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
public class Customer {

    @Id
    private String id;

    private Long level;

    private User user;

    @Data
    @AllArgsConstructor
    public static class User {

        private String name;

        private Long age;
    }
}
