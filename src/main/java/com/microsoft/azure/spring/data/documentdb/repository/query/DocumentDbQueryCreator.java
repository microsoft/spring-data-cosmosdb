/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbPersistentProperty;
import com.microsoft.azure.spring.data.documentdb.core.query.Criteria;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.Iterator;


public class DocumentDbQueryCreator extends AbstractQueryCreator<Query, Criteria> {

    private final MappingContext<?, DocumentDbPersistentProperty> mappingContext;

    public DocumentDbQueryCreator(PartTree tree, DocumentDbParameterAccessor accessor,
                                  MappingContext<?, DocumentDbPersistentProperty> mappingContext) {
        super(tree, accessor);

        this.mappingContext = mappingContext;
    }

    @Override
    protected Criteria create(Part part, Iterator<Object> iterator) {
        final PersistentPropertyPath<DocumentDbPersistentProperty> propertyPath =
                mappingContext.getPersistentPropertyPath(part.getProperty());
        final DocumentDbPersistentProperty property = propertyPath.getLeafProperty();
        final Criteria criteria = from(part, property, Criteria.where(propertyPath.toDotPath()), iterator);

        return criteria;
    }

    @Override
    protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {
        if (base == null) {
            return create(part, iterator);
        }

        final PersistentPropertyPath<DocumentDbPersistentProperty> path =
                mappingContext.getPersistentPropertyPath(part.getProperty());
        final DocumentDbPersistentProperty property = path.getLeafProperty();

        return from(part, property, base.and(path.toDotPath()), iterator);
    }

    @Override
    protected Query complete(Criteria criteria, Sort sort) {
        final Query query = new Query(criteria);
        return query;
    }

    @Override
    protected Criteria or(Criteria base, Criteria criteria) {
        // not supported yet
        throw new NotImplementedException("Criteria or is not supported.");
    }

    private Criteria from(Part part, DocumentDbPersistentProperty property,
                          Criteria criteria, Iterator<Object> parameters) {

        final Part.Type type = part.getType();

        switch (type) {
            case SIMPLE_PROPERTY:

                return isSimpleComparisionPossible(part) ? criteria.is(parameters.next())
                        : createLikeRegexCriteriaOrThrow(part, property, criteria, parameters, false);
            default:
                throw new IllegalArgumentException("unsupported keyword: " + type);
        }
    }

    private boolean isSimpleComparisionPossible(Part part) {
        switch (part.shouldIgnoreCase()) {
            case NEVER:
                return true;
            case WHEN_POSSIBLE:
                return part.getProperty().getType() != String.class;
            case ALWAYS:
                return false;
            default:
                return true;
        }
    }

    private Criteria createLikeRegexCriteriaOrThrow(Part part, DocumentDbPersistentProperty property,
        Criteria criteria, Iterator<Object> parameters, boolean shouldNegateExpression) {
        final PropertyPath path = part.getProperty().getLeafProperty();

        switch (part.shouldIgnoreCase()) {
            case ALWAYS:
                if (path.getType() != String.class) {
                    throw new IllegalArgumentException("part must be String, but: " + path.getType() + ", " + path);
                }

                /* fall through */
            case WHEN_POSSIBLE:

                return criteria;

            case NEVER:
                break;
        }

        return null;
    }

}
