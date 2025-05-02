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
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager.MessageContent;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import com.brandongcobb.vyrtuous.utils.inc.ModelRegistry;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.metadata.MetadataKey;
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
    private static Map<String, List<Map<String, Object>>> conversations;
    private static String openAIAPIKey;
    private int i;
    private static MetadataContainer conversationContainer;
    private static ModelInfo contextInfo;
    private static ModelInfo outputInfo;
    private static MetadataContainer metadataContainer;
    private CompletableFuture<List<MessageManager.MessageContent>> inputArray;
    private static EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private static Encoding encoding;

    public static CompletableFuture<Long> completeCalculateMaxOutputTokens(String model, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            Encoding encoding;
            try {
                encoding = registry.getEncodingForModel(model.replace('-', '_'))
                    .orElse(registry.getEncoding("cl200k_base").orElseThrow(() ->
                        new IllegalStateException("Fallback encoding 'cl200k_base' not available")));
                long promptTokens = encoding.encode(prompt).size();
                ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
                long contextLimit = contextInfo != null ? contextInfo.upperLimit() : 4096; // default fallback
                return Math.max(1, contextLimit - promptTokens - 20); // Ensure max output is always positive
            } catch (Exception e) {
                System.out.println("Tokenizer not available for model: " + model + ", using cl100k_base as fallback.");
                return 0L; // Return 0 in case of failure
            }
        });
    }

    private static CompletableFuture<Void> completeRequestWithRequestBody(long customId, Map<String, Object> requestBody) {
        return ConfigManager
            .completeGetNestedConfigValue("api_keys", "OpenAI")
            .thenCompose(openAIKeys -> openAIKeys.completeGetConfigStringValue("api_key"))
            .thenCompose(apiKey -> CompletableFuture.runAsync(() -> {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    String apiUrl = "https://api.openai.com/v1/responses";
                    HttpPost post = new HttpPost(apiUrl);
                    post.setHeader("Authorization", "Bearer " + apiKey);
                    post.setHeader("Content-Type", "application/json");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    post.setEntity(new StringEntity(jsonBody));
                    try (CloseableHttpResponse response = httpClient.execute(post)) {
                    }
                } catch (IOException e) {
                    app.logger.warning("Request failed: " + e.getMessage());
                }
            }));
    }

    private CompletableFuture<Void> completeLoadModerationIntoContainer(CompletableFuture<List<MessageContent>> inputArray, long customId) {
        return completeInputArrayToModerationRequestBody(inputArray).thenCompose(requestBody ->
            completeRequestWithRequestBody(customId, requestBody)
        );
    }

    private CompletableFuture<Void> completeLoadChatIntoContainer(CompletableFuture<List<MessageContent>> inputArray, long customId) {
        return completeInputArrayToTextRequestBody(inputArray).thenCompose(requestBody ->
            completeRequestWithRequestBody(customId, requestBody)
        );
    }

    public static CompletableFuture<String> completeChat(long customId, CompletableFuture<List<MessageContent>> inputArray) {
        return completeGetConversationContainer(customId)
            .thenCompose(conversationContainer ->
                completeInputArrayToTextRequestBody(inputArray)
                )
                .thenCompose(requestBody ->
                    completeRequestWithRequestBody(customId, requestBody)
                        .thenApply(ignored -> conversationContainer.get(new MetadataKey<>("output_content", String.class)))
            );
    }

    public static CompletableFuture<String> completeModeration(long customId, CompletableFuture<List<MessageContent>> inputArray) {
        return completeGetConversationContainer(customId)
            .thenCompose(conversationContainer ->
                completeInputArrayToModerationRequestBody(inputArray)
                    .thenCompose(requestBody ->
                        completeRequestWithRequestBody(customId, requestBody)
                            .thenApply(ignored -> {
                                MetadataKey<Boolean> flaggedKey = new MetadataKey<>("flagged", Boolean.class);
                                Boolean flagged = conversationContainer.get(flaggedKey);
                                if (Boolean.TRUE.equals(flagged)) {
                                    String[] keys = {
                                        "sexual",
                                        "sexual/minors",
                                        "harassment",
                                        "harassment/threatening",
                                        "hate",
                                        "hate/threatening",
                                        "illicit",
                                        "illicit/violent",
                                        "self-harm",
                                        "self-harm/intent",
                                        "self-harm/instructions",
                                        "violence",
                                        "violence/graphic"
                                    };
                                    Map<String, Boolean> reasonValues = new LinkedHashMap<>();
                                    for (String key : keys) {
                                        MetadataKey<Boolean> keyObj = new MetadataKey<>(key, Boolean.class);
                                        Boolean value = conversationContainer.get(keyObj);
                                        reasonValues.put(key, value != null && value);
                                    }
                                    String joinedReasons = reasonValues.entrySet().stream()
                                        .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.joining(", "));
                                    return "⚠️ Flagged for: " + joinedReasons;
                                }
                                return "✅ Not flagged.";
                            })
                    )
            )
            .exceptionally(ex -> {
                ex.printStackTrace();
                return "Error while processing request";
            });
    }


    /**
     * Extracts the previous response ID from the given ResponseObject,
     * storing it in a per‑conversation metadata container keyed by customId.
     *
     * @param customId         a unique conversation identifier
     * @param responseObject   a ResponseObject that provides the previous response ID
     * @return a CompletableFuture that, when complete, returns the previous response ID as a String
     */
    public static CompletableFuture<Map<String, Object>> formRequestBody(CompletableFuture<List<MessageContent>> inputArray,
                                                                          String model,
                                                                          Map<String, Object> text,
                                                                          boolean store,
                                                                          boolean stream,
                                                                          String instructions,
                                                                          float temperature,
                                                                          float top_p) throws IOException {
            return inputArray.thenCompose(messages ->
                CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> requestBody = new HashMap<>();
                    // example parameter settings:
                    requestBody.put("model", model);
                    requestBody.put("text", text);
                    requestBody.put("temperature", temperature);
                    requestBody.put("store", store);
                    requestBody.put("top_p", top_p);
                    List<Map<String, Object>> messagesList = new ArrayList<>();
                    // Collect token calculation futures for each message.
                    List<CompletableFuture<Void>> tokenFutures = new ArrayList<>();
                    for (MessageContent messageContent : messages) {
                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("role", messageContent.getType()); // e.g. "user" or "assistant"
                        messageMap.put("content", messageContent.getText());
                        messagesList.add(messageMap);
                        CompletableFuture<Void> tokenFuture = completeCalculateMaxOutputTokens(model, messageContent.getText())
                            .thenAccept(calculatedTokens -> {
                                ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
                                if (contextInfo != null && contextInfo.status()) {
                                    requestBody.put("max_completion_tokens", calculatedTokens);
                                } else {
                                    requestBody.put("max_tokens", calculatedTokens);
                                }
                            });
                        tokenFutures.add(tokenFuture);
                    }
                    requestBody.put("input", messagesList);
                    if (store) {
                        LocalDateTime now = LocalDateTime.now();
                        Map<String, String> metadataMap = new HashMap<>();
                        // Note: replace 'model' with appropriate user/conversation identifier if needed.
                        metadataMap.put("user", String.valueOf(model));
                        metadataMap.put("timestamp", String.valueOf(now));
                        requestBody.put("metadata", Collections.singletonList(metadataMap));
                    }
                    try {
                        encoding = registry.getEncodingForModel(model.replace('-', '_'))
                               .orElse(registry.getEncoding("cl200k_base").orElseThrow(() ->
                               new IllegalStateException("Fallback encoding 'cl200k_base' not available")));
                    } catch (Exception e) {
                        System.out.println("Tokenizer not available for model: " + model + ", using cl100k_base as fallback.");
                    }
                    ModelInfo outputInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(model);
                    // Wait for all token calculations to complete.
                    CompletableFuture.allOf(tokenFutures.toArray(new CompletableFuture[0])).join();
                    return requestBody;
                })
            );
    }

    public static CompletableFuture<MetadataContainer> completeGetConversationContainer(long customId) {
        MetadataKey<MetadataContainer> conversationContainerKey =
            new MetadataKey<>("conversation_" + customId, MetadataContainer.class);
        return ConfigManager.completeGetApp().thenCompose(app -> {
            MetadataContainer existing = app.metadataContainer.get(conversationContainerKey);
            if (existing != null) {
                return CompletableFuture.completedFuture(existing);
            }
            MetadataContainer newContainer = new MetadataContainer();
            app.metadataContainer.put(conversationContainerKey, newContainer);
            return CompletableFuture.completedFuture(newContainer);
        });
    }

    public static CompletableFuture<Map<String, Object>> completeInputArrayToTextRequestBody(CompletableFuture<List<MessageContent>> inputArray) {
        return ConfigManager.completeGetConfigStringValue("openai_chat_model")
            .thenCompose(model -> {
                ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(model);
                CompletableFuture<Boolean> streamFuture = ConfigManager.completeGetConfigBooleanValue("openai_chat_stream");
                CompletableFuture<Object> tempFuture = ConfigManager.completeGetConfigObjectValue("openai_chat_temperature");
                CompletableFuture<Object> topPFuture = ConfigManager.completeGetConfigObjectValue("openai_chat_top_p");
                return CompletableFuture.allOf(streamFuture, tempFuture, topPFuture)
                    .thenCompose(v -> {
                        float temperature = Float.parseFloat(String.valueOf(tempFuture.join()));
                        float topP = Float.parseFloat(String.valueOf(topPFuture.join()));
                        boolean stream = streamFuture.join();
                        try {
                            return formRequestBody(
                                inputArray,
                                model,
                                openAIDefaultChatCompletionResponseFormat,
                                openAIDefaultChatCompletionStore,
                                stream,
                                openAIDefaultChatCompletionSysInput,
                                temperature,
                                topP
                            );
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            Map<String, Object> empty = new HashMap<>();
                            return (CompletableFuture<Map<String, Object>>) empty;
                        }
                    });
            });
    }


    public static CompletableFuture<Map<String, Object>> completeInputArrayToModerationRequestBody(CompletableFuture<List<MessageContent>> inputArray) {
        return ConfigManager.completeGetConfigStringValue("openai_chat_model") // Assuming this is an async method.
            .thenCompose(chatModel -> {
                try {
                    ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS.get(chatModel);
                    return formRequestBody(
                        inputArray,
                        openAIDefaultChatModerationModel,
                        openAIDefaultChatModerationResponseFormat,
                        openAIDefaultChatModerationStore,
                        openAIDefaultChatModerationStream,
                        openAIDefaultChatModerationSysInput,
                        openAIDefaultChatModerationTemperature,
                        openAIDefaultChatModerationTopP
                    );
                } catch (IOException ioe) {
                    // Handle exception (perhaps return a failed CompletableFuture)
                    CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(ioe);
                    return failed;
                }
            });
    }

    public static List<String> splitLongResponse(String response, int limit) {
        List<String> outputChunks = new ArrayList<>();
        String[] parts = response.split("(?<=```)|(?=```)");
        boolean inCode = false;
        for (String part : parts) { if (part.equals("```")) {
                inCode = !inCode; // toggle code mode
                continue;
            }
            if (inCode) {
                outputChunks.add("```" + part + "```");
                inCode = false; // code block closed
            } else {
                // Split plain text into chunks
                for (int i = 0; i < part.length(); i += limit) {
                    int end = Math.min(i + limit, part.length());
                    outputChunks.add(part.substring(i, end));
                }
            }
        }
        return outputChunks;
    }
}
