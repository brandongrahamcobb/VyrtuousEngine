/*  AIManager.java The primary purpose of this class is to manage the=
 *  core AI functions of Vyrtuous.
 *  Copyright (C) 2024  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import com.brandongcobb.vyrtuous.utils.inc.ModelRegistry;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.metadata.MetadataKey;
import com.brandongcobb.vyrtuous.utils.handlers.ResponseObject;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RequestObject extends MetadataContainer {

    public RequestObject(Map<String, Object> requestMap) {
        MetadataKey<String> modelKey = new MetadataKey<>("model", String.class);
        String model = (String) requestMap.get("model");
        if (model == null) {
            throw new NullPointerException("Request map is missing the mandatory 'model' field.");
        }
        put(modelKey, model);
        MetadataKey<Float> temperatureKey = new MetadataKey<>("temperature", Float.class);
        Object tempObj = requestMap.get("temperature");
        if (tempObj != null) {
            // Parse or cast the value to Float
            Float temperature = Float.parseFloat(tempObj.toString());
            put(temperatureKey, temperature);
        }
        MetadataKey<Float> topPKey = new MetadataKey<>("top_p", Float.class);
        Object topPObj = requestMap.get("top_p");
        if (topPObj != null) {
            Float topP = Float.parseFloat(topPObj.toString());
            put(topPKey, topP);
        }
        MetadataKey<Boolean> streamKey = new MetadataKey<>("stream", Boolean.class);
        Object streamObj = requestMap.get("stream");
        if (streamObj != null) {
            Boolean stream = Boolean.parseBoolean(streamObj.toString());
            put(streamKey, stream);
        }
        MetadataKey<List<Map<String, Object>>> inputKey = new MetadataKey<>("input", List.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> input = (List<Map<String, Object>>) requestMap.get("input");
        if (input != null) {
            put(inputKey, input);
        } else {
            throw new NullPointerException("Request map is missing the mandatory 'input' field.");
        }
        if (requestMap.containsKey("metadata")) {
            MetadataKey<List<Map<String, String>>> metadataKey = new MetadataKey<>("metadata", List.class);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> metadata = (List<Map<String, String>>) requestMap.get("metadata");
            put(metadataKey, metadata);
        }
        if (requestMap.containsKey("max_output_tokens")) {
            MetadataKey<Long> maxOutputTokensKey = new MetadataKey<>("max_output_tokens", Long.class);
            Object maxOutputTokensObj = requestMap.get("max_output_tokens");
            if (maxOutputTokensObj != null) {
                Long maxOutputTokens = Long.parseLong(maxOutputTokensObj.toString());
                put(maxOutputTokensKey, maxOutputTokens);
            }
        } else if (requestMap.containsKey("max_tokens")) {
            MetadataKey<Long> maxTokensKey = new MetadataKey<>("max_tokens", Long.class);
            Object maxTokensObj = requestMap.get("max_tokens");
            if (maxTokensObj != null) {
                Long maxTokens = Long.parseLong(maxTokensObj.toString());
                put(maxTokensKey, maxTokens);
            }
        }

    }
}
