/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.CosmosResponseDiagnostics;
import com.azure.data.cosmos.FeedResponse;
import com.azure.data.cosmos.FeedResponseDiagnostics;
import com.azure.data.cosmos.Resource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseDiagnostics {

    private CosmosResponseDiagnostics cosmosResponseDiagnostics;
    private FeedResponseDiagnostics feedResponseDiagnostics;
    private CosmosResponseStatistics cosmosResponseStatistics;

    public ResponseDiagnostics(CosmosResponseDiagnostics cosmosResponseDiagnostics,
                               FeedResponseDiagnostics feedResponseDiagnostics) {
        this.cosmosResponseDiagnostics = cosmosResponseDiagnostics;
        this.feedResponseDiagnostics = feedResponseDiagnostics;
    }

    public ResponseDiagnostics(CosmosResponseDiagnostics cosmosResponseDiagnostics,
                               FeedResponseDiagnostics feedResponseDiagnostics,
                               CosmosResponseStatistics cosmosResponseStatistics) {
        this.cosmosResponseDiagnostics = cosmosResponseDiagnostics;
        this.feedResponseDiagnostics = feedResponseDiagnostics;
        this.cosmosResponseStatistics = cosmosResponseStatistics;
    }

    @Override
    public String toString() {
        final StringBuilder diagnostics = new StringBuilder();
        if (cosmosResponseDiagnostics != null) {
            diagnostics.append("cosmosResponseDiagnostics={")
                       .append(cosmosResponseDiagnostics)
                       .append("}");
        }
        if (feedResponseDiagnostics != null) {
            if (diagnostics.length() != 0) {
                diagnostics.append(", ");
            }
            diagnostics.append("feedResponseDiagnostics={")
                       .append(feedResponseDiagnostics)
                       .append("}");
        }
        if (cosmosResponseStatistics != null) {
            if (diagnostics.length() != 0) {
                diagnostics.append(", ");
            }
            diagnostics.append("cosmosResponseStatistics={")
                       .append(cosmosResponseStatistics)
                       .append("}");
        }
        return diagnostics.toString();
    }

    @Getter
    public static class CosmosResponseStatistics {

        private final double requestCharge;
        private final String activityId;

        public <T extends Resource> CosmosResponseStatistics(FeedResponse<T> feedResponse) {
            this.requestCharge = feedResponse.requestCharge();
            this.activityId = feedResponse.activityId();
        }

        @Override
        public String toString() {
            return "CosmosResponseStatistics{" +
                "requestCharge=" + requestCharge +
                ", activityId='" + activityId + '\'' +
                '}';
        }
    }
}
