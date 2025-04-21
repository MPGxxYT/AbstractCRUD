package me.mortaldev.crudapi;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.util.HashMap;

public class CRUDAdapters {
  private final HashMap<Class<?>, Object> typeAdapterHashMap = new HashMap<>();
  private final SimpleModule module = new SimpleModule();

  /**
   * Determines whether this instance is empty, meaning no type adapters have been registered.
   *
   * @return {@code true} if this instance is empty, {@code false} otherwise
   */
  public boolean isEmpty() {
    return typeAdapterHashMap.isEmpty();
  }

  /**
   * Returns a map containing all type adapters that were registered via
   * {@link #addTypeAdapters(HashMap)} addTypeAdapter(Class, Object)} or {@link #addTypeAdapters(HashMap)}.
   * The map is unmodifiable.
   *
   * @return an unmodifiable map containing all registered type adapters
   */
  public HashMap<Class<?>, Object> getTypeAdapters() {
    return typeAdapterHashMap;
  }

/**
 * Adds multiple type adapters to the internal type adapter hash map.
 *
 * @param typeAdapters a map containing classes and their corresponding type adapters
 * @return this, for method chaining
 */
  public CRUDAdapters addTypeAdapters(HashMap<Class<?>, Object> typeAdapters) {
    typeAdapterHashMap.putAll(typeAdapters);
    return this;
  }


  /**
   * Gets the {@link SimpleModule} containing all serializers and deserializers that were registered
   * via {@link #addSerializer(Class, StdSerializer)} or {@link #addDeserializer(Class, StdDeserializer)}.
   * @return the module containing the registered serializers and deserializers
   */
  public SimpleModule getModule() {
    return module;
  }

  /**
   * Registers a custom serializer for the given class.
   * @param clazz the class the serializer should be registered for
   * @param serializer the serializer to register
   * @return this, for method chaining
   */
  public <T> CRUDAdapters addSerializer(Class<T> clazz, StdSerializer<T> serializer) {
    module.addSerializer(clazz, serializer);
    return this;
  }

  /**
   * Registers a custom deserializer for the specified class.
   *
   * @param clazz the class for which the deserializer should be registered
   * @param serializer the deserializer to register
   * @return this, for method chaining
   */
  public <T> CRUDAdapters addDeserializer(Class<T> clazz, StdDeserializer<T> serializer) {
    module.addDeserializer(clazz, serializer);
    return this;
  }
}
