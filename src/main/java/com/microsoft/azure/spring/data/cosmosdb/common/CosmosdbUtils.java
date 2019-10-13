/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.common;

import com.azure.data.cosmos.CosmosResponse;
import com.azure.data.cosmos.CosmosResponseDiagnostics;
import com.azure.data.cosmos.FeedResponse;
import com.azure.data.cosmos.FeedResponseDiagnostics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.spring.data.cosmosdb.core.ResponseDiagnostics;
import com.microsoft.azure.spring.data.cosmosdb.core.ResponseDiagnosticsProcessor;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.ObjectMapperFactory;
import com.microsoft.azure.spring.data.cosmosdb.exception.ConfigurationException;
import lombok.NonNull;

import java.io.IOException;

public class CosmosdbUtils {

    @SuppressWarnings("unchecked")
    public static <T> T getCopyFrom(@NonNull T instance) {
        final ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();

        try {
            final String s = mapper.writeValueAsString(instance);
            return (T) mapper.readValue(s, instance.getClass());
        } catch (IOException e) {
            throw new ConfigurationException("failed to get copy from " + instance.getClass().getName(), e);
        }
    }

    public static void fillAndProcessResponseDiagnostics(ResponseDiagnosticsProcessor responseDiagnosticsProcessor,
                                                         CosmosResponse cosmosResponse, FeedResponse feedResponse) {
        if (responseDiagnosticsProcessor == null) {
            return;
        }
        CosmosResponseDiagnostics cosmosResponseDiagnostics = null;
        if (cosmosResponse != null) {
            cosmosResponseDiagnostics = cosmosResponse.cosmosResponseDiagnosticsString();
        }
        FeedResponseDiagnostics feedResponseDiagnostics = null;
        if (feedResponse != null) {
            feedResponseDiagnostics = feedResponse.feedResponseDiagnostics();
        }
        if (cosmosResponseDiagnostics == null && feedResponseDiagnostics == null) {
            responseDiagnosticsProcessor.processResponseDiagnostics(null);
            return;
        }
        final ResponseDiagnostics responseDiagnostics =
            new ResponseDiagnostics(cosmosResponseDiagnostics, feedResponseDiagnostics);

        //  Process response diagnostics
        responseDiagnosticsProcessor.processResponseDiagnostics(responseDiagnostics);
    }
}
