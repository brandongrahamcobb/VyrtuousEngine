package com.brandongcobb.vyrtuous.metadata;

import java.util.Objects;

public class MetadataString implements MetadataType<String> {

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataString;
    }

    @Override
    public int hashCode() {
        return Objects.hash(String.class);
    }
}
