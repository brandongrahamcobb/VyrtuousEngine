package com.brandongcobb.vyrtuous.metadata;

import java.util.List;
import java.util.Map;

public class Costs {

    public static void parseCosts(Map<String, Object> responseMap) {
        // Object type ("list")
        MetadataContainer container = new MetadataContainer();
        MetadataKey<String> costObjectTypeKey = new MetadataKey<>("object", String.class);
        String costObjectType = (String) responseMap.get("object");
        container.put(costObjectTypeKey, costObjectType);

        // Data buckets
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) responseMap.get("data");
        for (int i = 0; i < buckets.size(); i++) {
            Map<String, Object> bucket = buckets.get(i);

            // Each bucket's start_time and end_time
            MetadataKey<Long> bucketStartTimeKey = new MetadataKey<>("bucket_" + i + "_start_time", Long.class);
            MetadataKey<Long> bucketEndTimeKey = new MetadataKey<>("bucket_" + i + "_end_time", Long.class);
            Long bucketStartTime = ((Number) bucket.get("start_time")).longValue();
            Long bucketEndTime = ((Number) bucket.get("end_time")).longValue();
            container.put(bucketStartTimeKey, bucketStartTime);
            container.put(bucketEndTimeKey, bucketEndTime);

            // Results inside each bucket
            List<Map<String, Object>> results = (List<Map<String, Object>>) bucket.get("results");
            for (int j = 0; j < results.size(); j++) {
                Map<String, Object> result = results.get(j);

                // Organization cost result fields
                MetadataKey<Double> resultAmountValueKey = new MetadataKey<>("bucket_" + i + "_result_" + j + "_amount_value", Double.class);
                MetadataKey<String> resultAmountCurrencyKey = new MetadataKey<>("bucket_" + i + "_result_" + j + "_amount_currency", String.class);
                MetadataKey<String> resultProjectIdKey = new MetadataKey<>("bucket_" + i + "_result_" + j + "_project_id", String.class);
                MetadataKey<String> resultProjectNameKey = new MetadataKey<>("bucket_" + i + "_result_" + j + "_project_name", String.class);
                MetadataKey<String> resultOrganizationIdKey = new MetadataKey<>("bucket_" + i + "_result_" + j + "_organization_id", String.class);
                MetadataKey<String> resultOrganizationNameKey = new MetadataKey<>("bucket_" + i + "_result_" + j + "_organization_name", String.class);

                Map<String, Object> amount = (Map<String, Object>) result.get("amount");

                container.put(resultAmountValueKey, ((Number) amount.get("value")).doubleValue());
                container.put(resultAmountCurrencyKey, (String) amount.get("currency"));
                container.put(resultProjectIdKey, (String) result.get("project_id"));
                container.put(resultProjectNameKey, (String) result.get("project_name"));
                container.put(resultOrganizationIdKey, (String) result.get("organization_id"));
                container.put(resultOrganizationNameKey, (String) result.get("organization_name"));
            }
        }
    }
}
