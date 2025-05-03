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
    private static String apiUrl;
    private static long calculatedMaxTokens;
    private static long contextLimit;
    private static long customId;
    private long promptTokens;
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
    private static boolean openAIDefaultChatModerationUseHistory = false;
    private boolean addCompletionToHistory;
    private static String openAIAPIKey;
    private int i;
    private static MetadataContainer conversationContainer;
    private static ModelInfo contextInfo;
    private static ModelInfo outputInfo;
    private static MetadataContainer metadataContainer;
    private static EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    public static ResponseObject responseObject;
    private static Encoding encoding;

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

    private static CompletableFuture<ResponseObject> completeRequestWithRequestBody(
            MetadataContainer conversationContainer,
            Map<String, Object> requestBody) {
        
        return ConfigManager
            .completeGetNestedConfigValue("api_keys", "OpenAI")
            .thenCompose(openAIKeys -> openAIKeys.completeGetConfigStringValue("api_key"))
            .thenCompose(apiKey -> CompletableFuture.supplyAsync(() -> {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    String apiUrl = "https://api.openai.com/v1/responses";
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
                                new TypeReference<Map<String, Object>>() {});
                            return new ResponseObject(responseMap);
                        } else {
                            app.logger.warning("OpenAI API returned status: " + statusCode);
                            return null;
                        }
                    }
                } catch (IOException e) {
                    app.logger.warning("Request failed: " + e.getMessage());
                    return null;
                }
            }))
            .thenApply(responseObject -> {
                if (responseObject != null) {
                    MetadataKey<ResponseObject> responseObjectKey = new MetadataKey<>("responseObject", ResponseObject.class);
                    conversationContainer.put(responseObjectKey, responseObject);
                    app.logger.info("Stored OpenAI response in conversation container.");
                }
                return responseObject;
            });
    }

    public static CompletableFuture<ResponseObject> completeChat(MetadataContainer conversationContainer) {
        return completeConversationToTextRequestBody(conversationContainer)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(conversationContainer, requestBody)
            );
    }
    
    public static CompletableFuture<MetadataContainer> completeGetConversationContainer(long customId) {
        MetadataKey<MetadataContainer> conversationContainerKey =
            new MetadataKey<>("conversation_" + customId, MetadataContainer.class);
        return ConfigManager.completeGetApp().thenCompose(appInstance -> {
            MetadataContainer existing = appInstance.metadataContainer.get(conversationContainerKey);
            if (existing != null) {
                return CompletableFuture.completedFuture(existing);
            }
            MetadataContainer newContainer = new MetadataContainer();
            appInstance.metadataContainer.put(conversationContainerKey, newContainer);
            return CompletableFuture.completedFuture(newContainer);
        });
    }

    private static CompletableFuture<Map<String, Object>> completeConversationToTextRequestBody(
            MetadataContainer conversationContainer) {
    
        // Fetch the model from the ConfigManager
        return ConfigManager.completeGetConfigStringValue("openai_chat_model")
            .thenCompose(model -> {
                // Fetch configuration values for stream, temperature, and top_p
                CompletableFuture<Boolean> streamFuture = ConfigManager.completeGetConfigBooleanValue("openai_chat_stream");
                CompletableFuture<Object> tempFuture = ConfigManager.completeGetConfigObjectValue("openai_chat_temperature");
                CompletableFuture<Object> topPFuture = ConfigManager.completeGetConfigObjectValue("openai_chat_top_p");
                
                return CompletableFuture.allOf(streamFuture, tempFuture, topPFuture)
                    .thenCompose(v -> {
                        // Get the values from the futures
                        float temperature = Float.parseFloat(String.valueOf(tempFuture.join()));
                        float topP = Float.parseFloat(String.valueOf(topPFuture.join()));
                        boolean stream = streamFuture.join();
    
                        // Prepare the request body
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("model", model);
                        requestBody.put("temperature", temperature);
                        requestBody.put("top_p", topP);
                        requestBody.put("stream", stream);
    
                        // Fetch conversation text and role from the MetadataContainer
                        MetadataKey<String> textKey = new MetadataKey<>("conversation.text", String.class);
                        MetadataKey<String> roleKey = new MetadataKey<>("conversation.role", String.class);
                        String text = conversationContainer.get(textKey);
                        String role = conversationContainer.get(roleKey);
    
                        // Fallbacks if values are null
                        if (text == null) {
                            text = "";
                        }
                        if (role == null) {
                            role = "user";
                        }
    
                        // Prepare messages list with role and content
                        List<Map<String, Object>> messagesList = new ArrayList<>();
                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("role", role);
                        messageMap.put("content", text);
                        messagesList.add(messageMap);
                        requestBody.put("input", messagesList);
    
                        // Optional: Store metadata for OpenAI if configured
                        if (openAIDefaultChatCompletionStore) {
                            LocalDateTime now = LocalDateTime.now();
                            Map<String, String> metadataMap = new HashMap<>();
                            metadataMap.put("user", model);
                            metadataMap.put("timestamp", now.toString());
                            requestBody.put("metadata", List.of(metadataMap));
                        }
    
                        // Calculate the maximum output tokens
                        return completeCalculateMaxOutputTokens(model, text)
                            .thenApply(calculatedTokens -> {
                                ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
                                if (contextInfo != null && contextInfo.status()) {
                                    requestBody.put("max_output_tokens", calculatedTokens);
                                } else {
                                    requestBody.put("max_tokens", calculatedTokens);
                                }
                                return requestBody;
                            });
                    });
            });
    }

    public static CompletableFuture<Map<String, Object>> completeConversationToModerationRequestBody(
            MetadataContainer conversationContainer) {
    
        // Fetch the model from the ConfigManager
        return ConfigManager.completeGetConfigStringValue("openai_chat_model")
            .thenCompose(chatModel -> {
                try {
                    // Directly use the passed in conversationContainer
                    return formRequestBodyFromConversation(
                            conversationContainer,
                            openAIDefaultChatModerationModel,
                            openAIDefaultChatModerationResponseFormat,
                            openAIDefaultChatModerationStore,
                            openAIDefaultChatModerationStream,
                            openAIDefaultChatModerationSysInput,
                            openAIDefaultChatModerationTemperature,
                            openAIDefaultChatModerationTopP
                    );
                } catch (Exception ioe) {
                    // Handle any exceptions and complete the future exceptionally
                    CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(ioe);
                    return failed;
                }
            });
    }

    private static CompletableFuture<Map<String, Object>> formRequestBodyFromConversation(
            MetadataContainer conversationContainer,
            String model,
            Map<String, Object> textFormat,
            boolean store,
            boolean stream,
            String instructions,
            float temperature,
            float top_p) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("text", Map.of("format", textFormat));
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", top_p);
            requestBody.put("stream", stream);
            MetadataKey<String> textKey = new MetadataKey<>("conversation.text", String.class);
            MetadataKey<String> roleKey = new MetadataKey<>("conversation.role", String.class);
            String text = conversationContainer.get(textKey);
            String role = conversationContainer.get(roleKey);
            if(text == null) {
                text = "";
            }
            if(role == null) {
                role = "user";
            }
            List<Map<String, Object>> messagesList = new ArrayList<>();
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", role);
            messageMap.put("content", text);
            messagesList.add(messageMap);
            requestBody.put("input", messagesList);
            if (store) {
                LocalDateTime now = LocalDateTime.now();
                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put("user", model);
                metadataMap.put("timestamp", now.toString());
                requestBody.put("metadata", List.of(metadataMap));
            }
            long tokens = completeCalculateMaxOutputTokens(model, text).join();
            ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
            if (contextInfo != null && contextInfo.status()) {
                requestBody.put("max_output_tokens", tokens);
            } else {
                requestBody.put("max_tokens", tokens);
            }
            return requestBody;
        });
    }

    public static CompletableFuture<ResponseObject> completeModeration(MetadataContainer conversationContainer) {
        return completeConversationToModerationRequestBody(conversationContainer)
            .thenCompose(requestBody ->
                completeRequestWithRequestBody(conversationContainer, requestBody)
            );
    }

    public static List<String> splitLongResponse(String response, int limit) {
        List<String> outputChunks = new ArrayList<>();
        String[] parts = response.split("(?<=```)|(?=```)");
        boolean inCode = false;
        for (String part : parts) {
            if (part.equals("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                outputChunks.add("```" + part + "```");
                inCode = false;
            } else {
                for (int i = 0; i < part.length(); i += limit) {
                    int end = Math.min(i + limit, part.length());
                    outputChunks.add(part.substring(i, end));
                }
            }
        }
        return outputChunks;
    }
}
