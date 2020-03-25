/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.domain;

import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Objects;

@Document()
@Data
@AllArgsConstructor
public class Address {
    @Id
    String postalCode;
    String street;
    @PartitionKey
    String city;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Address address = (Address) o;
        return Objects.equals(postalCode, address.postalCode) &&
            Objects.equals(street, address.street) &&
            Objects.equals(city, address.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postalCode, street, city);
    }
}
