/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.domain.inheritance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class Square extends Shape {
    @Id
    private String id;

    private int length;

    public Square(String id, int length, int area) {
        this.id = id;
        this.length = length;
        this.area = area;
    }
}
