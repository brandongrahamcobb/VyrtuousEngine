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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import net.dv8tion.jda.api.entities.Message.Attachment;
import java.util.AbstractMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Message;

public class AIManager {

    private static Vyrtuous app;
    private static long calculatedMaxTokens;
    private static long contextLimit;
    private long promptTokens;
    private static ModelInfo contextInfo;
    private static ModelInfo outputInfo;
    private static EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private static Encoding encoding;
    private static ResponseObject responseObject;

    private static boolean openAIDefaultChatCompletion = false;
    private static boolean openAIDefaultChatCompletionAddToHistory = false;
    private static long openAIDefaultChatCompletionMaxTokens = Helpers.parseCommaNumber("32,768");
    private static String openAIDefaultChatCompletionModel = "gpt-4.1-nano";
    private static long openAIDefaultChatCompletionNumber = 1;
    private static Map<String, Object> openAIDefaultChatCompletionResponseFormat = Helpers.OPENAI_CHAT_COMPLETION_RESPONSE_FORMAT;
    private static String openAIDefaultChatCompletionStop = "";
    private static boolean openAIDefaultChatCompletionStore = false;
    private static boolean openAIDefaultChatCompletionStream = false;
    private static String openAIDefaultChatCompletionSysInput = Helpers.OPENAI_CHAT_COMPLETION_SYS_INPUT;;
    private static float openAIDefaultChatCompletionTemperature = 0.7f;
    private static float openAIDefaultChatCompletionTopP = 1.0f;
    private static boolean openAIDefaultChatCompletionUseHistory = false;
    private static boolean openAIDefaultChatModeration = true;
    private static boolean openAIDefaultChatModerationAddToHistory = false;
    private static long openAIDefaultChatModerationMaxTokens = Helpers.parseCommaNumber("32,768");
    private static String openAIDefaultChatModerationModel = "gpt-4.1-nano";
    private static long openAIDefaultChatModerationNumber = 1;
    private static Map<String, Object> openAIDefaultChatModerationResponseFormat = Helpers.OPENAI_CHAT_MODERATION_RESPONSE_FORMAT;
    private static String openAIDefaultChatModerationStop = "";
    private static boolean openAIDefaultChatModerationStore = false;
    private static boolean openAIDefaultChatModerationStream = false;
    private static String openAIDefaultChatModerationSysInput = "All incoming data is subject to moderation. Protect your backend by flagging a message if it is unsuitable for a public community.";
    private static float openAIDefaultChatModerationTemperature = 0.7f;
    private static float openAIDefaultChatModerationTopP = 1.0f;
    
    public static CompletableFuture<Long> completeCalculateMaxOutputTokens(String model, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Encoding encoding = registry.getEncoding("cl100k_base").orElseThrow(() ->
                    new IllegalStateException("Fallback encoding 'cl100k_base' not available"));
                long promptTokens = encoding.encode(prompt).size();
                ModelInfo outputInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(model);
                long outputLimit = outputInfo != null ? outputInfo.upperLimit() : 4096; // default fallback
                long tokens = Math.max(1, outputLimit - promptTokens - 20); // Ensure positive value
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

    public static CompletableFuture<ResponseObject> completeChat(String fullContent, String previousResponseId, String model) {
        return completeInputToTextRequestBody(fullContent, previousResponseId, model)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody)
            );
    }

    public static CompletableFuture<Map<String, Object>> completeInputToModerationRequestBody(String fullContent) {
        return ConfigManager.completeGetConfigStringValue("openai_chat_model")
            .thenCompose(chatModel -> {
                try {
                    return completeFormRequestBody(
                            fullContent,
                            openAIDefaultChatModerationModel,
                            openAIDefaultChatModerationResponseFormat,
                            openAIDefaultChatModerationStore,
                            openAIDefaultChatModerationStream,
                            openAIDefaultChatModerationSysInput,
                            openAIDefaultChatModerationTemperature,
                            openAIDefaultChatModerationTopP,
                            null
                    );
                } catch (Exception ioe) {
                    // Handle any exceptions and complete the future exceptionally
                    CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(ioe);
                    return failed;
                }
            });
    }

    public static CompletableFuture<String> completeResolveModel(String content, Boolean multiModal) {
        return ConfigManager.completeGetConfigStringValue("openai_chat_model")
            .thenCompose(model ->
                completePerplexity(content)
            )
            .thenCompose((ResponseObject responseObject) ->
                responseObject.completeGetPerplexity().thenApply(responsePerplexity -> {
                    Double perplexity = (Double) responsePerplexity;
                    if (perplexity < 1.0d) {
                        return "gpt-4.1-nano";
                    } else if (perplexity > 1.0d && perplexity < 1.5d && Boolean.TRUE.equals(multiModal)) {
                        return "o4-mini";
                    } else if (perplexity > 1.75d && perplexity < 2.0d && Boolean.TRUE.equals(multiModal)) {
                        return "gpt-4.1";
                    } else {
                        return "o3-mini";
                    }
                })
            );
    }

    public static CompletableFuture<Map<String, Object>> completeInputToPerplexityRequestBody(String fullContent, Map<String, Object> format) {
        return ConfigManager.completeGetConfigStringValue("openai_chat_model")
            .thenCompose(chatModel -> {
                try {
                    return completeFormRequestBody(
                            fullContent,
                            chatModel,
                            format,
                            false,
                            false,
                            "You determine how perplexing text is to you on a float scale from 0 (not perplexing) to 2 (most perplexing.",
                            0.7f,
                            1.0f,
                            null
                    );
                } catch (Exception ioe) {
                    // Handle any exceptions and complete the future exceptionally
                    CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(ioe);
                    return failed;
                }
            });
    }

    public static CompletableFuture<ResponseObject> completePerplexity(String fullContent) {
        return completeInputToPerplexityRequestBody(fullContent, Helpers.OPENAI_RESPONSES_TEXT_PERPLEXITY)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody)
            );
    }

    private static CompletableFuture<Map<String, Object>> completeFormRequestBody(
            String fullContent,
            String model,
            Map<String, Object> textFormat,
            boolean store,
            boolean stream,
            String instructions,
            float temperature,
            float top_p,
            String previousResponseId) {
    
        Map<String, Object> requestBody = new HashMap<>();
    
        // Set required fields
        requestBody.put("model", model);
        requestBody.put("text", Map.of("format", textFormat));
 //       if (Helpers.OPEN_CHAT_MODELS.get("deprecated").contains(model) 
 //       requestBody.put("temperature", temperature);
 //       requestBody.put("top_p", top_p);
 //       requestBody.put("stream", stream);
    
        // Optionally include previous response ID
        if (previousResponseId != null && !previousResponseId.isEmpty()) {
            requestBody.put("previous_response_id", previousResponseId);
        }
    
        // Build messages list
        List<Map<String, Object>> messagesList = new ArrayList<>();
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("content", fullContent);
        messagesList.add(messageMap);
        requestBody.put("input", messagesList);
    
        // Optionally add metadata
        if (store) {
            LocalDateTime now = LocalDateTime.now();
            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put("user", model);
            metadataMap.put("timestamp", now.toString());
            requestBody.put("metadata", List.of(metadataMap));
        }
    
        // Calculate token limits
        long tokens = completeCalculateMaxOutputTokens(model, fullContent).join();
        ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
    
        if (contextInfo != null && contextInfo.status()) {
            requestBody.put("max_output_tokens", tokens);
        } else {
            requestBody.put("max_tokens", tokens);
        }
    
        return CompletableFuture.completedFuture(requestBody);
    }

    private static CompletableFuture<Map<String, Object>> completeInputToTextRequestBody(
            String text, String previousResponseId, String model) {
        // Start with an async task that returns null.
        return CompletableFuture.supplyAsync(() -> null)
            // then flatten with thenCompose to build your request body
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
    
                if (openAIDefaultChatCompletionStore) {
                    LocalDateTime now = LocalDateTime.now();
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("user", model);
                    metadataMap.put("timestamp", now.toString());
                    requestBody.put("metadata", List.of(metadataMap));
                }
    
                // Complete the chain by calculating tokens, then adding it to the request body.
                return completeCalculateMaxOutputTokens(model, text)
                    .thenApply(calculatedTokens -> {
                        ModelInfo contextInfo = 
                            ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
                        // Use one key or another depending on the model info.
                        if (contextInfo != null && contextInfo.status()) {
                            requestBody.put("max_output_tokens", calculatedTokens);
                        } else {
                            requestBody.put("max_tokens", calculatedTokens);
                        }
                        return requestBody;
                    });
            });
    }



    public static CompletableFuture<ResponseObject> completeModeration(String fullContent) {
        return completeInputToModerationRequestBody(fullContent)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(requestBody)
            );
    }

    private static CompletableFuture<ResponseObject> completeRequestWithRequestBody(Map<String, Object> requestBody) {
        return ConfigManager
            .completeGetNestedConfigValue("api_keys", "OpenAI")
            .thenCompose(openAIKeys -> openAIKeys.completeGetConfigStringValue("api_key"))
            .thenCompose(apiKey -> CompletableFuture.supplyAsync(() -> {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    String apiUrl = "https://api.openai.com/v1/responses"; // Verify this endpoint is correct
                    HttpPost post = new HttpPost(apiUrl);
                    post.setHeader("Authorization", "Bearer " + apiKey);
                    post.setHeader("Content-Type", "application/json");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    post.setEntity(new StringEntity(jsonBody));
                    try (CloseableHttpResponse response = httpClient.execute(post)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                        System.out.println(responseBody);
                        if (statusCode >= 200 && statusCode < 300) {
                            Map<String, Object> responseMap = objectMapper.readValue(
                                responseBody,
                                new TypeReference<Map<String, Object>>() {}
                            );
                            ResponseObject responseObject = new ResponseObject(responseMap);
                            return responseObject;
                        } else {
                            app.logger.warning("OpenAI API returned status: " + statusCode);
                            return null;
                        }
                    }
                } catch (IOException e) {
                    app.logger.warning("Request failed: " + e.getMessage());
                    return null;
                }
            }));
    }
}
