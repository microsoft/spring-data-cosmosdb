/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.domain;

import com.microsoft.azure.documentdb.IndexingMode;
import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.core.mapping.Document;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentIndexingPolicy;
import com.microsoft.azure.spring.data.documentdb.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@DocumentIndexingPolicy(
        mode = IndexingMode.Lazy,
        automatic = false,
        includePaths = {
                TestConstants.INCLUDEDPATH_0,
                TestConstants.INCLUDEDPATH_1,
                TestConstants.INCLUDEDPATH_2,
        },
        excludePaths = {
                TestConstants.EXCLUDEDPATH_0,
                TestConstants.EXCLUDEDPATH_1,
        })
@Document(collection = TestConstants.ROLE_COLLECTION_NAME, ru = TestConstants.REQUEST_UNIT_STRING)
public class Role {
    @Id
    String id;

    @PartitionKey
    String name;

    String level;
}

