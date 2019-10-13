/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.common;

import com.azure.data.cosmos.CosmosResponseDiagnostics;
import com.azure.data.cosmos.FeedResponseDiagnostics;
import com.microsoft.azure.spring.data.cosmosdb.core.ResponseDiagnostics;
import com.microsoft.azure.spring.data.cosmosdb.core.ResponseDiagnosticsProcessor;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ResponseDiagnosticsTestUtils {

    private final AtomicInteger counter;
    private final ResponseDiagnosticsProcessor responseDiagnosticsProcessor;
    private ResponseDiagnostics diagnostics;

    public ResponseDiagnosticsTestUtils() {
        counter = new AtomicInteger(0);
        responseDiagnosticsProcessor = responseDiagnostics -> {
            counter.incrementAndGet();
            diagnostics = responseDiagnostics;
        };
    }

    public void initializeCounter() {
        this.counter.set(0);
    }

    public int getCounterValue() {
        return this.counter.get();
    }

    public CosmosResponseDiagnostics getCosmosResponseDiagnostics() {
        return diagnostics == null ? null : diagnostics.getCosmosResponseDiagnostics();
    }

    public FeedResponseDiagnostics getFeedResponseDiagnostics() {
        return diagnostics == null ? null : diagnostics.getFeedResponseDiagnostics();
    }
}
