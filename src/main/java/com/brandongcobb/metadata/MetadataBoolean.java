package com.brandongcobb.metadata;

import java.util.Objects;

public class MetadataBoolean implements MetadataType<Boolean> {

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataBoolean;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Boolean.class);
    }
}
