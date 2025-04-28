package com.brandongcobb.vyrtuous.metadata;

/**
 * A holder for a metadata entry, pairing a {@link MetadataKey} with its associated value.
 *
 * <p>This class encapsulates a single metadata key-value pair, providing access to both
 * the key descriptor and the stored value.
 *
 * @param <T> the Java type of the metadata value
 */
public class MetadataHolder<T> {

    private final MetadataKey<T> key;
    private final T value;

    /**
     * Constructs a new metadata holder with the given key and value.
     *
     * @param key the metadata key descriptor
     * @param value the value associated with the key
     */
    public MetadataHolder(MetadataKey<T> key, T value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the metadata key descriptor associated with this holder.
     *
     * @return the {@code MetadataKey} for this value
     */
    public MetadataKey<T> getKey() {
        return key;
    }

    /**
     * Returns the value stored in this metadata holder.
     *
     * @return the metadata value
     */
    public T getValue() {
        return value;
    }
}
