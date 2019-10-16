/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.CosmosResponseDiagnostics;
import com.azure.data.cosmos.FeedResponseDiagnostics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseDiagnostics {

    private CosmosResponseDiagnostics cosmosResponseDiagnostics;
    private FeedResponseDiagnostics feedResponseDiagnostics;

    @Override
    public String toString() {
        final StringBuilder diagnostics = new StringBuilder();
        if (cosmosResponseDiagnostics != null) {
            diagnostics.append("cosmosResponseDiagnostics={")
                       .append(cosmosResponseDiagnostics)
                       .append("}");
        }
        if (feedResponseDiagnostics != null) {
            diagnostics.append("feedResponseDiagnostics={")
                       .append(feedResponseDiagnostics)
                       .append("}");
        }
        return diagnostics.toString();
    }
}
