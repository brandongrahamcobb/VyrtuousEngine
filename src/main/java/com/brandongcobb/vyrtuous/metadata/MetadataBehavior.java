public interface MetadataBehavior<T> {

    String serialize(T value);

    T deserialize(String data);
}
