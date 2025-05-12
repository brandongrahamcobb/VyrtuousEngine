package com.brandongcobb.metadata;

import java.util.Map;
import java.util.Objects;

public class MetadataMap implements MetadataType<Map<String, Object>> {

    @Override
    public Class<Map<String, Object>> getType() {
        return (Class<Map<String, Object>>) (Object) Map.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MetadataMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Map");
    }

    @Override
    public String toString() {
        return "MetadataMap";
    }
}
