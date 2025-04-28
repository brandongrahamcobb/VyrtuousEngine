package com.brandongcobb.vyrtuous.metadata;

import java.util.List;
import java.util.Map;

public class Activity {

    public static void parseActivity(Map<String, Object> responseMap, MetadataContainer container) {
        // Object type ("list")
        MetadataKey<String> activityObjectTypeKey = new MetadataKey<>("activity_object", String.class);
        String activityObjectType = (String) responseMap.get("object");
        container.put(activityObjectTypeKey, activityObjectType);

        // Data buckets
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) responseMap.get("data");
        for (int i = 0; i < buckets.size(); i++) {
            Map<String, Object> bucket = buckets.get(i);

            // Each bucket's start_time and end_time
            MetadataKey<Long> bucketStartTimeKey = new MetadataKey<>("activity_bucket_" + i + "_start_time", Long.class);
            MetadataKey<Long> bucketEndTimeKey = new MetadataKey<>("activity_bucket_" + i + "_end_time", Long.class);
            Long bucketStartTime = ((Number) bucket.get("start_time")).longValue();
            Long bucketEndTime = ((Number) bucket.get("end_time")).longValue();
            container.put(bucketStartTimeKey, bucketStartTime);
            container.put(bucketEndTimeKey, bucketEndTime);

            // Results inside each bucket
            List<Map<String, Object>> results = (List<Map<String, Object>>) bucket.get("results");
            for (int j = 0; j < results.size(); j++) {
                Map<String, Object> result = results.get(j);

                // Usage fields
                MetadataKey<Integer> inputTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_tokens", Integer.class);
                MetadataKey<Integer> outputTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_output_tokens", Integer.class);
                MetadataKey<Integer> numModelRequestsKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_num_model_requests", Integer.class);
                MetadataKey<String> projectIdKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_project_id", String.class);
                MetadataKey<String> userIdKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_user_id", String.class);
                MetadataKey<String> apiKeyIdKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_api_key_id", String.class);
                MetadataKey<String> modelKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_model", String.class);
                MetadataKey<String> serviceTierKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_service_tier", String.class);
                MetadataKey<Integer> inputCachedTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_cached_tokens", Integer.class);
                MetadataKey<Integer> inputUncachedTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_uncached_tokens", Integer.class);
                MetadataKey<Integer> inputAudioTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_audio_tokens", Integer.class);
                MetadataKey<Integer> outputAudioTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_output_audio_tokens", Integer.class);

                container.put(inputTokensKey, ((Number) result.get("input_tokens")).intValue());
                container.put(outputTokensKey, ((Number) result.get("output_tokens")).intValue());
                container.put(numModelRequestsKey, ((Number) result.get("num_model_requests")).intValue());
                container.put(projectIdKey, (String) result.get("project_id"));
                container.put(userIdKey, (String) result.get("user_id"));
                container.put(apiKeyIdKey, (String) result.get("api_key_id"));
                container.put(modelKey, (String) result.get("model"));
                container.put(serviceTierKey, (String) result.get("service_tier"));
                container.put(inputCachedTokensKey, ((Number) result.get("input_cached_tokens")).intValue());
                container.put(inputUncachedTokensKey, ((Number) result.get("input_uncached_tokens")).intValue());
                container.put(inputAudioTokensKey, ((Number) result.get("input_audio_tokens")).intValue());
                container.put(outputAudioTokensKey, ((Number) result.get("output_audio_tokens")).intValue());
            }
        }
    }
}
