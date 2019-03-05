/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.performance;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class PerformanceReport {
    private static final String NEW_LINE = System.lineSeparator();

    private final List<PerfItem> perfItems = new ArrayList<>();

    public void addItem(PerfItem item) {
        perfItems.add(item);
    }

    public List<PerfItem> getPerfItems() {
        return this.perfItems;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        perfItems.forEach(item -> sb.append(item.toString()).append(NEW_LINE));

        return sb.toString();
    }
}

@Data
class PerfItem {
    private OperationType type;
    private long springCost;
    private long sdkCost;
    private int times;
    private float diffToSdk;

    public PerfItem(OperationType type, long springCost, long sdkCost, int times) {
        this.type = type;
        this.springCost = springCost;
        this.sdkCost = sdkCost;
        this.times = times;
        this.diffToSdk = (float) (springCost - sdkCost) / sdkCost;
    }

    @Override
    public String toString() {
        return "[type=" + type.toString() + ", springCost=" + springCost + ", sdkCost=" + sdkCost + ", times=" +
                times + ", diffToSdk=" + (diffToSdk * 100 + "%") + "];";
    }
}

enum OperationType {
    SAVE_ONE("save one"), SAVE_ALL("save all"), DELETE_ONE("delete one"), DELETE_ALL("delete all"),
    FIND_BY_ID("find by id"), FIND_BY_IDS("find by ids"), FIND_ALL("find all"), FIND_BY_SORT("find by sort"),
    FIND_BY_PAGING("find by paging"), FIND_BY_FIELD("find by field"), COUNT("count");

    private String type;

    OperationType(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }

}
