/*  Activity.java The primary purpose of this class is containerize
 *  the metadata of an OpenAI acitivty object.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.metadata;

import java.util.List;
import java.util.Map;

public class Activity {

    private static final MetadataType<String> STRING = new MetadataString();
    private static final MetadataType<Long> LONG = new MetadataLong();
    private static final MetadataType<Integer> INTEGER = new MetadataInteger();
    private static final MetadataType<Double> DOUBLE = new MetadataDouble();
    private static final MetadataType<Float> FLOAT = new MetadataFloat();
    private static final MetadataType<Boolean> BOOLEAN = new MetadataBoolean();
    private static final MetadataType<Map<String, Object>> MAP = new MetadataMap();

    public static void parseActivity(Map<String, Object> responseMap, MetadataContainer container) {

        MetadataKey<String> activityObjectTypeKey = new MetadataKey<>("activity_object", STRING);
        String activityObjectType = (String) responseMap.get("object");
        container.put(activityObjectTypeKey, activityObjectType);
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) responseMap.get("data");
        for (int i = 0; i < buckets.size(); i++) {
            Map<String, Object> bucket = buckets.get(i);
            MetadataKey<Long> bucketStartTimeKey = new MetadataKey<>("activity_bucket_" + i + "_start_time", LONG);
            MetadataKey<Long> bucketEndTimeKey = new MetadataKey<>("activity_bucket_" + i + "_end_time", LONG);
            Long bucketStartTime = ((Number) bucket.get("start_time")).longValue();
            Long bucketEndTime = ((Number) bucket.get("end_time")).longValue();
            container.put(bucketStartTimeKey, bucketStartTime);
            container.put(bucketEndTimeKey, bucketEndTime);
            List<Map<String, Object>> results = (List<Map<String, Object>>) bucket.get("results");
            for (int j = 0; j < results.size(); j++) {
                Map<String, Object> result = results.get(j);
                MetadataKey<Integer> inputTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_tokens", INTEGER);
                MetadataKey<Integer> outputTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_output_tokens", INTEGER);
                MetadataKey<Integer> numModelRequestsKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_num_model_requests", INTEGER);
                MetadataKey<String> projectIdKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_project_id", STRING);
                MetadataKey<String> userIdKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_user_id", STRING);
                MetadataKey<String> apiKeyIdKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_api_key_id", STRING);
                MetadataKey<String> modelKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_model", STRING);
                MetadataKey<String> serviceTierKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_service_tier", STRING);
                MetadataKey<Integer> inputCachedTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_cached_tokens", INTEGER);
                MetadataKey<Integer> inputUncachedTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_uncached_tokens", INTEGER);
                MetadataKey<Integer> inputAudioTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_input_audio_tokens", INTEGER);
                MetadataKey<Integer> outputAudioTokensKey = new MetadataKey<>("activity_bucket_" + i + "_result_" + j + "_output_audio_tokens", INTEGER);
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
