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
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.metadata.MetadataKey;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import net.dv8tion.jda.api.entities.Message.Attachment;
import org.yaml.snakeyaml.Yaml;

public class AIManager {

    private String moderationApiUrl = Helpers.OPENAI_ENDPOINT_URLS.get("moderations");
    private String responsesApiUrl = Helpers.OPENAI_ENDPOINT_URLS.get("responses");
    private ConfigManager cm;
    private EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private Logger logger = Logger.getLogger("Vyrtuous");

    public AIManager(ConfigManager cm) {
        this.cm = cm.completeGetInstance();
    }

    public CompletableFuture<Long> completeCalculateMaxOutputTokens(String model, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Encoding encoding = registry.getEncoding("cl100k_base").orElseThrow(() ->
                    new IllegalStateException("Fallback encoding 'cl100k_base' not available"));
                long promptTokens = encoding.encode(prompt).size();
                ModelInfo outputInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(model);
                long outputLimit = outputInfo != null ? outputInfo.upperLimit() : 4096;
                long tokens = Math.max(1, outputLimit - promptTokens - 20);
                if (tokens < 16) {
                    tokens = 16;
                }
                return tokens;
            } catch (Exception e) {
                System.out.println("Tokenizer not available for model: " + model + ", using fallback.");
                return 0L;
            }
        });
    }

    public CompletableFuture<ResponseObject> completeChat(String fullContent, String previousResponseId, String model) {
        return completeInputToTextRequestBody(fullContent, previousResponseId, model)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody, responsesApiUrl)
            );
    }

    public CompletableFuture<Map<String, Object>> completeInputToModerationRequestBody(String fullContent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return completeFormRequestBody(
                    fullContent,
                    CompletableFuture.completedFuture(Helpers.OPENAI_CHAT_MODERATION_MODEL),
                    Helpers.OPENAI_CHAT_MODERATION_RESPONSE_FORMAT,
                    Helpers.OPENAI_CHAT_MODERATION_STORE,
                    Helpers.OPENAI_CHAT_MODERATION_STREAM,
                    Helpers.OPENAI_CHAT_MODERATION_SYS_INPUT,
                    Helpers.OPENAI_CHAT_MODERATION_TEMPERATURE,
                    Helpers.OPENAI_CHAT_MODERATION_TOP_P,
                    null
                );
            } catch (Exception ioe) {
                CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                failed.completeExceptionally(ioe);
                return failed;
            }
        }).thenCompose(result -> result);
    }

    public CompletableFuture<String> completeResolveModel(String content, Boolean multiModal) {
        return completePerplexity(content)
            .thenCompose(responseObject -> {
                return responseObject.completeGetPerplexity().thenApply(responsePerplexity -> {
                    Integer perplexity = (Integer) responsePerplexity;
                    if (perplexity < 100) {
                        return "gpt-4.1-nano";
                    } else if (perplexity > 100 && perplexity < 150 && Boolean.TRUE.equals(multiModal)) {
                        return "o4-mini";
                    } else if (perplexity > 175 && perplexity < 200 && Boolean.TRUE.equals(multiModal)) {
                        return "gpt-4.1";
                    } else {
                        return "o3-mini";
                    }
                });
            });
    }

    private CompletableFuture<Map<String, Object>> completeInputToPerplexityRequestBody(String fullContent, Map<String, Object> format) {
            try {
                return completeFormRequestBody(
                        fullContent,
                        cm.completeGetConfigValue("openai_chat_model", String.class),
                        format,
                        false,
                        false,
                        "You determine how perplexing text is to you on a integer scale from 0 (not perplexing) to 200 (most perplexing.",
                        0.7f,
                        1.0f,
                        null
                );
            } catch (Exception ioe) {
                CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                failed.completeExceptionally(ioe);
                return failed;
            }
    }

    private CompletableFuture<ResponseObject> completePerplexity(String fullContent) {
        return completeInputToPerplexityRequestBody(fullContent, Helpers.OPENAI_RESPONSES_TEXT_PERPLEXITY)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody, responsesApiUrl)
            );
    }

    private CompletableFuture<Map<String, Object>> completeFormRequestBody(
            String fullContent,
            CompletableFuture<String> modelFuture,
            Map<String, Object> textFormat,
            boolean store,
            boolean stream,
            String instructions,
            float temperature,
            float top_p,
            String previousResponseId) {
        return modelFuture.thenApply(model -> {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("text", Map.of("format", textFormat));
            if (previousResponseId != null && !previousResponseId.isEmpty()) {
                requestBody.put("previous_response_id", previousResponseId);
            }
            List<Map<String, Object>> messagesList = new ArrayList<>();
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", "user");
            messageMap.put("content", fullContent);
            messagesList.add(messageMap);
            requestBody.put("input", messagesList);
            if (store) {
                LocalDateTime now = LocalDateTime.now();
                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put("timestamp", now.toString());
                requestBody.put("metadata", List.of(metadataMap));
            }
            long tokens = completeCalculateMaxOutputTokens(model, fullContent).join();
            ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
            if (contextInfo != null && contextInfo.status()) {
                requestBody.put("max_output_tokens", tokens);
            } else {
                requestBody.put("max_tokens", tokens);
            }
            return requestBody;
        });
    }

    private CompletableFuture<Map<String, Object>> completeFormModerationRequestBody(String fullContent) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", fullContent);
        return CompletableFuture.completedFuture(requestBody);
    }

    private CompletableFuture<Map<String, Object>> completeInputToTextRequestBody(String text, String previousResponseId, String model) {
        return CompletableFuture.supplyAsync(() -> null)
            .thenCompose(ignored -> {
                Map<String, Object> requestBody = new HashMap<>();
                if (previousResponseId != null) {
                    requestBody.put("previous_response_id", previousResponseId);
                }
                requestBody.put("model", model);
                List<Map<String, Object>> messagesList = new ArrayList<>();
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("role", "user");
                messageMap.put("content", text);
                messagesList.add(messageMap);
                requestBody.put("input", messagesList);
                if (Helpers.OPENAI_CHAT_STORE) {
                    LocalDateTime now = LocalDateTime.now();
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("user", model);
                    metadataMap.put("timestamp", now.toString());
                    requestBody.put("metadata", List.of(metadataMap));
                }
                return completeCalculateMaxOutputTokens(model, text)
                    .thenApply(calculatedTokens -> {
                        ModelInfo contextInfo = 
                            ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
                        if (contextInfo != null && contextInfo.status()) {
                            requestBody.put("max_output_tokens", calculatedTokens);
                        } else {
                            requestBody.put("max_tokens", calculatedTokens);
                        }
                        return requestBody;
                    });
            });
    }



    public CompletableFuture<ResponseObject> completeHttpsModeration(String fullContent) {
        return completeInputToModerationRequestBody(fullContent)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody, responsesApiUrl)
            );
    }

    public CompletableFuture<ResponseObject> completeModeration(String fullContent) {
        return completeFormModerationRequestBody(fullContent)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody, moderationApiUrl)
            );
    }

    private CompletableFuture<ResponseObject> completeRequestWithRequestBody(Map<String, Object> requestBody, String endpoint) {
        return cm.completeGetConfigValue("openai_api_key", String.class)
            .thenCompose(apiKey -> CompletableFuture.supplyAsync(() -> {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpPost post = new HttpPost(endpoint);
                    post.setHeader("Authorization", "Bearer " + apiKey);
                    post.setHeader("Content-Type", "application/json");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    post.setEntity(new StringEntity(jsonBody));
                    try (CloseableHttpResponse response = httpClient.execute(post)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                        if (statusCode >= 200 && statusCode < 300) {
                            Map<String, Object> responseMap = objectMapper.readValue(
                                responseBody,
                                new TypeReference<Map<String, Object>>() {}
                            );
                            ResponseObject responseObject = new ResponseObject(responseMap);
                            return responseObject;
                        } else {
                            logger.warning("OpenAI API returned status: " + statusCode);
                            return null;
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Request failed: " + e.getMessage());
                    return null;
                }
            }));
    }
}
