/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.documentdb.SqlParameter;
import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter.toDocumentDBValue;

@NoArgsConstructor
public class FindQuerySpecGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {

    @Override
    public SqlQuerySpec generate(@NonNull DocumentQuery query) {
        final Pair<String, List<Pair<String, Object>>> queryBody = super.generateQueryBody(query);
        final String queryHead = "SELECT * FROM ROOT r";
        final String queryTail = super.generateQueryTail(query);
        final String queryString = String.join(" ", queryHead, queryBody.getValue0(), queryTail);
        final SqlParameterCollection sqlParameters = new SqlParameterCollection();

        sqlParameters.addAll(queryBody.getValue1().stream()
                .map(p -> new SqlParameter("@" + p.getValue0(), toDocumentDBValue(p.getValue1())))
                .collect(Collectors.toList()));

        return new SqlQuerySpec(queryString, sqlParameters);
    }
}
