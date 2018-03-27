/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository.query;

import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbPersistentProperty;
import com.microsoft.azure.spring.data.documentdb.core.query.Criteria;
import com.microsoft.azure.spring.data.documentdb.core.query.Criteria.CriteriaType;
import com.microsoft.azure.spring.data.documentdb.core.query.Query;

import org.apache.commons.lang3.NotImplementedException;
import org.javatuples.Pair;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DocumentDbQueryCreator extends AbstractQueryCreator<Query, Criteria> {

    private static final Logger logger = LoggerFactory.getLogger(DocumentDbQueryCreator.class);

    private static final Map<Part.Type, Pair<CriteriaType, Boolean>> criteriaLookup;
    static {
        final Map<Part.Type, Pair<CriteriaType, Boolean>> init = new HashMap<>();
        init.put(Part.Type.AFTER, new Pair<>(CriteriaType.IS_LESS_THAN_OR_EQUAL, Boolean.TRUE));
        init.put(Part.Type.BEFORE, new Pair<>(CriteriaType.IS_LESS_THAN, Boolean.FALSE));
        init.put(Part.Type.CONTAINING, new Pair<>(CriteriaType.CONTAINING, Boolean.FALSE));
        init.put(Part.Type.BETWEEN, new Pair<>(CriteriaType.BETWEEN, Boolean.FALSE));
        init.put(Part.Type.ENDING_WITH, new Pair<>(CriteriaType.ENDING_WITH, Boolean.FALSE));
        init.put(Part.Type.EXISTS, new Pair<>(CriteriaType.EXISTS, Boolean.FALSE));
        init.put(Part.Type.GREATER_THAN, new Pair<>(CriteriaType.IS_GREATER_THAN_OR_EQUAL, Boolean.FALSE));
        init.put(Part.Type.GREATER_THAN_EQUAL, new Pair<>(CriteriaType.IS_GREATER_THAN, Boolean.FALSE));
        init.put(Part.Type.IS_EMPTY, new Pair<>(CriteriaType.IS_EMPTY, Boolean.FALSE));
        init.put(Part.Type.IS_NOT_EMPTY, new Pair<>(CriteriaType.IS_EMPTY, Boolean.TRUE));
        init.put(Part.Type.IS_NOT_NULL, new Pair<>(CriteriaType.IS_NULL, Boolean.TRUE));
        init.put(Part.Type.IS_NULL, new Pair<>(CriteriaType.IS_NULL, Boolean.FALSE));
        init.put(Part.Type.LESS_THAN, new Pair<>(CriteriaType.IS_LESS_THAN, Boolean.FALSE));
        init.put(Part.Type.LESS_THAN_EQUAL, new Pair<>(CriteriaType.IS_LESS_THAN_OR_EQUAL, Boolean.FALSE));
        init.put(Part.Type.LIKE, new Pair<>(CriteriaType.LIKE, Boolean.FALSE));
        init.put(Part.Type.NEAR, new Pair<>(CriteriaType.NEAR, Boolean.FALSE));
        init.put(Part.Type.NOT_LIKE, new Pair<>(CriteriaType.LIKE, Boolean.TRUE));
        init.put(Part.Type.REGEX, new Pair<>(CriteriaType.REGEX, Boolean.FALSE));
        init.put(Part.Type.STARTING_WITH, new Pair<>(CriteriaType.STARTING_WITH, Boolean.FALSE));
        init.put(Part.Type.WITHIN, new Pair<>(CriteriaType.BETWEEN, Boolean.FALSE));
        init.put(Part.Type.IN, new Pair<>(CriteriaType.IN, Boolean.FALSE));
        init.put(Part.Type.NOT_IN, new Pair<>(CriteriaType.IN, Boolean.TRUE));
        init.put(Part.Type.SIMPLE_PROPERTY, new Pair<>(CriteriaType.IS_EQUAL, Boolean.FALSE));
        init.put(Part.Type.NEGATING_SIMPLE_PROPERTY, new Pair<>(CriteriaType.IS_EQUAL, Boolean.TRUE));
        criteriaLookup = Collections.unmodifiableMap(init);
    }  

    private final MappingContext<?, DocumentDbPersistentProperty> mappingContext;

    public DocumentDbQueryCreator(PartTree tree, DocumentDbParameterAccessor accessor,
                                  MappingContext<?, DocumentDbPersistentProperty> mappingContext) {
        super(tree, accessor);

        this.mappingContext = mappingContext;
    }

    @Override
    protected Criteria create(Part part, Iterator<Object> iterator) {

        logger.debug("Creating criteria from part: {}", part);
        
        return createSingleConditionCriteria(part, iterator);
    }

    @Override
    protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {

        logger.debug("Combining existing {} criteria with {} using AND condition", base, part);

        return Criteria.and(base, createSingleConditionCriteria(part, iterator));
    }

    @Override
    protected Criteria or(Criteria base, Criteria criteria) {

        logger.debug("Combining existing criteria {} with {} using AND condition", base, criteria);

        return Criteria.or(base, criteria);
    }

    @Override
    protected Query complete(Criteria criteria, Sort sort) {

        return new Query(criteria, sort);
    }
    
    private Criteria createSingleConditionCriteria(Part part, Iterator<Object> parameters) {

        final Part.Type type = part.getType();
        
        final String conditionSubject = mappingContext.getPersistentPropertyPath(part.getProperty()).toDotPath();

        switch (type) {

            case FALSE:
                return Criteria.value(conditionSubject, CriteriaType.IS_EQUAL,
                        Arrays.asList(new Object[] {Boolean.FALSE}), isCaseSensitiveCondition(part));

            case TRUE:
                return Criteria.value(conditionSubject, CriteriaType.IS_EQUAL,
                        Arrays.asList(new Object[] {Boolean.TRUE}), isCaseSensitiveCondition(part));

            default:

                // Copy the method parameters into an array of values
                
                final List<Object> valueList = new ArrayList<>();
                
                for (int i = 0; i < part.getNumberOfArguments(); ++i) {
                    valueList.add(parameters.next());
                }

                if (criteriaLookup.containsKey(type)) {
                
                    return Criteria.value(conditionSubject, criteriaLookup.get(type).getValue0(),
                            valueList, isCaseSensitiveCondition(part), criteriaLookup.get(type).getValue1());
                }
                
                throw new IllegalArgumentException("unsupported keyword: " + type);
        }
    }
    
    private boolean isCaseSensitiveCondition(Part part) {
        switch (part.shouldIgnoreCase()) {
            case NEVER:
                return false;
            case WHEN_POSSIBLE:
                return part.getProperty().getType() == String.class;
            case ALWAYS:
                return true;
            default:
                return false;
        }
    }
}
