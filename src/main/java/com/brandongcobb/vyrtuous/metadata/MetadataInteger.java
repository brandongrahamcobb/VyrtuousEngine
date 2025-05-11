package com.brandongcobb.vyrtuous.metadata;

import java.util.Objects;

public class MetadataInteger implements MetadataType<Integer> {

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataInteger;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.class);
    }
}
