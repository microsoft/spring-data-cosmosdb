/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.domain;

import com.microsoft.azure.documentdb.IndexingMode;
import com.microsoft.azure.spring.data.documentdb.core.mapping.Document;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDBIndexingPolicy;
import com.microsoft.azure.spring.data.documentdb.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Document(collection = "Address", ru = "1000")
@DocumentDBIndexingPolicy(mode = IndexingMode.Lazy, automatic = false)
@Data
@AllArgsConstructor
public class Address {
    @Id
    String postalCode;
    String street;
    @PartitionKey
    String city;
}
