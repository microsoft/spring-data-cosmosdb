/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.domain;

import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

/**
 * For testing date and enum purpose
 */
@Document()
@Data
public class PageableMemo {
    @Id
    private String id;
    @PartitionKey
    private String message;
    private Date date;
    private Importance importance;

    public PageableMemo() {}

    public PageableMemo(String id, String message, Date date, Importance importance) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.importance = importance;
    }
}
