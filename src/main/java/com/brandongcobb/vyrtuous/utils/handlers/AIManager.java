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

//import com.brandongcobb.vyrtuous.Vyrtuous;
//import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
//import com.brandongcobb.vyrtuous.metadata.MetadataKey;
//import com.brandongcobb.vyrtuous.records.ModelInfo;
//import com.brandongcobb.vyrtuous.utils.handlers.*;
//import com.brandongcobb.vyrtuous.utils.inc.*;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.knuddels.jtokkit.Encodings;
//import com.knuddels.jtokkit.api.Encoding;
//import com.knuddels.jtokkit.api.EncodingRegistry;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.logging.Logger;
//import net.dv8tion.jda.api.entities.Message;
//import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.HttpEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//import net.dv8tion.jda.api.entities.Message.Attachment;
//import org.yaml.snakeyaml.Yaml;


import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.metadata.MetadataKey;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import com.brandongcobb.vyrtuous.utils.inc.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AIManager {
    private static final Logger logger = Logger.getLogger(AIManager.class.getName());

    private final String moderationApiUrl = Maps.OPENAI_ENDPOINT_URLS.get("moderations");
    private final String responseApiUrl   = Maps.OPENAI_ENDPOINT_URLS.get("responses");
    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    public CompletableFuture<Long> completeCalculateMaxOutputTokens(String model, String prompt) {
        logger.info(() -> "Entering completeCalculateMaxOutputTokens(model=" + model + ", promptLength=" + (prompt == null ? 0 : prompt.length()) + ")");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Encoding encoding = registry.getEncoding("cl100k_base")
                    .orElseThrow(() -> new IllegalStateException("Encoding cl100k_base not available"));
                long promptTokens = encoding.encode(prompt).size();
                ModelInfo outputInfo = Maps.OPENAI_RESPONSE_MODEL_OUTPUT_LIMITS.get(model);
                long outputLimit = outputInfo != null ? outputInfo.upperLimit() : 4096;
                long tokens = Math.max(1, outputLimit - promptTokens - 20);
                if (tokens < 16) tokens = 16;
                //logger.info(() -> "Tokens calculated: " + tokens + " (promptTokens=" + promptTokens + ", outputLimit=" + outputLimit + ")");
                return tokens;
            } catch (Exception e) {
                logger.warning("completeCalculateMaxOutputTokens fallback: " + e.getMessage());
                return 0L;
            }
        });
    }

    public CompletableFuture<ResponseObject> completeChat(String fullContent, String previousResponseId, String model) {
        logger.info(() -> "Entering completeChat(fullContentLength=" + (fullContent == null ? 0 : fullContent.length()) +
                          ", previousResponseId=" + previousResponseId + ", model=" + model + ")");
        return completeInputToTextRequestBody(fullContent, previousResponseId, model)
            .thenCompose(requestBody -> {
                logger.info(() -> "completeChat: requestBody prepared, sending to " + responseApiUrl);
                return completeRequestWithRequestBody(requestBody, responseApiUrl);
            })
            .thenApply(resp -> {
                if (resp == null) logger.warning("completeChat: received null ResponseObject");
                else logger.info("completeChat: received ResponseObject");
                return resp;
            });
    }

    public CompletableFuture<Map<String, Object>> completeInputToModerationRequestBody(String fullContent) {
        logger.info(() -> "Entering completeInputToModerationRequestBody(fullContentLength=" + (fullContent == null ? 0 : fullContent.length()) + ")");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return completeFormRequestBody(
                    fullContent,
                    ModelRegistry.OPENAI_MODERATION_MODEL.asString(),
                    Maps.OPENAI_MODERATION_RESPONSE_FORMAT,
                    ModelRegistry.OPENAI_MODERATION_RESPONSE_STORE.asBoolean(),
                    ModelRegistry.OPENAI_MODERATION_RESPONSE_STREAM.asBoolean(),
                    ModelRegistry.OPENAI_MODERATION_RESPONSE_SYS_INPUT.asString(),
                    ModelRegistry.OPENAI_MODERATION_RESPONSE_TEMPERATURE.asFloat(),
                    ModelRegistry.OPENAI_MODERATION_RESPONSE_TOP_P.asFloat(),
                    null
                );
            } catch (Exception ioe) {
                logger.warning("completeInputToModerationRequestBody failed: " + ioe.getMessage());
                CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
                failed.completeExceptionally(ioe);
                return failed;
            }
        }).thenCompose(result -> result);
    }

    public CompletableFuture<String> completeResolveModel(String content, Boolean multiModal, String model) {
        logger.info(() -> "Entering completeResolveModel(model=" + model + ", multiModal=" + multiModal + ")");
        return completePerplexity(content, model)
            .thenCompose(responseObject -> {
                if (responseObject == null) {
                    logger.warning("completeResolveModel: perplexity responseObject is null");
                    return CompletableFuture.completedFuture(model);
                }
                return responseObject.completeGetPerplexity()
                    .thenApply(responsePerplexity -> {
                        Integer perplexity = (Integer) responsePerplexity;
                        logger.info("Perplexity: " + perplexity);
                        if (perplexity < 100) return "gpt-4.1-nano";
                        if (perplexity < 150 && Boolean.TRUE.equals(multiModal)) return "o4-mini";
                        if (perplexity < 200 && Boolean.TRUE.equals(multiModal)) return "gpt-4.1";
                        return "o3-mini";
                    });
            });
    }

    private CompletableFuture<Map<String, Object>> completeInputToPerplexityRequestBody(
            String fullContent, Map<String, Object> format, String model) {
        logger.info(() -> "Entering completeInputToPerplexityRequestBody(model=" + model + ")");
        try {
            return completeFormRequestBody(
                fullContent,
                model,
                format,
                ModelRegistry.OPENAI_RESPONSE_STORE.asBoolean(),
                ModelRegistry.OPENAI_RESPONSE_STREAM.asBoolean(),
                "You determine how perplexing text is on a scale from 0 to 200.",
                ModelRegistry.OPENAI_RESPONSE_TEMPERATURE.asFloat(),
                ModelRegistry.OPENAI_RESPONSE_TOP_P.asFloat(),
                null
            );
        } catch (Exception ioe) {
            logger.warning("completeInputToPerplexityRequestBody failed: " + ioe.getMessage());
            CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
            failed.completeExceptionally(ioe);
            return failed;
        }
    }

    private CompletableFuture<ResponseObject> completePerplexity(String fullContent, String model) {
        logger.info(() -> "Entering completePerplexity(model=" + model + ")");
        return completeInputToPerplexityRequestBody(fullContent, Maps.OPENAI_RESPONSE_FORMAT_PERPLEXITY, model)
            .thenCompose(requestBody -> {
                logger.info("completePerplexity: requestBody prepared, sending to " + responseApiUrl);
                return completeRequestWithRequestBody(requestBody, responseApiUrl);
            })
            .thenApply(resp -> {
                if (resp == null) logger.warning("completePerplexity: received null ResponseObject");
                else logger.info("completePerplexity: received ResponseObject");
                return resp;
            });
    }

    private CompletableFuture<Map<String, Object>> completeFormRequestBody(
            String fullContent,
            String model,
            Map<String, Object> textFormat,
            boolean store,
            boolean stream,
            String instructions,
            float temperature,
            float top_p,
            String previousResponseId) {
        logger.info(() -> "Entering completeFormRequestBody(model=" + model + ", store=" + store + ", stream=" + stream + ")");
        return completeCalculateMaxOutputTokens(model, fullContent)
            .thenApplyAsync(tokens -> {
                logger.info(() -> "completeFormRequestBody: tokens=" + tokens);
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("text", Map.of("format", textFormat));
                if (previousResponseId != null) {
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
                    logger.info(() -> "Adding metadata timestamp=" + now);
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("timestamp", now.toString());
                    requestBody.put("metadata", List.of(metadataMap));
                }
                ModelInfo contextInfo = Maps.OPENAI_RESPONSE_MODEL_CONTEXT_LIMITS.get(model);
                if (contextInfo != null) {
                    requestBody.put("max_output_tokens", tokens);
                } else {
                    requestBody.put("max_tokens", tokens);
                }
                return requestBody;
            });
    }

    private CompletableFuture<Map<String, Object>> completeFormModerationRequestBody(String fullContent) {
        logger.info(() -> "Entering completeFormModerationRequestBody(fullContentLength=" + (fullContent == null ? 0 : fullContent.length()) + ")");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", fullContent);
        return CompletableFuture.completedFuture(requestBody);
    }

    private CompletableFuture<Map<String, Object>> completeInputToTextRequestBody(
            String text, String previousResponseId, String model) {
        logger.info(() -> "Entering completeInputToTextRequestBody(model=" + model + ")");
        return CompletableFuture.supplyAsync(() -> {
                // no-op, just to shift to async
                return null;
            })
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
                if (ModelRegistry.OPENAI_RESPONSE_STORE.asBoolean()) {
                    LocalDateTime now = LocalDateTime.now();
                    logger.info(() -> "Adding metadata timestamp=" + now);
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("user", model);
                    metadataMap.put("timestamp", now.toString());
                    requestBody.put("metadata", List.of(metadataMap));
                }
                return completeCalculateMaxOutputTokens(model, text)
                    .thenApply(calculatedTokens -> {
                        ModelInfo contextInfo = Maps.OPENAI_RESPONSE_MODEL_CONTEXT_LIMITS.get(model);
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
        logger.info(() -> "Entering completeHttpsModeration");
        return completeInputToModerationRequestBody(fullContent)
            .thenCompose(requestBody -> {
                logger.info("completeHttpsModeration: requestBody prepared, sending to " + responseApiUrl);
                return completeRequestWithRequestBody(requestBody, responseApiUrl);
            });
    }

    public CompletableFuture<ResponseObject> completeModeration(String fullContent) {
        logger.info(() -> "Entering completeModeration");
        return completeFormModerationRequestBody(fullContent)
            .thenCompose(requestBody -> {
                logger.info("completeModeration: requestBody prepared, sending to " + moderationApiUrl);
                return completeRequestWithRequestBody(requestBody, moderationApiUrl);
            });
    }

    private CompletableFuture<ResponseObject> completeRequestWithRequestBody(Map<String, Object> requestBody, String endpoint) {
        logger.info(() -> "Entering completeRequestWithRequestBody(endpoint=" + endpoint + ")");
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("OPENAI_API_KEY is not set");
            return CompletableFuture.failedFuture(new IllegalStateException("Missing OPENAI_API_KEY"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(endpoint);
                post.setHeader("Authorization", "Bearer " + apiKey);
                post.setHeader("Content-Type", "application/json");
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                logger.info(() -> "Request JSON: " + jsonBody);
                post.setEntity(new StringEntity(jsonBody));
                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                    logger.info(() -> "Response status: " + statusCode);
                    if (statusCode >= 200 && statusCode < 300) {
                        Map<String, Object> responseMap = objectMapper.readValue(
                            responseBody, new TypeReference<Map<String, Object>>() {}
                        );
                        logger.info(() -> "Response JSON parsed successfully");
                        return new ResponseObject(responseMap);
                    } else {
                        logger.warning("OpenAI API returned error status: " + statusCode);
                        throw new IOException("Status " + statusCode + ": " + responseBody);
                    }
                }
            } catch (Exception e) {
                logger.warning("Request failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
//public class AIManager {
//
//    private String moderationApiUrl = Maps.OPENAI_ENDPOINT_URLS.get("moderations");
//    private String responseApiUrl = Maps.OPENAI_ENDPOINT_URLS.get("response");
//    private EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
//    private Logger logger = Logger.getLogger("Vyrtuous");
//
//    public CompletableFuture<Long> completeCalculateMaxOutputTokens(String model, String prompt) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                Encoding encoding = registry.getEncoding("cl100k_base").orElseThrow(() ->
//                    new IllegalStateException("Fallback encoding 'cl100k_base' not available"));
//                long promptTokens = encoding.encode(prompt).size();
//                ModelInfo outputInfo = Maps.OPENAI_RESPONSE_MODEL_OUTPUT_LIMITS.get(model);
//                long outputLimit = outputInfo != null ? outputInfo.upperLimit() : 4096;
//                long tokens = Math.max(1, outputLimit - promptTokens - 20);
//                if (tokens < 16) {
//                    tokens = 16;
//                }
//                return tokens;
//            } catch (Exception e) {
//                System.out.println("Tokenizer not available for model: " + model + ", using fallback.");
//                return 0L;
//            }
//        });
//    }
//
//    public CompletableFuture<ResponseObject> completeChat(String fullContent, String previousResponseId, String model) {
//        return completeInputToTextRequestBody(fullContent, previousResponseId, model)
//            .thenCompose(requestBody ->
//                completeRequestWithRequestBody(requestBody, responseApiUrl)
//            );
//    }
//
//    public CompletableFuture<Map<String, Object>> completeInputToModerationRequestBody(String fullContent) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                return completeFormRequestBody(
//                    fullContent,
//                    ModelRegistry.OPENAI_MODERATION_MODEL.asString(),
//                    Maps.OPENAI_MODERATION_RESPONSE_FORMAT,
//                    ModelRegistry.OPENAI_MODERATION_RESPONSE_STORE.asBoolean(),
//                    ModelRegistry.OPENAI_MODERATION_RESPONSE_STREAM.asBoolean(),
//                    ModelRegistry.OPENAI_MODERATION_RESPONSE_SYS_INPUT.asString(),
//                    ModelRegistry.OPENAI_MODERATION_RESPONSE_TEMPERATURE.asFloat(),
//                    ModelRegistry.OPENAI_MODERATION_RESPONSE_TOP_P.asFloat(),
//                    null
//                );
//            } catch (Exception ioe) {
//                CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
//                failed.completeExceptionally(ioe);
//                return failed;
//            }
//        }).thenCompose(result -> result);
//    }
//
//    public CompletableFuture<String> completeResolveModel(String content, Boolean multiModal, String model) {
//        return completePerplexity(content, model)
//            .thenCompose(responseObject -> {
//                return responseObject.completeGetPerplexity().thenApply(responsePerplexity -> {
//                    Integer perplexity = (Integer) responsePerplexity;
//                    if (perplexity < 100) {
//                        return "gpt-4.1-nano";
//                    } else if (perplexity > 100 && perplexity < 150 && Boolean.TRUE.equals(multiModal)) {
//                        return "o4-mini";
//                    } else if (perplexity > 175 && perplexity < 200 && Boolean.TRUE.equals(multiModal)) {
//                        return "gpt-4.1";
//                    } else {
//                        return "o3-mini";
//                    }
//                });
//            });
//    }
//
//    private CompletableFuture<Map<String, Object>> completeInputToPerplexityRequestBody(String fullContent, Map<String, Object> format, String model) {
//            try {
//                return completeFormRequestBody(
//                        fullContent,
//                        model,
//                        format,
//                        ModelRegistry.OPENAI_RESPONSE_STORE.asBoolean(),
//                        ModelRegistry.OPENAI_RESPONSE_STREAM.asBoolean(),
//                        "You determine how perplexing text is to you on a integer scale from 0 (not perplexing) to 200 (most perplexing.",
//                        ModelRegistry.OPENAI_RESPONSE_TEMPERATURE.asFloat(),
//                        ModelRegistry.OPENAI_RESPONSE_TOP_P.asFloat(),
//                        null
//                );
//            } catch (Exception ioe) {
//                CompletableFuture<Map<String, Object>> failed = new CompletableFuture<>();
//                failed.completeExceptionally(ioe);
//                return failed;
//            }
//    }
//
//    private CompletableFuture<ResponseObject> completePerplexity(String fullContent, String model) {
//        return completeInputToPerplexityRequestBody(fullContent, Maps.OPENAI_RESPONSE_FORMAT_PERPLEXITY, model)
//            .thenCompose(requestBody ->
//                completeRequestWithRequestBody(requestBody, responseApiUrl)
//            );
//    }
//
//    private CompletableFuture<Map<String, Object>> completeFormRequestBody(
//            String fullContent,
//            String model,
//            Map<String, Object> textFormat,
//            boolean store,
//            boolean stream,
//            String instructions,
//            float temperature,
//            float top_p,
//            String previousResponseId) {
//        CompletableFuture<Long> tokensFuture = completeCalculateMaxOutputTokens(model, fullContent);
//        return tokensFuture.thenApplyAsync(tokens -> {
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put("model", model);
//            requestBody.put("text", Map.of("format", textFormat));
//            if (previousResponseId != null && !previousResponseId.isEmpty()) {
//                requestBody.put("previous_response_id", previousResponseId);
//            }
//            List<Map<String, Object>> messagesList = new ArrayList<>();
//            Map<String, Object> messageMap = new HashMap<>();
//            messageMap.put("role", "user");
//            messageMap.put("content", fullContent);
//            messagesList.add(messageMap);
//            requestBody.put("input", messagesList);
//            if (store) {
//                LocalDateTime now = LocalDateTime.now();
//                Map<String, String> metadataMap = new HashMap<>();
//                metadataMap.put("timestamp", now.toString());
//                requestBody.put("metadata", List.of(metadataMap));
//            }
//            ModelInfo contextInfo = Maps.OPENAI_RESPONSE_MODEL_CONTEXT_LIMITS.get(model);
//            if (contextInfo != null && contextInfo.status()) {
//                requestBody.put("max_output_tokens", tokens);
//            } else {
//                requestBody.put("max_tokens", tokens);
//            }
//            return requestBody;
//        });
//    }
//
//    private CompletableFuture<Map<String, Object>> completeFormModerationRequestBody(String fullContent) {
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("input", fullContent);
//        return CompletableFuture.completedFuture(requestBody);
//    }
//
//    private CompletableFuture<Map<String, Object>> completeInputToTextRequestBody(String text, String previousResponseId, String model) {
//        return CompletableFuture.supplyAsync(() -> null)
//            .thenCompose(ignored -> {
//                Map<String, Object> requestBody = new HashMap<>();
//                if (previousResponseId != null) {
//                    requestBody.put("previous_response_id", previousResponseId);
//                }
//                requestBody.put("model", model);
//                List<Map<String, Object>> messagesList = new ArrayList<>();
//                Map<String, Object> messageMap = new HashMap<>();
//                messageMap.put("role", "user");
//                messageMap.put("content", text);
//                messagesList.add(messageMap);
//                requestBody.put("input", messagesList);
//                if (ModelRegistry.OPENAI_RESPONSE_STORE.asBoolean()) {
//                    LocalDateTime now = LocalDateTime.now();
//                    Map<String, String> metadataMap = new HashMap<>();
//                    metadataMap.put("user", model);
//                    metadataMap.put("timestamp", now.toString());
//                    requestBody.put("metadata", List.of(metadataMap));
//                }
//                return completeCalculateMaxOutputTokens(model, text)
//                    .thenApply(calculatedTokens -> {
//                        ModelInfo contextInfo = Maps.OPENAI_RESPONSE_MODEL_CONTEXT_LIMITS.get(model);
//                        if (contextInfo != null && contextInfo.status()) {
//                            requestBody.put("max_output_tokens", calculatedTokens);
//                        } else {
//                            requestBody.put("max_tokens", calculatedTokens);
//                        }
//                        return requestBody;
//                    });
//            });
//    }
//
//    public CompletableFuture<ResponseObject> completeHttpsModeration(String fullContent) {
//        return completeInputToModerationRequestBody(fullContent)
//            .thenCompose(requestBody ->
//                completeRequestWithRequestBody(requestBody, responseApiUrl)
//            );
//    }
//
//    public CompletableFuture<ResponseObject> completeModeration(String fullContent) {
//        return completeFormModerationRequestBody(fullContent)
//            .thenCompose(requestBody ->
//                completeRequestWithRequestBody(requestBody, moderationApiUrl)
//            );
//    }
//
//    private CompletableFuture<ResponseObject> completeRequestWithRequestBody(Map<String, Object> requestBody, String endpoint) {
//        String apiKey = System.getenv("OPENAI_API_KEY");
//        if (apiKey == null) {
//            return CompletableFuture.completedFuture(null);
//        }
//        return CompletableFuture.supplyAsync(() -> {
//            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//                HttpPost post = new HttpPost(endpoint);
//                post.setHeader("Authorization", "Bearer " + apiKey);
//                post.setHeader("Content-Type", "application/json");
//                ObjectMapper objectMapper = new ObjectMapper();
//                String jsonBody = objectMapper.writeValueAsString(requestBody);
//                System.out.println(jsonBody);
//                post.setEntity(new StringEntity(jsonBody));
//                try (CloseableHttpResponse response = httpClient.execute(post)) {
//                    int statusCode = response.getStatusLine().getStatusCode();
//                    String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
//                    if (statusCode >= 200 && statusCode < 300) {
//                        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
//                            new TypeReference<Map<String, Object>>() {});
//                        return new ResponseObject(responseMap);
//                    } else {
//                        logger.warning("OpenAI API returned status: " + statusCode);
//                        return null;
//                    }
//                }
//            } catch (IOException e) {
//                logger.warning("Request failed: " + e.getMessage());
//                return null;
//            }
//        });
//    }
//}
