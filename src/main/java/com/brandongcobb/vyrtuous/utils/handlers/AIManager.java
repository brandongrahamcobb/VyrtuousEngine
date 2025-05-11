/*  AIManager.java The primary purpose of this class is to manage the=
 *  core AI functions of Vyrtuous.
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
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.metadata.*;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import com.brandongcobb.vyrtuous.utils.handlers.*;
import com.brandongcobb.vyrtuous.utils.inc.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class AIManager {

    private String moderationApiUrl = Maps.OPENAI_ENDPOINT_URLS.get("moderations");
    private String responseApiUrl = Maps.OPENAI_ENDPOINT_URLS.get("responses");
    private EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private Map<String, Object> OPENAI_RESPONSE_FORMAT = new HashMap<>();

    public CompletableFuture<Long> completeCalculateMaxOutputTokens(String model, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Encoding encoding = registry.getEncoding("cl100k_base")
                    .orElseThrow(() -> new IllegalStateException("Encoding cl100k_base not available"));
                long promptTokens = encoding.encode(prompt).size();
                ModelInfo outputInfo = Maps.OPENAI_RESPONSE_MODEL_OUTPUT_LIMITS.get(model);
                long outputLimit = outputInfo != null ? outputInfo.upperLimit() : 4096;
                long tokens = Math.max(1, outputLimit - promptTokens - 20);
                if (tokens < 16) tokens = 16;
                return tokens;
            } catch (Exception e) {
                return 0L;
            }
        });
    }

    private CompletableFuture<ResponseObject> sendRequest(Map<String, Object> requestBody, String endpoint) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing OPENAI_API_KEY"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(endpoint);
                post.setHeader("Authorization", "Bearer " + apiKey);
                post.setHeader("Content-Type", "application/json");
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(requestBody);
                post.setEntity(new StringEntity(json));
                try (CloseableHttpResponse resp = client.execute(post)) {
                    int code = resp.getStatusLine().getStatusCode();
                    String respBody = EntityUtils.toString(resp.getEntity(), "UTF-8");
                    System.out.println(respBody);
                    if (code >= 200 && code < 300) {
                        Map<String, Object> respMap = mapper.readValue(respBody, new TypeReference<>() {});
                        return new ResponseObject(respMap);
                    } else {
                        throw new IOException("HTTP " + code + ": " + respBody);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<Map<String, Object>> buildRequestBody(
        String content,
        String model,
        Map<String, Object> textFormat,
        boolean store,
        boolean stream,
        String systemPrompt,
        float temperature,
        float topP,
        String previousResponseId,
        String requestType
    ) {
        return completeCalculateMaxOutputTokens(model, content).thenApplyAsync(tokens -> {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            if ("perplexity".equals(requestType)) {
                body.put("text", Map.of("format", textFormat));
            }
            if ("moderation".equals(requestType)) {
                body.put("input", content);
            } else {
                body.put("instructions", systemPrompt);
                List<Map<String, Object>> messages = new ArrayList<>();
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("role", "user");
                msgMap.put("content", content);
                messages.add(msgMap);
                body.put("input", messages);
            }
            if (previousResponseId != null && !previousResponseId.isEmpty()) {
                body.put("previous_response_id", previousResponseId);
            }
            if (store) {
                body.put("metadata", List.of(Map.of("timestamp", LocalDateTime.now().toString())));
            }
            ModelInfo info = Maps.OPENAI_RESPONSE_MODEL_CONTEXT_LIMITS.get(model);
            if (info != null && info.status()) {
                body.put("max_output_tokens", tokens);
            } else {
                body.put("max_tokens", tokens);
            }
            return body;
        });
    }

    private CompletableFuture<Map<String, Object>> prepareChatRequest(
        String content,
        String previousResponseId,
        String model
    ) {
        return buildRequestBody(
            content,
            model,
            OPENAI_RESPONSE_FORMAT,
            ModelRegistry.OPENAI_RESPONSE_STORE.asBoolean(),
            ModelRegistry.OPENAI_RESPONSE_STREAM.asBoolean(),
            Maps.OPENAI_RESPONSE_SYS_INPUT,
            ModelRegistry.OPENAI_RESPONSE_TEMPERATURE.asFloat(),
            ModelRegistry.OPENAI_RESPONSE_TOP_P.asFloat(),
            previousResponseId,
            "chat"
        );
    }

    private CompletableFuture<Map<String, Object>> prepareModerationRequest(String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("input", content);
        return CompletableFuture.completedFuture(body);
    }

    private CompletableFuture<Map<String, Object>> preparePerplexityRequest(String content, String model) {
        return buildRequestBody(
            content,
            model,
            Maps.OPENAI_RESPONSE_FORMAT_PERPLEXITY,
            ModelRegistry.OPENAI_RESPONSE_STORE.asBoolean(),
            ModelRegistry.OPENAI_RESPONSE_STREAM.asBoolean(),
            ModelRegistry.OPENAI_PERPLEXITY_SYS_INPUT.asString(),
            ModelRegistry.OPENAI_RESPONSE_TEMPERATURE.asFloat(),
            ModelRegistry.OPENAI_RESPONSE_TOP_P.asFloat(),
            null,
            "perplexity"
        );
    }

    public CompletableFuture<ResponseObject> completeChat(String content, String previousResponseId, String model) {
        return prepareChatRequest(content, previousResponseId, model)
            .thenCompose(reqBody -> sendRequest(reqBody, responseApiUrl));
    }

    public CompletableFuture<ResponseObject> completeModeration(String content) {
        return prepareModerationRequest(content)
            .thenCompose(reqBody -> sendRequest(reqBody, moderationApiUrl));
    }

    public CompletableFuture<ResponseObject> completePerplexity(String content, String model) {
        return preparePerplexityRequest(content, model)
            .thenCompose(reqBody -> sendRequest(reqBody, responseApiUrl));
    }

    public CompletableFuture<String> completeResolveModel(String content, Boolean multiModal, String model) {
        return completePerplexity(content, model)
            .thenCompose(resp -> resp.completeGetPerplexity())
            .thenApply(perplexityObj -> {
                Integer perplexity = (Integer) perplexityObj;
                if (perplexity < 100) return "gpt-4.1-nano";
                if (perplexity > 100 && perplexity < 150 && Boolean.TRUE.equals(multiModal))
                    return "o4-mini";
                if (perplexity > 175 && perplexity < 200 && Boolean.TRUE.equals(multiModal))
                    return "gpt-4.1";
                return "o3-mini";
            });
    }
}
