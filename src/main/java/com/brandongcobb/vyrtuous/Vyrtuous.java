/*  Vyrtuous.java The primary purpose of this class is to integrate
 *  Discord, LinkedIn, OpenAI, Patreon, Twitch and many more into one
 *  hub.
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
package com.brandongcobb.vyrtuous;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.utils.handlers.*;
import com.brandongcobb.vyrtuous.utils.inc.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletionException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.NoSuchElementException;
import net.dv8tion.jda.api.JDA;

public class Vyrtuous {

    private final Map<Long, ResponseObject> userResponseMap = new ConcurrentHashMap<>();
    private static Vyrtuous app;
    private static Boolean isInputThreadRunning = false;

    private Map<Long, String> userModelSettings = new HashMap<>();

    public static void main(String[] args) {
        app = new Vyrtuous();
        DiscordBot bot = new DiscordBot();
        if (!isInputThreadRunning) {
            app.startChatInputThread();
            isInputThreadRunning = true;
        }
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void startChatInputThread() {
        Thread inputThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Chat input thread started. Type your messages:");
                while (true) {
                    System.out.print("> ");
                    String input;
                    try {
                        input = scanner.nextLine(); // Blocks for input
                    } catch (NoSuchElementException e) {
                        System.out.println("Input stream closed.");
                        break;
                    }
    
                    if (input.equalsIgnoreCase(".exit") || input.equalsIgnoreCase(".quit")) {
                        System.out.println("Exiting chat input thread.");
                        break;
                    }
    
                    // Your method to handle the input
                    String response = completeREPL(input);
                    System.out.println("Bot: " + response);
                }
            } catch (IllegalStateException e) {
                System.out.println("System.in is unavailable.");
            }
        });
    
        inputThread.setName("ChatInputThread");
        inputThread.setDaemon(false); // Important: keep it non-daemon so it doesn't exit immediately
        inputThread.start();
    }

    public String completeREPL(String message) {
        // If you want to ignore dot-commands:
        if (message.startsWith(".")) {
            return null;
        }
    
        AIManager aim = new AIManager();
        long senderId = 1L;  // or however you derive the user’s ID
        ResponseObject previousResponse = userResponseMap.get(senderId);
        boolean multimodal = false;  // or however you decide this flag
    
        try {
            // Build a chain that ultimately yields a CompletableFuture<String>
            CompletableFuture<String> resultFuture = Vyrtuous.getInstance()
                .completeGetUserModelSettings()
                .thenCompose(userModelSettings -> {
                    String modelSetting = userModelSettings
                        .getOrDefault(senderId, ModelRegistry.OPENAI_RESPONSE_MODEL.asString());
    
                    // Resolve which model to use
                    return aim.completeResolveModel(message, multimodal, modelSetting)
                        .thenCompose(model -> {
                            // Get previous response ID if any
                            CompletableFuture<String> prevIdFut = (previousResponse != null)
                                ? previousResponse.completeGetResponseId()
                                : CompletableFuture.completedFuture(null);
    
                            return prevIdFut.thenCompose(prevId -> 
                                // Send the chat
                                aim.completeChat(message, prevId, model)
                                    .thenCompose(chatResponse -> {
                                        // Chain setting the "previousResponseId" on the new response
                                        CompletableFuture<Void> setPrevFut;
                                        if (previousResponse != null) {
                                            setPrevFut = previousResponse
                                                .completeGetPreviousResponseId()
                                                .thenCompose(prevRespId ->
                                                    chatResponse.completeSetPreviousResponseId(prevRespId)
                                                );
                                        } else {
                                            setPrevFut = chatResponse.completeSetPreviousResponseId(null);
                                        }
    
                                        // Once that’s done, store it and get the output
                                        return setPrevFut.thenCompose(v -> {
                                            userResponseMap.put(senderId, chatResponse);
                                            return chatResponse.completeGetOutput();  // CompletableFuture<String>
                                        });
                                    })
                            );
                        });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    // Return an error message to the REPL
                    return "Error during completion: " + ex.getMessage();
                });
    
            // Block and get the result
            return resultFuture.join();
    
        } catch (CompletionException ce) {
            ce.printStackTrace();
            return "Unhandled exception: " + ce.getCause().getMessage();
        }
    }

    public CompletableFuture<Map<Long, String>> completeGetUserModelSettings() {
        return CompletableFuture.completedFuture(app.userModelSettings);
    }

    public static Vyrtuous getInstance() {
        return app;
    }

    /*
     * Setters
     *
     */
    public void completeSetUserModelSettings(Map<Long, String> userModelSettings) {
        app.userModelSettings = userModelSettings;
    }
}
