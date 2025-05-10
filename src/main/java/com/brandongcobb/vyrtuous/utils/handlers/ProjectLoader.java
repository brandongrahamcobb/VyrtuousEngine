package com.brandongcobb.utils.handlers;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;

public class ProjectLoader {
    public static String loadProjectSource() {
        Path sourceRoot = Paths.get("/app/src");

        try {
            return Files.walk(sourceRoot)
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> {
                    try {
                        return Files.readString(p);
                    } catch (IOException e) {
                        return "";
                    }
                })
                .collect(Collectors.joining("\n\n"));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
