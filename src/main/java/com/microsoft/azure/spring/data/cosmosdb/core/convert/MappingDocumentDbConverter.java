/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.spring.data.cosmosdb.Constants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbPersistentEntity;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbPersistentProperty;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.util.Assert;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static com.microsoft.azure.spring.data.cosmosdb.Constants.ISO_8601_COMPATIBLE_DATE_PATTERN;

public class MappingDocumentDbConverter
        implements EntityConverter<DocumentDbPersistentEntity<?>, DocumentDbPersistentProperty, Object, Document>,
        ApplicationContextAware {

    protected final MappingContext<? extends DocumentDbPersistentEntity<?>,
            DocumentDbPersistentProperty> mappingContext;
    protected GenericConversionService conversionService;
    private ApplicationContext applicationContext;
    private ObjectMapper objectMapper;

    public MappingDocumentDbConverter(
            MappingContext<? extends DocumentDbPersistentEntity<?>, DocumentDbPersistentProperty> mappingContext,
            @Qualifier(Constants.OBJECTMAPPER_BEAN_NAME) ObjectMapper objectMapper) {
        this.mappingContext = mappingContext;
        this.conversionService = new GenericConversionService();
        this.objectMapper = objectMapper == null ? ObjectMapperFactory.getObjectMapper() : objectMapper;
    }

    @Override
    public <R extends Object> R read(Class<R> type, Document sourceDocument) {
        if (sourceDocument == null) {
            return null;
        }

        final DocumentDbPersistentEntity<?> entity = mappingContext.getPersistentEntity(type);
        Assert.notNull(entity, "Entity is null.");

        return readInternal(entity, type, sourceDocument);
    }

    protected <R extends Object> R readInternal(final DocumentDbPersistentEntity<?> entity, Class<R> type,
                                                final Document sourceDocument) {
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
            throw new IllegalStateException("Failed to read the source document " + sourceDocument.toJson()
                    + "  to target type " + type, e);
        }
    }

    @Override
    @Deprecated
    public void write(Object sourceEntity, Document document) {
        throw new UnsupportedOperationException("The feature is not implemented yet");
    }

    public Document writeDoc(Object sourceEntity) {
        if (sourceEntity == null) {
            return null;
        }

        final DocumentDbPersistentEntity<?> persistentEntity =
                mappingContext.getPersistentEntity(sourceEntity.getClass());

        if (persistentEntity == null) {
            throw new MappingException("no mapping metadata for entity type: " + sourceEntity.getClass().getName());
        }

        final ConvertingPropertyAccessor accessor = getPropertyAccessor(sourceEntity);
        final DocumentDbPersistentProperty idProperty = persistentEntity.getIdProperty();
        final Document document;

        try {
            document = new Document(objectMapper.writeValueAsString(sourceEntity));
        } catch (JsonProcessingException e) {
            throw new DocumentDBAccessException("Failed to map document value.", e);
        }

        if (idProperty != null) {
            final Object value = accessor.getProperty(idProperty);
            final String id = value == null ? null : value.toString();
            document.setId(id);
        }

        return document;
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

        Assert.notNull(entityInformation, "EntityInformation should not be null.");
        final PersistentPropertyAccessor accessor = entityInformation.getPropertyAccessor(entity);
        return new ConvertingPropertyAccessor(accessor, conversionService);
    }

    /**
     * Convert a property value to the value stored in CosmosDB
     *
     * @param fromPropertyValue
     * @return
     */
    public static Object toDocumentDBValue(Object fromPropertyValue) {
        if (fromPropertyValue == null) {
            return null;
        }

        // com.microsoft.azure.documentdb.JsonSerializable#set(String, T) cannot set values for Date and Enum correctly
        if (fromPropertyValue instanceof Date) {
            fromPropertyValue = ((Date) fromPropertyValue).getTime();
        } else if (fromPropertyValue instanceof ZonedDateTime) {
            fromPropertyValue = ((ZonedDateTime) fromPropertyValue)
                    .format(DateTimeFormatter.ofPattern(ISO_8601_COMPATIBLE_DATE_PATTERN));
        } else if (fromPropertyValue instanceof Enum) {
            fromPropertyValue = fromPropertyValue.toString();
        }

        return fromPropertyValue;
    }
}
