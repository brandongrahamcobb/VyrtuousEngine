/*  ArtificialIntelligence.java 
 *
 *  Copyright (C) 2024  github.com/brandongrahamcobb
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
 *  aInteger with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.metadata.*;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResponseObject {

    private final MetadataContainer container;

    public ResponseObject(Map<String, Object> responseMap) {
        // Initialize the instance variable (avoid shadowing)
        this.container = new MetadataContainer();

        // Set up mandatory keys
        MetadataKey<String> idKey = new MetadataKey<>("id", String.class);
        MetadataKey<String> objectKey = new MetadataKey<>("object", String.class);

        String requestId = (String) responseMap.get("id");
        System.out.println(requestId);
        String requestObject = (String) responseMap.get("object");

        // If the "object" value is missing, throw an exception or handle it gracefully
        if (requestObject == null) {
            throw new NullPointerException("The response map is missing the mandatory 'object' field.");
        }

        container.put(idKey, requestId);
        container.put(objectKey, requestObject);

        // Refactored to use a switch on a non-null value and include breaks
        switch (requestObject) {
            case "chat.completion": {
                MetadataKey<Integer> completionCreatedKey = new MetadataKey<>("created", Integer.class);
                Integer completionCreated = (Integer) responseMap.get("created");
                container.put(completionCreatedKey, completionCreated);

                MetadataKey<String> completionModelKey = new MetadataKey<>("model", String.class);
                String completionModel = (String) responseMap.get("model");
                container.put(completionModelKey, completionModel);

                List<Map<String, Object>> completionChoices = (List<Map<String, Object>>) responseMap.get("choices");
                if (completionChoices != null && !completionChoices.isEmpty()) {
                    Map<String, Object> completionChoice = completionChoices.get(0);

                    Map<String, String> completionMessage = (Map<String, String>) completionChoice.get("message");
                    MetadataKey<String> completionRoleKey = new MetadataKey<>("role", String.class);
                    String completionRole = completionMessage != null ? completionMessage.get("role") : null;
                    container.put(completionRoleKey, completionRole);

                    MetadataKey<String> completionContentKey = new MetadataKey<>("content", String.class);
                    String completionContent = completionMessage != null ? completionMessage.get("content") : null;
                    container.put(completionContentKey, completionContent);
                    MetadataKey<String> completionFinishReasonKey = new MetadataKey<>("finish_reason", String.class);
                    String completionFinishReason = (String) completionChoice.get("finish_reason");
                    container.put(completionFinishReasonKey, completionFinishReason);

                    MetadataKey<Integer> completionIndexKey = new MetadataKey<>("index", Integer.class);
                    Integer completionIndex = (Integer) completionChoice.get("index");
                    container.put(completionIndexKey, completionIndex);
                }

                Map<String, Integer> completionUsage = (Map<String, Integer>) responseMap.get("usage");
                if (completionUsage != null) {
                    MetadataKey<Integer> completionTotalTokensKey = new MetadataKey<>("total_tokens", Integer.class);
                    Integer completionTotalTokens = completionUsage.get("total_tokens");
                    container.put(completionTotalTokensKey, completionTotalTokens);

                    MetadataKey<Integer> completionPromptTokensKey = new MetadataKey<>("prompt_tokens", Integer.class);
                    Integer completionPromptTokens = completionUsage.get("prompt_tokens");
                    container.put(completionPromptTokensKey, completionPromptTokens);

                    MetadataKey<Integer> completionCompletionTokensKey = new MetadataKey<>("completion_tokens", Integer.class);
                    Integer completionCompletionTokens = completionUsage.get("completion_tokens");
                    container.put(completionCompletionTokensKey, completionCompletionTokens);
                }
                break;
            }
            case "moderation": {
                MetadataKey<Integer> moderationCreatedKey = new MetadataKey<>("created", Integer.class);
                Integer moderationCreated = (Integer) responseMap.get("created");
                container.put(moderationCreatedKey, moderationCreated);
                MetadataKey<String> moderationModelKey = new MetadataKey<>("model", String.class);
                String moderationModel = (String) responseMap.get("model");
                container.put(moderationModelKey, moderationModel);

                List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> result = results.get(0);

                    MetadataKey<Boolean> flaggedKey = new MetadataKey<>("flagged", Boolean.class);
                    Boolean moderationFlagged = (Boolean) result.get("flagged");
                    container.put(flaggedKey, moderationFlagged);

                    Map<String, Boolean> categories = (Map<String, Boolean>) result.get("categories");
                    if (categories != null) {
                        MetadataKey<Boolean> sexualKey = new MetadataKey<>("sexual", Boolean.class);
                        Boolean moderationSexual = categories.get("sexual");
                        container.put(sexualKey, moderationSexual);

                        MetadataKey<Boolean> sexualMinorsKey = new MetadataKey<>("sexual/minors", Boolean.class);
                        Boolean moderationSexualMinors = categories.get("sexual/minors");
                        container.put(sexualMinorsKey, moderationSexualMinors);

                        MetadataKey<Boolean> harassmentKey = new MetadataKey<>("harassment", Boolean.class);
                        Boolean moderationHarassment = categories.get("harassment");
                        container.put(harassmentKey, moderationHarassment);

                        MetadataKey<Boolean> harassmentThreateningKey = new MetadataKey<>("harassment/threatening", Boolean.class);
                        Boolean moderationHarassmentThreatening = categories.get("harassment/threatening");
                        container.put(harassmentThreateningKey, moderationHarassmentThreatening);

                        MetadataKey<Boolean> hateKey = new MetadataKey<>("hate", Boolean.class);
                        Boolean moderationHate = categories.get("hate");
                        container.put(hateKey, moderationHate);

                        MetadataKey<Boolean> hateThreateningKey = new MetadataKey<>("hate/threatening", Boolean.class);
                        Boolean moderationHateThreatening = categories.get("hate/threatening");
                        container.put(hateThreateningKey, moderationHateThreatening);

                        MetadataKey<Boolean> illicitKey = new MetadataKey<>("illicit", Boolean.class);
                        Boolean moderationIllicit = categories.get("illicit");
                        container.put(illicitKey, moderationIllicit);

                        // Note: watch out for extra spaces in key names
                        MetadataKey<Boolean> illicitViolentKey = new MetadataKey<>("illicit/violent", Boolean.class);
                        Boolean moderationIllicitViolent = categories.get("illicit/violent");
                        container.put(illicitViolentKey, moderationIllicitViolent);

                        MetadataKey<Boolean> selfHarmKey = new MetadataKey<>("self-harm", Boolean.class);
                        Boolean moderationSelfHarm = categories.get("self-harm");
                        container.put(selfHarmKey, moderationSelfHarm);

                        MetadataKey<Boolean> selfHarmIntentKey = new MetadataKey<>("self-harm/intent", Boolean.class);
                        Boolean moderationSelfHarmIntent = categories.get("self-harm/intent");
                        container.put(selfHarmIntentKey, moderationSelfHarmIntent);

                        MetadataKey<Boolean> selfHarmInstructionsKey = new MetadataKey<>("self-harm/instructions", Boolean.class);
                        Boolean moderationSelfHarmInstructions = categories.get("self-harm/instructions");
                        container.put(selfHarmInstructionsKey, moderationSelfHarmInstructions);

                        MetadataKey<Boolean> violenceKey = new MetadataKey<>("violence", Boolean.class);
                        Boolean moderationViolence = categories.get("violence");
                        container.put(violenceKey, moderationViolence);

                        MetadataKey<Boolean> violenceGraphicKey = new MetadataKey<>("violence/graphic", Boolean.class);
                        Boolean moderationViolenceGraphic = categories.get("violence/graphic");
                        container.put(violenceGraphicKey, moderationViolenceGraphic);
                    }

                    Map<String, String> categoryScores = (Map<String, String>) result.get("category_scores");
                    if (categoryScores != null) {
                        MetadataKey<String> sexualScoreKey = new MetadataKey<>("sexual", String.class);
                        String moderationSexualScore = categoryScores.get("sexual");
                        container.put(sexualScoreKey, moderationSexualScore);

                        MetadataKey<String> sexualMinorsScoreKey = new MetadataKey<>("sexual/minors", String.class);
                        String moderationSexualMinorsScore = categoryScores.get("sexual/minors");
                        container.put(sexualMinorsScoreKey, moderationSexualMinorsScore);

                        MetadataKey<String> harassmentScoreKey = new MetadataKey<>("harassment", String.class);
                        String moderationHarassmentScore = categoryScores.get("harassment");
                        container.put(harassmentScoreKey, moderationHarassmentScore);

                        MetadataKey<String> harassmentThreateningScoreKey = new MetadataKey<>("harassment/threatening", String.class);
                        String moderationHarassmentThreateningScore = categoryScores.get("harassment/threatening");
                        container.put(harassmentThreateningScoreKey, moderationHarassmentThreateningScore);

                        MetadataKey<String> hateScoreKey = new MetadataKey<>("hate", String.class);
                        String moderationHateScore = categoryScores.get("hate");
                        container.put(hateScoreKey, moderationHateScore);

                        MetadataKey<String> hateThreateningScoreKey = new MetadataKey<>("hate/threatening", String.class);
                        String moderationHateThreateningScore = categoryScores.get("hate/threatening");
                        container.put(hateThreateningScoreKey, moderationHateThreateningScore);

                        MetadataKey<String> illicitScoreKey = new MetadataKey<>("illicit", String.class);
                        String moderationIllicitScore = categoryScores.get("illicit");
                        container.put(illicitScoreKey, moderationIllicitScore);

                        MetadataKey<String> illicitViolentScoreKey = new MetadataKey<>("illicit/violent", String.class);
                        String moderationIllicitViolentScore = categoryScores.get("illicit/violent");
                        container.put(illicitViolentScoreKey, moderationIllicitViolentScore);

                        MetadataKey<String> selfHarmScoreKey = new MetadataKey<>("self-harm", String.class);
                        String moderationSelfHarmScore = categoryScores.get("self-harm");
                        container.put(selfHarmScoreKey, moderationSelfHarmScore);

                        MetadataKey<String> selfHarmIntentScoreKey = new MetadataKey<>("self-harm/intent", String.class);
                        String moderationSelfHarmIntentScore = categoryScores.get("self-harm/intent");
                        container.put(selfHarmIntentScoreKey, moderationSelfHarmIntentScore);

                        MetadataKey<String> selfHarmInstructionsScoreKey = new MetadataKey<>("self-harm/instructions", String.class);
                        String moderationSelfHarmInstructionsScore = categoryScores.get("self-harm/instructions");
                        container.put(selfHarmInstructionsScoreKey, moderationSelfHarmInstructionsScore);

                        MetadataKey<String> violenceScoreKey = new MetadataKey<>("violence", String.class);
                        String moderationViolenceScore = categoryScores.get("violence");
                        container.put(violenceScoreKey, moderationViolenceScore);

                        MetadataKey<String> violenceGraphicScoreKey = new MetadataKey<>("violence/graphic", String.class);
                        String moderationViolenceGraphicScore = categoryScores.get("violence/graphic");
                        container.put(violenceGraphicScoreKey, moderationViolenceGraphicScore);
                    }

                    Map<String, Object> categoryAppliedInputTypes = (Map<String, Object>) result.get("category_applied_input_types");
                    if (categoryAppliedInputTypes != null) {
                        // For each category, extract and store the applied input types as needed.
                        MetadataKey<List<String>> sexualInputKey = new MetadataKey<>("sexual", List.class);
                        List<String> moderationSexualAppliedType = (List<String>) categoryAppliedInputTypes.get("sexual");
                        container.put(sexualInputKey, moderationSexualAppliedType);

                        // ... Similar handling for the rest of the keys.
                    }
                }
                break;
            }
            case "response": {
                MetadataKey<String> responsesIdKey = new MetadataKey<>("id", String.class);
                String responsesId = (String) responseMap.get("id");
                container.put(responsesIdKey, responsesId);

                MetadataKey<String> responsesObjectKey = new MetadataKey<>("object", String.class);
                String responsesObject = (String) responseMap.get("object");
                container.put(responsesObjectKey, responsesObject);

                MetadataKey<Integer> responsesCreatedAtKey = new MetadataKey<>("created_at", Integer.class);
                Integer responsesCreatedAt = (Integer) responseMap.get("created_at");
                container.put(responsesCreatedAtKey, responsesCreatedAt);

                MetadataKey<String> responsesStatusKey = new MetadataKey<>("status", String.class);
                String responsesStatus = (String) responseMap.get("status");
                container.put(responsesStatusKey, responsesStatus);

                MetadataKey<String> responsesErrorKey = new MetadataKey<>("error", String.class);
                String responsesError = (String) responseMap.get("error");
                container.put(responsesErrorKey, responsesError);

                MetadataKey<String> responsesIncompleteDetailsReasonKey = new MetadataKey<>("reason", String.class);
                // It looks like the incomplete details might be nested—make sure you get the right field.
                Map<String, String> responsesIncompleteDetails = (Map<String, String>) responseMap.get("incomplete_details");
                String reason = responsesIncompleteDetails != null ? responsesIncompleteDetails.get("reason") : null;
                container.put(responsesIncompleteDetailsReasonKey, reason);

                MetadataKey<String> responsesInstructionsKey = new MetadataKey<>("instructions", String.class);
                String responsesInstructions = (String) responseMap.get("instructions");
                container.put(responsesInstructionsKey, responsesInstructions);

                MetadataKey<Integer> responsesMaxOutputTokensKey = new MetadataKey<>("max_output_tokens", Integer.class);
                Integer responsesMaxOutputTokens = (Integer) responseMap.get("max_output_tokens");
                container.put(responsesMaxOutputTokensKey, responsesMaxOutputTokens);
                MetadataKey<String> responsesModelKey = new MetadataKey<>("model", String.class);
                String responsesModel = (String) responseMap.get("model");
                container.put(responsesModelKey, responsesModel);

                MetadataKey<Boolean> responsesParallelToolCallsKey = new MetadataKey<>("parallel_tool_calls", Boolean.class);
                Boolean responsesParallelToolCalls = (Boolean) responseMap.get("parallel_tool_calls");
                container.put(responsesParallelToolCallsKey, responsesParallelToolCalls);

                MetadataKey<String> responsesPreviousResponseIdKey = new MetadataKey<>("previous_response_id", String.class);
                String responsesPreviousResponseId = (String) responseMap.get("previous_response_id");
                container.put(responsesPreviousResponseIdKey, responsesPreviousResponseId);

                MetadataKey<String> responsesReasoningEffortKey = new MetadataKey<>("effort", String.class);
                MetadataKey<String> responsesReasoningSummaryKey = new MetadataKey<>("summary", String.class);
                Map<String, String> responsesReasoning = (Map<String, String>) responseMap.get("reasoning");
                if (responsesReasoning != null) {
                    String responsesReasoningEffort = responsesReasoning.get("effort");
                    container.put(responsesReasoningEffortKey, responsesReasoningEffort);

                    String responsesReasoningSummary = responsesReasoning.get("summary");
                    container.put(responsesReasoningSummaryKey, responsesReasoningSummary);
                }

                MetadataKey<Double> responsesTemperatureKey = new MetadataKey<>("temperature", Double.class);
                Double responsesTemperature = (Double) responseMap.get("temperature");
                container.put(responsesTemperatureKey, responsesTemperature);

                MetadataKey<Map<String, Object>> responsesTextFormatKey = new MetadataKey<>("text_format", Map.class);
                // Ensure to retrieve the right nested field; here it previously was called "text" in your version.
                Map<String, Object> responsesTextFormat = (Map<String, Object>) responseMap.get("text");
                container.put(responsesTextFormatKey, responsesTextFormat);

                MetadataKey<String> responsesToolChoiceKey = new MetadataKey<>("tool_choice", String.class);
                String responsesToolChoice = (String) responseMap.get("tool_choice");
                container.put(responsesToolChoiceKey, responsesToolChoice);

                MetadataKey<List<String>> responsesToolsKey = new MetadataKey<>("tools", List.class);
                List<String> responsesTools = (List<String>) responseMap.get("tools");
                container.put(responsesToolsKey, responsesTools);

                MetadataKey<Double> responsesTopPKey = new MetadataKey<>("top_p", Double.class);
                Double responsesTopP = (Double) responseMap.get("top_p");
                container.put(responsesTopPKey, responsesTopP);

                MetadataKey<String> responsesTruncationKey = new MetadataKey<>("truncation", String.class);
                String responsesTruncation = (String) responseMap.get("truncation");
                container.put(responsesTruncationKey, responsesTruncation);

                MetadataKey<Integer> responsesTotalTokensKey = new MetadataKey<>("total_tokens", Integer.class);
                Map<String, Object> responsesUsage = (Map<String, Object>) responseMap.get("usage");
                if (responsesUsage != null) {
                    Integer responsesTotalTokens = (Integer) responsesUsage.get("total_tokens");
                    container.put(responsesTotalTokensKey, responsesTotalTokens);
                }

                MetadataKey<String> responsesUserKey = new MetadataKey<>("user", String.class);
                String responsesUser = (String) responseMap.get("user");
                container.put(responsesUserKey, responsesUser);

                MetadataKey<Map<String, Object>> responsesMetadataKey = new MetadataKey<>("metadata", Map.class);
                Map<String, Object> responsesMetadata = (Map<String, Object>) responseMap.get("metadata");
                container.put(responsesMetadataKey, responsesMetadata);

                MetadataKey<String> responsesOutputContentKey = new MetadataKey<>("output_content", String.class);
                List<Map<String, Object>> responsesOutput = (List<Map<String, Object>>) responseMap.get("output");
                if (responsesOutput != null && !responsesOutput.isEmpty()) {
                    Map<String, Object> responsesMessage = responsesOutput.get(0);
                    List<Map<String, Object>> responsesContentList = (List<Map<String, Object>>) responsesMessage.get("content");
                    if (responsesContentList != null && !responsesContentList.isEmpty()) {
                        String responsesOutputContent = (String) responsesContentList.get(0).get("text");
                        container.put(responsesOutputContentKey, responsesOutputContent);
                    }
                }
                break;
            }
            default: {
                // Optionally handle unknown types
                break;
            }
        }
    }

    public static CompletableFuture<Boolean> completeGetFlagged(MetadataContainer container) {
        return CompletableFuture.supplyAsync(() -> {
            MetadataKey<String> flaggedKey = new MetadataKey<>("flagged", String.class);
            return Boolean.parseBoolean(container.get(flaggedKey));
        });
    }

    public static CompletableFuture<String> completeGetOutput(MetadataContainer container) {
        return CompletableFuture.supplyAsync(() -> {
            MetadataKey<String> outputKey = new MetadataKey<>("output_content", String.class);
            return container.get(outputKey);
        });
    }

    public static CompletableFuture<Map<String, Boolean>> completeGetFlaggedReasons(MetadataContainer conversationContainer) {
        return CompletableFuture.supplyAsync(() -> {
            String[] keys = {
                "sexual", "sexual/minors", "harassment", "harassment/threatening",
                "hate", "hate/threatening", "illicit", "illicit/violent",
                "self-harm", "self-harm/intent", "self-harm/instructions",
                "violence", "violence/graphic"
            };
            Map<String, Boolean> reasonValues = new LinkedHashMap<>();
            for (String key : keys) {
                MetadataKey<Boolean> keyObj = new MetadataKey<>(key, Boolean.class);
                Boolean value = conversationContainer.get(keyObj);
                reasonValues.put(key, value != null && value);
            }
            return reasonValues;
        });
    }

    public static CompletableFuture<String> completeGetFormatFlaggedReasons(MetadataContainer conversationContainer) {
        return completeGetFlaggedReasons(conversationContainer).thenApply(reasons -> {
            String joinedReasons = reasons.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
    
            return "⚠️ Flagged for: " + joinedReasons;
        });
    }

}
//    public Boolean getFlagged() { MetadataKey<String> flaggedKey = new MetadataKey<>("flagged", String.class); return Boolean.parseBoolean(container.get(flaggedKey));
//    }
//
//    public String getOutput() {
//        MetadataKey<String> outputKey = new MetadataKey<>("output_content", String.class);
//        return container.get(outputKey);
//    }
//
//    public String getPreviousResponseId() {
//        MetadataKey<String> previousResponseIdKey = new MetadataKey<>("previous_response_id", String.class);
//        return container.get(previousResponseIdKey);
//    }
//
//    public static Map<String, Boolean> getFlaggedReasons(MetadataContainer conversationContainer) {
//        String[] keys = {
//            "sexual",
//            "sexual/minors",
//            "harassment",
//            "harassment/threatening",
//            "hate",
//            "hate/threatening",
//            "illicit",
//            "illicit/violent",
//            "self-harm",
//            "self-harm/intent",
//            "self-harm/instructions",
//            "violence",
//            "violence/graphic"
//        };
//        Map<String, Boolean> reasonValues = new LinkedHashMap<>();
//        for (String key : keys) {
//            MetadataKey<Boolean> keyObj = new MetadataKey<>(key, Boolean.class);
//            Boolean value = conversationContainer.get(keyObj);
//            reasonValues.put(key, value != null && value);
//        }
//        return reasonValues;
//    }
//
//    public static String formatFlaggedReasons(MetadataContainer conversationContainer) {
//        Map<String, Boolean> reasons = getFlaggedReasons(conversationContainer);
//
//        String joinedReasons = reasons.entrySet().stream()
//                .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
//                .map(Map.Entry::getKey)
//                .collect(Collectors.joining(", "));
//
//        return "⚠️ Flagged for: " + joinedReasons;
//    }
