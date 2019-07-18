/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.domain;

import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "#{@dynamicCollectionContainer.getCollectionName()}")
public class SpELBeanStudent {
    private String id;
    private String firstName;
    private String lastName;
}
