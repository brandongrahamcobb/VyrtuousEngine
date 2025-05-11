package com.brandongcobb.vyrtuous.metadata;

import java.util.List;
import java.util.Objects;

public class MetadataList<T> implements MetadataType<List<T>> {
    private final MetadataType<T> elementType;

    public MetadataList(MetadataType<T> elementType) {
        this.elementType = elementType;
    }

    @Override
    public Class<List<T>> getType() {
        return (Class<List<T>>)(Object) List.class;
    }

    public MetadataType<T> getElementType() {
        return elementType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetadataList)) return false;
        MetadataList<?> that = (MetadataList<?>) o;
        return elementType.equals(that.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType);
    }

    @Override
    public String toString() {
        return "MetadataList<" + elementType.toString() + ">";
    }
}

