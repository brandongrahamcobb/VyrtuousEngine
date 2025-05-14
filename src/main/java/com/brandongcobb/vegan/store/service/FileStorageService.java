package com.brandongcobb.vegan.store.service;

import java.io.InputStream;

public interface FileStorageService {
    /**
     * Store the incoming stream under the given filename (must be unique or renamed),
     * and return a public URL (e.g. “/uploads/…”).
     */
    String store(String filename, InputStream data);
}
