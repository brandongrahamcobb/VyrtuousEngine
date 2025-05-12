package com.brandongcobb.metadata;

import com.brandongcobb.metadata.*;

public interface MetadataBehavior<T> {

    String serialize(T value);

    T deserialize(String data);
}
