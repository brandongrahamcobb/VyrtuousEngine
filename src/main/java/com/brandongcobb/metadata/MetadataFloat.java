package com.brandongcobb.metadata;

import java.util.Objects;

public class MetadataFloat implements MetadataType<Float> {

    @Override
    public Class<Float> getType() {
        return Float.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataFloat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Float.class);
    }
}
