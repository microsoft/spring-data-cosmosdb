/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.domain;

import com.microsoft.azure.documentdb.IndexingMode;
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
                "{\"path\":\"/*\",\"indexes\":[" +
                        "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":3}" +
                        "]}",
                "{\"path\":\"/cache/*\",\"indexes\":[" +
                        "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":-1}," +
                        "]}",
        },
        excludePaths = {
                "{\"path\":\"/excluded/*\"}",
                "{\"path\":\"/props/*\"}",
        })
@Document(collection = "Role", ru = "1000")
public class Role {
    @Id
    String id;

    @PartitionKey
    String name;

    String level;
}

