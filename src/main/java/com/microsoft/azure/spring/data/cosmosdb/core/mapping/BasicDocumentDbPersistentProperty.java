/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core.mapping;

import com.microsoft.azure.spring.data.cosmosdb.Constants;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * {@link BasicDocumentDbPersistentProperty} is deprecated.
 * Instead use BasicCosmosPersistentProperty, which is introduced in 2.2.0 version.
 */
@Deprecated
public class BasicDocumentDbPersistentProperty extends AnnotationBasedPersistentProperty<DocumentDbPersistentProperty>
        implements DocumentDbPersistentProperty {

    public BasicDocumentDbPersistentProperty(Property property, DocumentDbPersistentEntity<?> owner,
                                             SimpleTypeHolder simpleTypeHolder) {
        super(property, owner, simpleTypeHolder);
    }

    @Override
    protected Association<DocumentDbPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }

    @Override
    public boolean isIdProperty() {

        if (super.isIdProperty()) {
            return true;
        }

        return getName().equals(Constants.ID_PROPERTY_NAME);
    }

}
