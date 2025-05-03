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

public class ResponseObject extends MetadataContainer{

    public ResponseObject(Map<String, Object> responseMap) {
        MetadataKey<String> idKey = new MetadataKey<>("id", String.class);
        MetadataKey<String> objectKey = new MetadataKey<>("object", String.class);

        String requestId = (String) responseMap.get("id");
        String requestObject = (String) responseMap.get("object");

        // If the "object" value is missing, throw an exception or handle it gracefully
        if (requestObject == null) {
            throw new NullPointerException("The response map is missing the mandatory 'object' field.");
        }

        put(idKey, requestId);
        put(objectKey, requestObject);

        // Refactored to use a switch on a non-null value and include breaks
        switch (requestObject) {
            case "chat.completion": {
                MetadataKey<Integer> completionCreatedKey = new MetadataKey<>("created", Integer.class);
                Integer completionCreated = (Integer) responseMap.get("created");
                put(completionCreatedKey, completionCreated);

                MetadataKey<String> completionModelKey = new MetadataKey<>("model", String.class);
                String completionModel = (String) responseMap.get("model");
                put(completionModelKey, completionModel);

                List<Map<String, Object>> completionChoices = (List<Map<String, Object>>) responseMap.get("choices");
                if (completionChoices != null && !completionChoices.isEmpty()) {
                    Map<String, Object> completionChoice = completionChoices.get(0);

                    Map<String, String> completionMessage = (Map<String, String>) completionChoice.get("message");
                    MetadataKey<String> completionRoleKey = new MetadataKey<>("role", String.class);
                    String completionRole = completionMessage != null ? completionMessage.get("role") : null;
                    put(completionRoleKey, completionRole);

                    MetadataKey<String> completionContentKey = new MetadataKey<>("content", String.class);
                    String completionContent = completionMessage != null ? completionMessage.get("content") : null;
                    put(completionContentKey, completionContent);
                    MetadataKey<String> completionFinishReasonKey = new MetadataKey<>("finish_reason", String.class);
                    String completionFinishReason = (String) completionChoice.get("finish_reason");
                    put(completionFinishReasonKey, completionFinishReason);

                    MetadataKey<Integer> completionIndexKey = new MetadataKey<>("index", Integer.class);
                    Integer completionIndex = (Integer) completionChoice.get("index");
                    put(completionIndexKey, completionIndex);
                }

                Map<String, Integer> completionUsage = (Map<String, Integer>) responseMap.get("usage");
                if (completionUsage != null) {
                    MetadataKey<Integer> completionTotalTokensKey = new MetadataKey<>("total_tokens", Integer.class);
                    Integer completionTotalTokens = completionUsage.get("total_tokens");
                    put(completionTotalTokensKey, completionTotalTokens);

                    MetadataKey<Integer> completionPromptTokensKey = new MetadataKey<>("prompt_tokens", Integer.class);
                    Integer completionPromptTokens = completionUsage.get("prompt_tokens");
                    put(completionPromptTokensKey, completionPromptTokens);

                    MetadataKey<Integer> completionCompletionTokensKey = new MetadataKey<>("completion_tokens", Integer.class);
                    Integer completionCompletionTokens = completionUsage.get("completion_tokens");
                    put(completionCompletionTokensKey, completionCompletionTokens);
                }
                break;
            }
            case "moderation": {
                MetadataKey<Integer> moderationCreatedKey = new MetadataKey<>("created", Integer.class);
                Integer moderationCreated = (Integer) responseMap.get("created");
                put(moderationCreatedKey, moderationCreated);
                MetadataKey<String> moderationModelKey = new MetadataKey<>("model", String.class);
                String moderationModel = (String) responseMap.get("model");
                put(moderationModelKey, moderationModel);

                List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> result = results.get(0);

                    MetadataKey<Boolean> flaggedKey = new MetadataKey<>("flagged", Boolean.class);
                    Boolean moderationFlagged = (Boolean) result.get("flagged");
                    put(flaggedKey, moderationFlagged);

                    Map<String, Boolean> categories = (Map<String, Boolean>) result.get("categories");
                    if (categories != null) {
                        MetadataKey<Boolean> sexualKey = new MetadataKey<>("sexual", Boolean.class);
                        Boolean moderationSexual = categories.get("sexual");
                        put(sexualKey, moderationSexual);

                        MetadataKey<Boolean> sexualMinorsKey = new MetadataKey<>("sexual/minors", Boolean.class);
                        Boolean moderationSexualMinors = categories.get("sexual/minors");
                        put(sexualMinorsKey, moderationSexualMinors);

                        MetadataKey<Boolean> harassmentKey = new MetadataKey<>("harassment", Boolean.class);
                        Boolean moderationHarassment = categories.get("harassment");
                        put(harassmentKey, moderationHarassment);

                        MetadataKey<Boolean> harassmentThreateningKey = new MetadataKey<>("harassment/threatening", Boolean.class);
                        Boolean moderationHarassmentThreatening = categories.get("harassment/threatening");
                        put(harassmentThreateningKey, moderationHarassmentThreatening);

                        MetadataKey<Boolean> hateKey = new MetadataKey<>("hate", Boolean.class);
                        Boolean moderationHate = categories.get("hate");
                        put(hateKey, moderationHate);

                        MetadataKey<Boolean> hateThreateningKey = new MetadataKey<>("hate/threatening", Boolean.class);
                        Boolean moderationHateThreatening = categories.get("hate/threatening");
                        put(hateThreateningKey, moderationHateThreatening);

                        MetadataKey<Boolean> illicitKey = new MetadataKey<>("illicit", Boolean.class);
                        Boolean moderationIllicit = categories.get("illicit");
                        put(illicitKey, moderationIllicit);

                        // Note: watch out for extra spaces in key names
                        MetadataKey<Boolean> illicitViolentKey = new MetadataKey<>("illicit/violent", Boolean.class);
                        Boolean moderationIllicitViolent = categories.get("illicit/violent");
                        put(illicitViolentKey, moderationIllicitViolent);

                        MetadataKey<Boolean> selfHarmKey = new MetadataKey<>("self-harm", Boolean.class);
                        Boolean moderationSelfHarm = categories.get("self-harm");
                        put(selfHarmKey, moderationSelfHarm);

                        MetadataKey<Boolean> selfHarmIntentKey = new MetadataKey<>("self-harm/intent", Boolean.class);
                        Boolean moderationSelfHarmIntent = categories.get("self-harm/intent");
                        put(selfHarmIntentKey, moderationSelfHarmIntent);

                        MetadataKey<Boolean> selfHarmInstructionsKey = new MetadataKey<>("self-harm/instructions", Boolean.class);
                        Boolean moderationSelfHarmInstructions = categories.get("self-harm/instructions");
                        put(selfHarmInstructionsKey, moderationSelfHarmInstructions);

                        MetadataKey<Boolean> violenceKey = new MetadataKey<>("violence", Boolean.class);
                        Boolean moderationViolence = categories.get("violence");
                        put(violenceKey, moderationViolence);

                        MetadataKey<Boolean> violenceGraphicKey = new MetadataKey<>("violence/graphic", Boolean.class);
                        Boolean moderationViolenceGraphic = categories.get("violence/graphic");
                        put(violenceGraphicKey, moderationViolenceGraphic);
                    }

                    Map<String, String> categoryScores = (Map<String, String>) result.get("category_scores");
                    if (categoryScores != null) {
                        MetadataKey<String> sexualScoreKey = new MetadataKey<>("sexual", String.class);
                        String moderationSexualScore = categoryScores.get("sexual");
                        put(sexualScoreKey, moderationSexualScore);

                        MetadataKey<String> sexualMinorsScoreKey = new MetadataKey<>("sexual/minors", String.class);
                        String moderationSexualMinorsScore = categoryScores.get("sexual/minors");
                        put(sexualMinorsScoreKey, moderationSexualMinorsScore);

                        MetadataKey<String> harassmentScoreKey = new MetadataKey<>("harassment", String.class);
                        String moderationHarassmentScore = categoryScores.get("harassment");
                        put(harassmentScoreKey, moderationHarassmentScore);

                        MetadataKey<String> harassmentThreateningScoreKey = new MetadataKey<>("harassment/threatening", String.class);
                        String moderationHarassmentThreateningScore = categoryScores.get("harassment/threatening");
                        put(harassmentThreateningScoreKey, moderationHarassmentThreateningScore);

                        MetadataKey<String> hateScoreKey = new MetadataKey<>("hate", String.class);
                        String moderationHateScore = categoryScores.get("hate");
                        put(hateScoreKey, moderationHateScore);

                        MetadataKey<String> hateThreateningScoreKey = new MetadataKey<>("hate/threatening", String.class);
                        String moderationHateThreateningScore = categoryScores.get("hate/threatening");
                        put(hateThreateningScoreKey, moderationHateThreateningScore);

                        MetadataKey<String> illicitScoreKey = new MetadataKey<>("illicit", String.class);
                        String moderationIllicitScore = categoryScores.get("illicit");
                        put(illicitScoreKey, moderationIllicitScore);

                        MetadataKey<String> illicitViolentScoreKey = new MetadataKey<>("illicit/violent", String.class);
                        String moderationIllicitViolentScore = categoryScores.get("illicit/violent");
                        put(illicitViolentScoreKey, moderationIllicitViolentScore);

                        MetadataKey<String> selfHarmScoreKey = new MetadataKey<>("self-harm", String.class);
                        String moderationSelfHarmScore = categoryScores.get("self-harm");
                        put(selfHarmScoreKey, moderationSelfHarmScore);

                        MetadataKey<String> selfHarmIntentScoreKey = new MetadataKey<>("self-harm/intent", String.class);
                        String moderationSelfHarmIntentScore = categoryScores.get("self-harm/intent");
                        put(selfHarmIntentScoreKey, moderationSelfHarmIntentScore);

                        MetadataKey<String> selfHarmInstructionsScoreKey = new MetadataKey<>("self-harm/instructions", String.class);
                        String moderationSelfHarmInstructionsScore = categoryScores.get("self-harm/instructions");
                        put(selfHarmInstructionsScoreKey, moderationSelfHarmInstructionsScore);

                        MetadataKey<String> violenceScoreKey = new MetadataKey<>("violence", String.class);
                        String moderationViolenceScore = categoryScores.get("violence");
                        put(violenceScoreKey, moderationViolenceScore);

                        MetadataKey<String> violenceGraphicScoreKey = new MetadataKey<>("violence/graphic", String.class);
                        String moderationViolenceGraphicScore = categoryScores.get("violence/graphic");
                        put(violenceGraphicScoreKey, moderationViolenceGraphicScore);
                    }

                    Map<String, Object> categoryAppliedInputTypes = (Map<String, Object>) result.get("category_applied_input_types");
                    if (categoryAppliedInputTypes != null) {
                        // For each category, extract and store the applied input types as needed.
                        MetadataKey<List<String>> sexualInputKey = new MetadataKey<>("sexual", List.class);
                        List<String> moderationSexualAppliedType = (List<String>) categoryAppliedInputTypes.get("sexual");
                        put(sexualInputKey, moderationSexualAppliedType);

                        // ... Similar handling for the rest of the keys.
                    }
                }
                break;
            }
            case "response": {
                MetadataKey<String> responsesIdKey = new MetadataKey<>("id", String.class);
                String responsesId = (String) responseMap.get("id");
                put(responsesIdKey, responsesId);

                MetadataKey<String> responsesObjectKey = new MetadataKey<>("object", String.class);
                String responsesObject = (String) responseMap.get("object");
                put(responsesObjectKey, responsesObject);

                MetadataKey<Integer> responsesCreatedAtKey = new MetadataKey<>("created_at", Integer.class);
                Integer responsesCreatedAt = (Integer) responseMap.get("created_at");
                put(responsesCreatedAtKey, responsesCreatedAt);

                MetadataKey<String> responsesStatusKey = new MetadataKey<>("status", String.class);
                String responsesStatus = (String) responseMap.get("status");
                put(responsesStatusKey, responsesStatus);

                MetadataKey<String> responsesErrorKey = new MetadataKey<>("error", String.class);
                String responsesError = (String) responseMap.get("error");
                put(responsesErrorKey, responsesError);

                MetadataKey<String> responsesIncompleteDetailsReasonKey = new MetadataKey<>("reason", String.class);
                // It looks like the incomplete details might be nested—make sure you get the right field.
                Map<String, String> responsesIncompleteDetails = (Map<String, String>) responseMap.get("incomplete_details");
                String reason = responsesIncompleteDetails != null ? responsesIncompleteDetails.get("reason") : null;
                put(responsesIncompleteDetailsReasonKey, reason);

                MetadataKey<String> responsesInstructionsKey = new MetadataKey<>("instructions", String.class);
                String responsesInstructions = (String) responseMap.get("instructions");
                put(responsesInstructionsKey, responsesInstructions);

                MetadataKey<Integer> responsesMaxOutputTokensKey = new MetadataKey<>("max_output_tokens", Integer.class);
                Integer responsesMaxOutputTokens = (Integer) responseMap.get("max_output_tokens");
                put(responsesMaxOutputTokensKey, responsesMaxOutputTokens);
                MetadataKey<String> responsesModelKey = new MetadataKey<>("model", String.class);
                String responsesModel = (String) responseMap.get("model");
                put(responsesModelKey, responsesModel);

                MetadataKey<Boolean> responsesParallelToolCallsKey = new MetadataKey<>("parallel_tool_calls", Boolean.class);
                Boolean responsesParallelToolCalls = (Boolean) responseMap.get("parallel_tool_calls");
                put(responsesParallelToolCallsKey, responsesParallelToolCalls);

                MetadataKey<String> responsesPreviousResponseIdKey = new MetadataKey<>("previous_response_id", String.class);
                String responsesPreviousResponseId = (String) responseMap.get("previous_response_id");
                put(responsesPreviousResponseIdKey, responsesPreviousResponseId);

                MetadataKey<String> responsesReasoningEffortKey = new MetadataKey<>("effort", String.class);
                MetadataKey<String> responsesReasoningSummaryKey = new MetadataKey<>("summary", String.class);
                Map<String, String> responsesReasoning = (Map<String, String>) responseMap.get("reasoning");
                if (responsesReasoning != null) {
                    String responsesReasoningEffort = responsesReasoning.get("effort");
                    put(responsesReasoningEffortKey, responsesReasoningEffort);

                    String responsesReasoningSummary = responsesReasoning.get("summary");
                    put(responsesReasoningSummaryKey, responsesReasoningSummary);
                }

                MetadataKey<Double> responsesTemperatureKey = new MetadataKey<>("temperature", Double.class);
                Double responsesTemperature = (Double) responseMap.get("temperature");
                put(responsesTemperatureKey, responsesTemperature);

                MetadataKey<Map<String, Object>> responsesTextFormatKey = new MetadataKey<>("text_format", Map.class);
                // Ensure to retrieve the right nested field; here it previously was called "text" in your version.
                Map<String, Object> responsesTextFormat = (Map<String, Object>) responseMap.get("text");
                put(responsesTextFormatKey, responsesTextFormat);

                MetadataKey<String> responsesToolChoiceKey = new MetadataKey<>("tool_choice", String.class);
                String responsesToolChoice = (String) responseMap.get("tool_choice");
                put(responsesToolChoiceKey, responsesToolChoice);

                MetadataKey<List<String>> responsesToolsKey = new MetadataKey<>("tools", List.class);
                List<String> responsesTools = (List<String>) responseMap.get("tools");
                put(responsesToolsKey, responsesTools);

                MetadataKey<Double> responsesTopPKey = new MetadataKey<>("top_p", Double.class);
                Double responsesTopP = (Double) responseMap.get("top_p");
                put(responsesTopPKey, responsesTopP);

                MetadataKey<String> responsesTruncationKey = new MetadataKey<>("truncation", String.class);
                String responsesTruncation = (String) responseMap.get("truncation");
                put(responsesTruncationKey, responsesTruncation);

                MetadataKey<Integer> responsesTotalTokensKey = new MetadataKey<>("total_tokens", Integer.class);
                Map<String, Object> responsesUsage = (Map<String, Object>) responseMap.get("usage");
                if (responsesUsage != null) {
                    Integer responsesTotalTokens = (Integer) responsesUsage.get("total_tokens");
                    put(responsesTotalTokensKey, responsesTotalTokens);
                }

                MetadataKey<String> responsesUserKey = new MetadataKey<>("user", String.class);
                String responsesUser = (String) responseMap.get("user");
                put(responsesUserKey, responsesUser);

                MetadataKey<Map<String, Object>> responsesMetadataKey = new MetadataKey<>("metadata", Map.class);
                Map<String, Object> responsesMetadata = (Map<String, Object>) responseMap.get("metadata");
                put(responsesMetadataKey, responsesMetadata);

                MetadataKey<String> responsesOutputContentKey = new MetadataKey<>("output_content", String.class);
                List<Map<String, Object>> responsesOutput = (List<Map<String, Object>>) responseMap.get("output");
                if (responsesOutput != null && !responsesOutput.isEmpty()) {
                    Map<String, Object> responsesMessage = responsesOutput.get(0);
                    List<Map<String, Object>> responsesContentList = (List<Map<String, Object>>) responsesMessage.get("content");
                    if (responsesContentList != null && !responsesContentList.isEmpty()) {
                        String responsesOutputContent = (String) responsesContentList.get(0).get("text");
                        put(responsesOutputContentKey, responsesOutputContent);
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

    public CompletableFuture<Boolean> completeGetFlagged() {
        return CompletableFuture.supplyAsync(() -> {
            MetadataKey<Boolean> flaggedKey = new MetadataKey<>("flagged", Boolean.class);
            Object flaggedObj = this.get(flaggedKey);
            return flaggedObj != null && Boolean.parseBoolean(String.valueOf(flaggedObj));
        });
    }

    public CompletableFuture<Map<String, Boolean>> completeGetFlaggedReasons() {
        return CompletableFuture.supplyAsync(() -> {
            MetadataKey<Boolean>[] keys = new MetadataKey[] {
                new MetadataKey<>("sexual", Boolean.class),
                new MetadataKey<>("sexual/minors", Boolean.class),
                new MetadataKey<>("harassment", Boolean.class),
                new MetadataKey<>("harassment/threatening", Boolean.class),
                new MetadataKey<>("hate", Boolean.class),
                new MetadataKey<>("hate/threatening", Boolean.class),
                new MetadataKey<>("illicit", Boolean.class),
                new MetadataKey<>("illicit/violent", Boolean.class),
                new MetadataKey<>("self-harm", Boolean.class),
                new MetadataKey<>("self-harm/intent", Boolean.class),
                new MetadataKey<>("self-harm/instructions", Boolean.class),
                new MetadataKey<>("violence", Boolean.class),
                new MetadataKey<>("violence/graphic", Boolean.class)
            };
            Map<String, Boolean> reasonValues = new LinkedHashMap<>();
            for (MetadataKey<Boolean> key : keys) {
                Object value = this.get(key);
                reasonValues.put(key.getName(), value != null && Boolean.parseBoolean(String.valueOf(value)));
            }
            return reasonValues;
        });
    }

    public CompletableFuture<String> completeGetFormatFlaggedReasons() {
        return completeGetFlaggedReasons().thenApply(reasons -> {
            String joinedReasons = reasons.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
            return "⚠️ Flagged for: " + joinedReasons;
        });
    }

    public CompletableFuture<String> completeGetOutput() {
        return CompletableFuture.supplyAsync(() -> {
            MetadataKey<String> outputKey = new MetadataKey<>("output_content", String.class);
            return this.get(outputKey); // this refers to your MetadataContainer or similar
        });
    }
}
