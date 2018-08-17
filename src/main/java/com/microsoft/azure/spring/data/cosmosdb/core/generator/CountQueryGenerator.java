package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.documentdb.SqlParameter;
import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.javatuples.Pair;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter.toDocumentDBValue;

public class CountQueryGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {
    public <T> CountQueryGenerator(@NonNull Class<T> domainClass) {
        super(domainClass);
    }

    @Override
    public SqlQuerySpec generate(DocumentQuery query) {
        final String queryHeader = "SELECT VALUE COUNT(1) FROM r";
        if (query == null) {
            return new SqlQuerySpec(queryHeader, null);
        }

        final Pair<String, List<Pair<String, Object>>> queryBody = super.generateQueryBody(query);

        final String queryString = queryHeader + " WHERE " + queryBody.getValue0();
        final List<Pair<String, Object>> parameters = queryBody.getValue1();
        final SqlParameterCollection sqlParameters = new SqlParameterCollection();

        sqlParameters.addAll(parameters.stream()
                .map(p -> new SqlParameter("@" + p.getValue0(), toDocumentDBValue(p.getValue1())))
                .collect(Collectors.toList()));

        return new SqlQuerySpec(queryString, sqlParameters);
    }
}
