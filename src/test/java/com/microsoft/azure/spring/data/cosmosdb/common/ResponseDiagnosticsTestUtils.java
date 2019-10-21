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
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ResponseDiagnosticsTestUtils {

    private final ResponseDiagnosticsProcessor responseDiagnosticsProcessor;
    private ResponseDiagnostics diagnostics;

    public ResponseDiagnosticsTestUtils() {
        responseDiagnosticsProcessor = responseDiagnostics -> {
            diagnostics = responseDiagnostics;
        };
    }

    public CosmosResponseDiagnostics getCosmosResponseDiagnostics() {
        return diagnostics == null ? null : diagnostics.getCosmosResponseDiagnostics();
    }

    public FeedResponseDiagnostics getFeedResponseDiagnostics() {
        return diagnostics == null ? null : diagnostics.getFeedResponseDiagnostics();
    }
}
