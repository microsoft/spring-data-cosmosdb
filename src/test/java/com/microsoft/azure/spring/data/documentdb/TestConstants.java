/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb;

import com.microsoft.azure.documentdb.IndexingMode;
import com.microsoft.azure.spring.data.documentdb.domain.Address;

import java.util.Arrays;
import java.util.List;

public class TestConstants {

    private TestConstants() {
        // Hide the implicit public one
    }

    private static final Address ADDRESS_1 = new Address("201107", "Zixing Road", "Shanghai");
    private static final Address ADDRESS_2 = new Address("200000", "Xuhui", "Shanghai");
    public static final List<String> HOBBIES = Arrays.asList("photography", "fishing");
    public static final List<String> UPDATED_HOBBIES = Arrays.asList("updatedPhotography", "updatedFishing");
    public static final List<Address> ADDRESSES = Arrays.asList(ADDRESS_1, ADDRESS_2);

    public static final String DEFAULT_COLLECTION_NAME = "Person";
    public static final int DEFAULT_REQUEST_UNIT = 4000;
    public static final boolean DEFAULT_INDEXINGPOLICY_AUTOMATIC = true;
    public static final IndexingMode DEFAULT_INDEXINGPOLICY_MODE = IndexingMode.Consistent;
    public static final String[] DEFAULT_EXCLUDEDPATHS = {};
    public static final String[] DEFAULT_INCLUDEDPATHS = {
            "{\"path\":\"/*\",\"indexes\":[" +
                    "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":-1}," +
                    "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":3}" +
                    "]}",
    };

    public static final String ROLE_COLLECTION_NAME = "RoleCollectionName";
    public static final int REQUEST_UNIT = 1000;
    public static final String REQUEST_UNIT_STRING = "1000";
    public static final boolean INDEXINGPOLICY_AUTOMATIC = false;
    public static final IndexingMode INDEXINGPOLICY_MODE = IndexingMode.Lazy;
    public static final String INCLUDEDPATH_0 = "{\"path\":\"/*\",\"indexes\":[" +
            "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":2}," +
            "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":2}," +
            "{\"kind\":\"Spatial\",\"dataType\":\"Point\"}" +
            "]}";
    public static final String INCLUDEDPATH_1 = "{\"path\":\"/cache/*\",\"indexes\":[" +
            "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":3}," +
            "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":3}," +
            "{\"kind\":\"Spatial\",\"dataType\":\"LineString\"}" +
            "]}";
    public static final String INCLUDEDPATH_2 = "{\"path\":\"/entities/*\",\"indexes\":[" +
            "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":4}," +
            "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":4}," +
            "{\"kind\":\"Spatial\",\"dataType\":\"Polygon\"}" +
            "]}";
    public static final String[] INCLUDEDPATHS = {
            INCLUDEDPATH_0,
            INCLUDEDPATH_1,
            INCLUDEDPATH_2,
    };
    public static final String EXCLUDEDPATH_0 = "{\"path\":\"/excluded/*\"}";
    public static final String EXCLUDEDPATH_1 = "{\"path\":\"/props/*\"}";
    public static final String[] EXCLUDEDPATHS = {
            EXCLUDEDPATH_0,
            EXCLUDEDPATH_1,
    };

    public static final String DB_NAME = "database_name_pli";
    public static final String ID = "template_id_pli";
    public static final String FIRST_NAME = "first_name_li";
    public static final String LAST_NAME = "last_name_p";
    public static final String NEW_ID = "new_id";
    public static final String NEW_FIRST_NAME = "new_first_name";
    public static final String NEW_LAST_NAME = "new_last_name";
    public static final String UPDATED_FIRST_NAME = "updated_first_name";
    public static final String UPDATED_LAST_NAME = "updated_last_name";
    public static final String LEVEL = "B";
    public static final String ROLE_NAME = "Developer";
    public static final String NOT_EXIST_ID = "non_exist_id";

    public static final String DATE_STRING = "1/1/2000";
    public static final String NEW_DATE_STRING = "1/1/2001";
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_TIMEZONE_STRING = "1/1/2000 00:00 GMT";
    public static final String DATE_TIMEZONE_FORMAT = "dd/MM/yyyy HH:mm ZZZ";
    public static final long MILLI_SECONDS = 946684800000L;

    public static final String POSTAL_CODE = "98052";
    public static final String POSTAL_CODE_0 = "00000";
    public static final String POSTAL_CODE_1 = "11111";
    public static final String CITY = "testCity";
    public static final String CITY_0 = "testCityZero";
    public static final String CITY_1 = "testCityOne";
    public static final String UPDATED_CITY = "updatedCityOne";
    public static final String STREET = "testStreet";
    public static final String STREET_0 = "testStreetZero";
    public static final String STREET_1 = "testStreetOne";
    public static final String STREET_2 = "testStreetTwo";
    public static final String NEW_STREET = "newTestStreet";
    public static final String UPDATED_STREET = "updatedTestStreet";
    public static final String MESSAGE = "test pojo with date";
    public static final String NEW_MESSAGE = "new test message";

    public static final String CRITERIA_KEY = "CriteriaTestKey";
    public static final String CRITERIA_OBJECT = "CriteriaTestObject";

    public static final String DOCUMENTDB_FAKE_HOST = "https://fakeuri";
    public static final String DOCUMENTDB_FAKE_KEY = "fakekey";

    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_HOBBIES = "hobbies";
    public static final String PROPERTY_SHIPPING_ADDRESSES = "shippingAddresses";

    public static final String PROPERTY_CITY = "city";
    public static final String PROPERTY_STREET = "street";

    public static final String PROPERTY_MESSAGE = "message";
    public static final String PROPERTY_DATE = "date";
}

