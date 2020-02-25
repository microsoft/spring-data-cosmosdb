/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.domain;

import java.util.List;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentIndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Version;

@Document(ru = "10000")
@Data
@EqualsAndHashCode(exclude = "_etag")
@NoArgsConstructor
@DocumentIndexingPolicy(includePaths = TestConstants.ORDER_BY_STRING_PATH)
public class Person {
    private String id;
    private String firstName;

    @PartitionKey
    private String lastName;
    private List<String> hobbies;
    private List<Address> shippingAddresses;
    @Version
    private String _etag;
    
    public Person(String id, String firstName, String lastName, List<String> hobbies, List<Address> shippingAddresses) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.hobbies = hobbies;
        this.shippingAddresses = shippingAddresses;
    }
}
