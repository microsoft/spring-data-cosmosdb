/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.domain;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentIndexingPolicy;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Document()
@Data
@AllArgsConstructor
@DocumentIndexingPolicy(includePaths = TestConstants.ORDER_BY_STRING_PATH)
public class Project {

    @Id
    private String id;

    private String name;

    @PartitionKey
    private String creator;

    private Boolean hasReleased;

    private Long starCount;

    private Long forkCount;
}
