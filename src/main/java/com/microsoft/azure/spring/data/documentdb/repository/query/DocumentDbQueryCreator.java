/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbPersistentProperty;
import com.microsoft.azure.spring.data.documentdb.core.query.Criteria;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;


public class DocumentDbQueryCreator extends AbstractQueryCreator<Query, Criteria> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDbQueryCreator.class);
    private final DocumentDbParameterAccessor accessor;
    private final MappingContext<?, DocumentDbPersistentProperty> mappingContext;

    public DocumentDbQueryCreator(PartTree tree, DocumentDbParameterAccessor accessor,
                                  MappingContext<?, DocumentDbPersistentProperty> mappingContext) {
        super(tree, accessor);

        this.accessor = accessor;
        this.mappingContext = mappingContext;

    }

    @Override
    protected Criteria create(Part part, Iterator<Object> iterator) {
        final PersistentPropertyPath<DocumentDbPersistentProperty> propertyPath =
                mappingContext.getPersistentPropertyPath(part.getProperty());
        final DocumentDbPersistentProperty property = propertyPath.getLeafProperty();
        final LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
        final Collection<Object> clonedIterator = new ArrayList<Object>();

        while (iterator.hasNext()) {
            final Object obj = iterator.next();
            params.put(propertyPath.toDotPath(), obj);
            clonedIterator.add(obj);
        }

        final Criteria criteria = from(part, property, Criteria.where(propertyPath.toDotPath(), params),
                clonedIterator.iterator());
        return criteria;
    }

    @Override
    protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {
        // not supported yet
        return null;
    }

    @Override
    protected Query complete(Criteria criteria, Sort sort) {
        final Query query = new Query(criteria);
        return query;
    }

    @Override
    protected Criteria or(Criteria base, Criteria criteria) {
        // not supported yet
        return null;
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
