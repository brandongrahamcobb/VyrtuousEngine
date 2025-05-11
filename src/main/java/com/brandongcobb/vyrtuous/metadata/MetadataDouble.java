package com.brandongcobb.vyrtuous.metadata;

import java.util.Objects;

public class MetadataDouble implements MetadataType<Double> {

    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataDouble;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Double.class);
    }
}
