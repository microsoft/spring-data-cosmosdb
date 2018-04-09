/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.domain;

import com.microsoft.azure.spring.data.documentdb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * For testing date purpose
 */
@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Memo {
    private String id;
    private String message;
    private Date date;
}
