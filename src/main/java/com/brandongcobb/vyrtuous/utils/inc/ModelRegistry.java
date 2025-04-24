package com.brandongcobb.vyrtuous.utils.inc;

import com.brandongcobb.vyrtuous.records.ModelInfo;
import java.util.Map;

public class ModelRegistry {
    public static Long parseCommaNumber(String number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c != ',') {
                sb.append(c);
            }
        }
        String cleanedNumber = sb.toString();
        try {
            int intVal = Integer.parseInt(cleanedNumber);
            return (long) intVal; // safely fits in int
        } catch (NumberFormatException e) {
            return Long.parseLong(cleanedNumber);
        }
    }

    public static final Map<String, ModelInfo> OPENAI_CHAT_COMPLETION_MODEL_CONTEXT_LIMITS = Map.ofEntries(
        Map.entry("ft:gpt-4o-mini-2024-07-18:spawd:vyrtuous:AjZpTNN2", new ModelInfo(parseCommaNumber("16,384"), false)),
        Map.entry("gpt-3.5-turbo", new ModelInfo(parseCommaNumber("4,096"), false)),
        Map.entry("gpt-4", new ModelInfo(parseCommaNumber("8,192"), false)),
        Map.entry("gpt-4-32k", new ModelInfo(parseCommaNumber("32,768"), false)),
        Map.entry("gpt-4-turbo", new ModelInfo(parseCommaNumber("128,000"), false)),
        Map.entry("gpt-4.1", new ModelInfo(parseCommaNumber("300,000"), true)),
        Map.entry("gpt-4.1-nano", new ModelInfo(parseCommaNumber("1,047,576"), true)),
        Map.entry("gpt-4o", new ModelInfo(parseCommaNumber("128,000"), false)),
        Map.entry("gpt-4o-audio", new ModelInfo(parseCommaNumber("128,000"), false)),
        Map.entry("gpt-4o-mini", new ModelInfo(parseCommaNumber("128,000"), false)),
        Map.entry("o1-mini", new ModelInfo(parseCommaNumber("128,000"), true)),
        Map.entry("o1-preview", new ModelInfo(parseCommaNumber("128,000"), true)),
        Map.entry("o3-mini", new ModelInfo(parseCommaNumber("200,000"), true)),
        Map.entry("o4-mini", new ModelInfo(parseCommaNumber("200,000"), true))
    );

    public static final Map<String, ModelInfo> OPENAI_CHAT_MODEL_OUTPUT_LIMITS = Map.ofEntries(
        Map.entry("ft:gpt-4o-mini-2024-07-18:spawd:vyrtuous:AjZpTNN2", new ModelInfo(parseCommaNumber("128,000"), false)),
        Map.entry("gpt-3.5-turbo", new ModelInfo(parseCommaNumber("4,096"), false)),
        Map.entry("gpt-4", new ModelInfo(parseCommaNumber("8,192"), false)),
        Map.entry("gpt-4-32k", new ModelInfo(parseCommaNumber("32,768"), false)),
        Map.entry("gpt-4-turbo", new ModelInfo(parseCommaNumber("4,096"), false)),
        Map.entry("gpt-4.1", new ModelInfo(parseCommaNumber("32,768"), true)),
        Map.entry("gpt-4.1-nano", new ModelInfo(parseCommaNumber("32,768"), true)),
        Map.entry("gpt-4o", new ModelInfo(parseCommaNumber("4,096"), false)),        // # Initially capped at 4")),096; updated to 16")),384 in later versions
        Map.entry("gpt-4o-audio", new ModelInfo(parseCommaNumber("16,384"), false)),
        Map.entry("gpt-4o-mini", new ModelInfo(parseCommaNumber("16,384"), false)),
        Map.entry("o1-mini", new ModelInfo(parseCommaNumber("16,384"), true)),
        Map.entry("o1-preview", new ModelInfo(parseCommaNumber("32,768"), true)),
        Map.entry("o3-mini", new ModelInfo(parseCommaNumber("100,000"), true)),
        Map.entry("o4-mini", new ModelInfo(parseCommaNumber("100,000"), true))
    );
}
