/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.convert;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.spring.data.documentdb.Constants;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbPersistentEntity;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbPersistentProperty;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;

public class MappingDocumentDbConverter
        implements EntityConverter<DocumentDbPersistentEntity<?>, DocumentDbPersistentProperty, Object, Document>,
        ApplicationContextAware {

    protected final MappingContext<? extends DocumentDbPersistentEntity<?>,
            DocumentDbPersistentProperty> mappingContext;
    protected GenericConversionService conversionService;
    private ApplicationContext applicationContext;

    public MappingDocumentDbConverter(
            MappingContext<? extends DocumentDbPersistentEntity<?>, DocumentDbPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
        this.conversionService = new GenericConversionService();
    }

    @Override
    public <R extends Object> R read(Class<R> type, Document sourceDocument) {
        if (sourceDocument == null) {
            return null;
        }

        final DocumentDbPersistentEntity<?> entity = mappingContext.getPersistentEntity(type);
        return readInternal(entity, type, sourceDocument);
    }

    protected <R extends Object> R readInternal(final DocumentDbPersistentEntity<?> entity, Class<R> type,
                                                final Document sourceDocument) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            final DocumentDbPersistentProperty idProperty = entity.getIdProperty();
            final Object idValue = sourceDocument.getId();

            final JSONObject jsonObject = new JSONObject(sourceDocument.toJson());
            if (idProperty != null) {
                // Replace the key id to the actual id field name in domain
                jsonObject.remove(Constants.ID_PROPERTY_NAME);
                jsonObject.put(idProperty.getName(), idValue);
            }

            return objectMapper.readValue(jsonObject.toString(), type);
        } catch (IOException e) {
            throw  new IllegalStateException("Failed to read the source document " + sourceDocument.toJson()
                    + "  to target type " + type, e);
        }
    }

    @Override
    public void write(Object sourceEntity, Document document) {
        if (sourceEntity == null) {
            return;
        }

        final DocumentDbPersistentEntity<?> entity = mappingContext.getPersistentEntity(sourceEntity.getClass());
        writeInternal(sourceEntity, document, entity);
    }

    public void writeInternal(final Object entity,
                              final Document targetDocument,
                              final DocumentDbPersistentEntity<?> entityInformation) {
        if (entity == null) {
            return;
        }

        if (entityInformation == null) {
            throw new MappingException("no mapping metadata for entity type: " + entity.getClass().getName());
        }

        final ConvertingPropertyAccessor accessor = getPropertyAccessor(entity);
        final DocumentDbPersistentProperty idProperty = entityInformation.getIdProperty();

        if (idProperty != null) {
            targetDocument.setId((String) accessor.getProperty(idProperty));
        }

        for (final Field field : entity.getClass().getDeclaredFields()) {
            if (null != idProperty && field.getName().equals(idProperty.getName())) {
                continue;
            }

            final PersistentProperty property = entityInformation.getPersistentProperty(field.getName());
            Assert.notNull(property, "Property is null.");

            targetDocument.set(field.getName(), mapToDocumentDBValue(accessor.getProperty(property)));
        }
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

    public MappingContext<? extends DocumentDbPersistentEntity<?>, DocumentDbPersistentProperty> getMappingContext() {
        return mappingContext;
    }


    private ConvertingPropertyAccessor getPropertyAccessor(Object entity) {
        final DocumentDbPersistentEntity<?> entityInformation = mappingContext.getPersistentEntity(entity.getClass());
        final PersistentPropertyAccessor accessor = entityInformation.getPropertyAccessor(entity);
        return new ConvertingPropertyAccessor(accessor, conversionService);
    }

    /**
     * Convert a property value to the value stored in DocumentDB
     * @param fromPropertyValue
     * @return
     */
    public Object mapToDocumentDBValue(Object fromPropertyValue) {
        if (fromPropertyValue == null) {
            return null;
        }

        if (fromPropertyValue instanceof Date) {
            fromPropertyValue = ((Date) fromPropertyValue).getTime();
        }

        return fromPropertyValue;
    }
}
