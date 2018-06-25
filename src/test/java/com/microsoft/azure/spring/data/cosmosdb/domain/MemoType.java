/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.domain;

public enum MemoType {
    HAPPY("happy"),
    SAD("sad"),
    DEFAULT("default");

    private String type;

    MemoType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
