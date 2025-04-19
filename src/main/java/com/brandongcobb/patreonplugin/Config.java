package com.brandongcobb.patreonplugin;


import com.brandongcobb.patreonplugin.utils.inc.Helpers;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.List;

public class Config {

    private static Helpers helpers = new Helpers();
    public static Map<String, Object> config = new HashMap<>();
    private static int i;

    static {
        loadConfig();
    }

    public static Map<String, Object> getConfig() {
        return config;
    }

    private static void loadConfig() {
        File configFile = new File(helpers.PATH_CONFIG_YAML);
        if (configFile.exists()) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(inputStream);
                if (promptForYesNo("Do you want to change any settings? (yes/no): ", true)) {
                    modifyApiKeys();
                    promptAdditionalConfig();
                    saveConfig();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            createDefaultConfig();
        }
    }

    public static ConfigSection getNestedConfigValue(String outerKey, String innerKey) {
        Map<String, Object> outerMap = (Map<String, Object>) config.get(outerKey);
        if (outerMap != null) {
            Object innerValue = outerMap.get(innerKey);
            if (innerValue instanceof Map) {
                return new ConfigSection((Map<String, Object>) innerValue);
            }
        }
        return null; // Return null or handle accordingly if the outer key doesn't exist
    }

    public static Object getConfigValue(String key) {
        return config.get(key);
    }

    public static String getStringValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null; // or throw an exception if you expect a String
    }

    public static Integer getIntValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null; // or throw an exception if you expect an Integer
    }

    public static Float getFloatValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return null; // or throw an exception if you expect a Float
    }

    public static Boolean getBooleanValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            // Handle string representations of boolean, e.g., "true", "false"
            return Boolean.parseBoolean((String) value);
        }
        return null; // or throw an exception if you expect a Boolean
    }

    private static void createDefaultConfig() {
        config.put("api_keys", new HashMap<>());
        createApiKeys();
        promptAdditionalConfig(true);
        saveConfig();
    }

    private static void saveConfig() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (Writer writer = new FileWriter(helpers.PATH_CONFIG_YAML)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createApiKeys() {
        int numKeys = promptForInt("How many API keys do you want to set up? (1-20)", 1, 20);
        for (int i = 1; i <= numKeys; i++) {
            String keyName = promptForString("Enter a unique name for API key #" + i, "api_key_" + i);
            while (config.containsKey(keyName)) {
                keyName = promptForString("Enter a unique name for API key #" + i, "api_key_" + i);
            }
            Map<String, String> apiKeyData = new HashMap<>();
            apiKeyData.put("api_key", promptForString("Enter API key for '" + keyName + "'", ""));
            apiKeyData.put("client_id", promptForString("Enter client ID for '" + keyName + "'", ""));
            apiKeyData.put("client_secret", promptForString("Enter client secret for '" + keyName + "'", ""));
            apiKeyData.put("redirect_uri", promptForString("Enter redirect URI for '" + keyName + "'", ""));
            config.put(keyName, apiKeyData);
        }
    }

    private static void modifyApiKeys() {
        if (config.containsKey("api_keys")) {
            Map<String, Map<String, String>> apiKeys = (Map<String, Map<String, String>>) config.get("api_keys");
            if (apiKeys.isEmpty()) {
                System.out.println("No API keys to modify.");
                return;
            }
            System.out.println("Available API keys:");
            int index = 1;
            for (String keyName : apiKeys.keySet()) {
                System.out.println(index + ". " + keyName);
                index++;
            }
            int choice = promptForInt("Select the number of the API key to modify (1-" + apiKeys.size() + ")", 1, apiKeys.size());
            String selectedKeyName = (String) apiKeys.keySet().toArray()[choice - 1];
            Map<String, String> keyDetails = apiKeys.get(selectedKeyName);
            System.out.println("Modifying API key: " + selectedKeyName);
            keyDetails.put("api_key", promptForString("Enter API key for '" + selectedKeyName + "'", keyDetails.get("api_key")));
            keyDetails.put("client_id", promptForString("Enter client ID for '" + selectedKeyName + "'", keyDetails.get("client_id")));
            keyDetails.put("client_secret", promptForString("Enter client secret for '" + selectedKeyName + "'", keyDetails.get("client_secret")));
            keyDetails.put("redirect_uri", promptForString("Enter redirect URI for '" + selectedKeyName + "'", keyDetails.get("redirect_uri")));
            saveConfig();
            System.out.println("API key '" + selectedKeyName + "' has been modified.");
        } else {
            System.out.println("No API keys found in the configuration.");
        }
    }

    private static void promptAdditionalConfig() {
        promptAdditionalConfig(false);
    }

    private static void promptAdditionalConfig(boolean creating) {
        config.put("discord_character_limit", promptForInt("Discord character limit?", helpers.DISCORD_CHARACTER_LIMIT, 4000));
        config.put("discord_command_prefix", promptForString("Discord command prefix?", helpers.DISCORD_COMMAND_PREFIX));
        config.put("discord_moderation_warning", promptForString("What should be sent to users if their message was moderated?", helpers.DISCORD_MODERATION_WARNING));
        config.put("discord_owner_id", promptForLong("Discord Owner ID?", helpers.DISCORD_OWNER_ID, 2000000000000000000L));
        config.put("discord_release_mode", promptForYesNo("Discord release mode?", helpers.DISCORD_RELEASE_MODE));
        config.put("discord_role_pass", promptForLong("What is the role ID youd like unfiltered?", helpers.DISCORD_ROLE_PASS, 2000000000000000000L));
        config.put("discord_testing_guild_id", promptForLong("What is the Discord testing guild ID?", helpers.DISCORD_TESTING_GUILD_ID, 2000000000000000000L));
        config.put("discord_testing_guild_ids", promptForLong("Any extras?", helpers.DISCORD_TESTING_GUILD_ID, 2000000000000000000L));
        config.put("discord_token", promptForString("What is the Discord token?", ""));
        config.put("logging_level", promptForString("What is the logging level (DEBUG, INFO, etc.)?", helpers.LOGGING_LEVEL));
        config.put("openai_chat_add_completion_to_history", promptForYesNo("Should completions be added to conversations?", helpers.OPENAI_CHAT_ADD_COMPLETION_TO_HISTORY));
        config.put("openai_chat_model", promptForString("Which chat model would you like to use for OpenAIs ChatGPT?", helpers.OPENAI_CHAT_MODEL));
        config.put("openai_chat_moderation_model", promptForString("Which OpenAI completions model would you like to use for moderation?", helpers.OPENAI_CHAT_MODERATION_MODEL));
        config.put("openai_chat_completion", promptForYesNo("Enable or disable OpenAI text completions (True/False)?", helpers.OPENAI_CHAT_COMPLETION));
        config.put("openai_chat_moderation", promptForYesNo("Enable or disable OpenAI text moderation (True/False)?", helpers.OPENAI_CHAT_MODERATION));
        config.put("openai_chat_store", promptForYesNo("Store OpenAI completions (True/False)?", helpers.OPENAI_CHAT_STORE));
        config.put("openai_chat_stream", promptForYesNo("Enable or disable OpenAI completions streaming (True/False)?", helpers.OPENAI_CHAT_STREAM));
        config.put("openai_chat_stop", promptForString("What might be the OpenAI stop criteria for completions?", helpers.OPENAI_CHAT_STOP));
        List<String> models = helpers.OPENAI_CHAT_MODELS.get("deprecated");
        for (i = 0; i == models.size(); i++) {
            if (models.get(i) == helpers.OPENAI_CHAT_MODEL) {
                boolean exists = true;
                if (exists) {
                    config.put("openai_chat_sys_input", promptForString("What is the OpenAI completions system input?", helpers.OPENAI_CHAT_SYS_INPUT));
                }
                else {
                    config.put("openai_chat_sys_input", promptForString("What is the OpenAI completions system input?", ""));
                }
                return;
            }
        }
        config.put("openai_chat_temperature", promptForFloat("What is the OpenAI completions temperature (0.0 to 2.0)?", helpers.OPENAI_CHAT_TEMPERATURE, 2.0f));
        config.put("openai_chat_top_p", promptForFloat("What should the top p be for OpenAI completions?", helpers.OPENAI_CHAT_TOP_P, 1.0f));
        config.put("openai_chat_use_history", promptForYesNo("Should OpenAI moderations use history?", helpers.OPENAI_CHAT_USE_HISTORY));
        config.put("openai_chat_user", promptForString("What is your OpenAI username?", helpers.OPENAI_CHAT_USER));
        config.put("openai_moderation_image", promptForYesNo("Enable or disable OpenAI image moderation (True/False)?", helpers.OPENAI_MODERATION_IMAGE));
        config.put("openai_moderation_model", promptForString("Which model do you want for OpenAI image moderation?", helpers.OPENAI_MODERATION_MODEL));
        config.put("openai_organization", promptForString("What is the OpenAI-Organization ID?", helpers.OPENAI_CHAT_HEADERS.getOrDefault("OpenAI-Organization", "")));
        config.put("openai_project", promptForString("What is the OpenAI-Project ID?", helpers.OPENAI_CHAT_HEADERS.getOrDefault("OpenAI-Project", "")));
        config.put("user_agent", promptForString("What should be the User-Agent header?", helpers.USER_AGENT));
        config.put("version", promptForString("Would you like to override the bot version?", helpers.VERSION));
    }

    private static long promptForLong(String prompt, long defaultValue, long maxValue) {
        long value;
        long minValue = 0L;
        while (true) {
             try {
                 System.out.print(prompt + " (default: " + defaultValue + "): ");
                 String input = new Scanner(System.in).nextLine();
     
                 // Use the default value if the input is empty
                 value = input.isEmpty() ? defaultValue : Long.parseLong(input);
     
                 // Check if the value is within the defined limits
                 if (value >= minValue && value <= maxValue) {
                     break; // Input is valid, exit the loop
                 } else {
                     System.out.println("Please enter a value between " + minValue + " and " + maxValue + ".");
                 }
             } catch (NumberFormatException e) {
                 System.out.println("Invalid input. Please enter a valid number.");
             }
         }
         return value; // Return the valid long value
     }
    private static float promptForFloat(String prompt, float defaultValue, float maxValue) {
        float value;
        float minValue = 0.0f; // System-defined minimum value for float
    
        while (true) {
            try {
                System.out.print(prompt + " (default: " + defaultValue + "): ");
                String input = new Scanner(System.in).nextLine();
    
                // Use the default value if the input is empty
                value = input.isEmpty() ? defaultValue : Float.parseFloat(input);
    
                // Check if the value is within the system-defined limits
                if (value >= minValue && value <= maxValue) {
                    break; // Input is valid, exit the loop
                } else {
                    System.out.println("Please enter a value between " + minValue + " and " + maxValue + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
        return value; // Return the valid float value
    }

    private static String promptForString(String prompt, String defaultValue) {
        System.out.print(prompt + " (default: " + defaultValue + "): ");
        String input = new Scanner(System.in).nextLine();
        return input.isEmpty() ? defaultValue : input;
    }

    private static boolean promptForYesNo(String prompt, boolean defaultValue) {
        while (true) {
            System.out.print(prompt + " (default: " + defaultValue + ") [true/false]: ");
            String input = new Scanner(System.in).nextLine().trim().toLowerCase();
            if (input.isEmpty()) {
                return defaultValue; // Return the default value if input is empty
            } else if (input.equals("true") || input.equals("yes") || input.equals("1")) {
                return true; // User input indicates true
            } else if (input.equals("false") || input.equals("no") || input.equals("0")) {
                return false; // User input indicates false
            } else {
                System.out.println("Invalid input. Please enter true/false, yes/no, or 1/0.");
            }
        }
    }

    private static int promptForInt(String prompt, int maxValue, int defaultValue) {
        int value;
        int minValue = 0; // Set the minimum value to 0
    
        while (true) {
            try {
                System.out.print(prompt + " (default: " + defaultValue + "): ");
                String input = new Scanner(System.in).nextLine();
    
                // Use the default value if the input is empty
                value = input.isEmpty() ? defaultValue : Integer.parseInt(input);
    
                // Check if the value is within the defined limits
                if (value >= minValue && value <= maxValue) {
                    break; // Input is valid, exit the loop
                } else {
                    System.out.println("Please enter a value between " + minValue + " and " + maxValue + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
        return value; // Return the valid integer value
    }
}
