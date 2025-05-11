package com.brandongcobb.vyrtuous.metadata;

import com.brandongcobb.vyrtuous.metadata.*;

public interface MetadataBehavior<T> {

    String serialize(T value);

    T deserialize(String data);
}
