package com.brandongcobb.vegan.store.service.impl;

import com.brandongcobb.vegan.store.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
public class FileSystemStorageService implements FileStorageService {

    private final Path root;

    public FileSystemStorageService(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir);
        Files.createDirectories(root);
    }

    @Override
    public String store(String filename, InputStream data) {
        try {
            // You may want to sanitize or randomize the filename
            Path destination = root.resolve(filename);
            Files.copy(data, destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }
}
