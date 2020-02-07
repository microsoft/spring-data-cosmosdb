/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.azure.data.cosmos.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FindQuerySpecGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {

    @Override
    public SqlQuerySpec generateCosmos(DocumentQuery query) {
        return super.generateCosmosQuery(query, "SELECT * FROM ROOT r");
    }
}
