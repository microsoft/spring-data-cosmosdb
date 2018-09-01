/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.domain;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentIndexingPolicy;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@DocumentIndexingPolicy(includePaths = TestConstants.STARTSWITH_INCLUDEDPATH)
public class Student {
    private String id;
    private String firstName;
    private String lastName;
}
