package com.brandongcobb.vyrtuous.metadata;

import com.brandongcobb.vyrtuous.metadata.*;

public class MetadataUtils {

    public static <T> String serialize(T value, MetadataType<T> type) {
        try {
            return type.getBehavior().serialize(value);
        } catch (Exception e) {
            throw new RuntimeException("Serialize not supported for type: " + type.getType(), e);
        }
    }

    public static <T> T deserialize(String data, MetadataType<T> type) {
        try {
            return type.getBehavior().deserialize(data);
        } catch (Exception e) {
            throw new RuntimeException("Deserialize not supported for type: " + type.getType(), e);
        }
    }
}
