package com.brandongcobb.metadata;

import java.util.Objects;

public class MetadataLong implements MetadataType<Long> {

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataLong;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Long.class);
    }
}
