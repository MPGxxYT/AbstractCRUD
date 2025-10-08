package me.mortaldev.crudapi;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A container for registering custom type adapters and serializers for different JSON processing
 * libraries. This class is designed to support both Jackson and GSON configurations within the same
 * CRUD operation.
 */
public class CRUDAdapters {

  /**
   * A single, unified module for all custom serializers and deserializers added directly. Using a
   * single module prevents race conditions and ensures predictable registration order.
   */
  private final SimpleModule primaryJacksonModule = new SimpleModule();

  /**
   * A set to hold any additional, pre-configured Jackson modules (e.g., JavaTimeModule). This
   * restores the flexibility to use third-party modules.
   */
  private final Set<Module> additionalJacksonModules = new HashSet<>();

  /** A map to hold type adapters specifically for the GSON library. */
  private final Map<Class<?>, Object> gsonTypeAdapters = new HashMap<>();

  /**
   * Returns a set containing all configured Jackson modules, including the primary module with
   * custom serializers and any additionally added modules.
   *
   * @return An unmodifiable set containing all Jackson modules.
   */
  public Set<Module> getModules() {
    Set<Module> allModules = new HashSet<>(additionalJacksonModules);
    allModules.add(primaryJacksonModule);
    return Collections.unmodifiableSet(allModules);
  }

  /**
   * Adds a pre-configured Jackson Module, such as JavaTimeModule.
   *
   * @param module The module to add.
   * @return this, for method chaining.
   */
  public CRUDAdapters addModule(Module module) {
    this.additionalJacksonModules.add(module);
    return this;
  }

  /**
   * Registers a custom Jackson serializer for the given class.
   *
   * @param clazz The class the serializer should be registered for.
   * @param serializer The serializer to register.
   * @return this, for method chaining.
   */
  public <T> CRUDAdapters addSerializer(Class<? extends T> clazz, JsonSerializer<T> serializer) {
    this.primaryJacksonModule.addSerializer(clazz, serializer);
    return this;
  }

  /**
   * Registers a custom Jackson key serializer for the given class.
   *
   * @param clazz The class the key serializer should be registered for.
   * @param serializer The key serializer to register.
   * @return this, for method chaining.
   */
  public <T> CRUDAdapters addKeySerializer(Class<? extends T> clazz, JsonSerializer<T> serializer) {
    this.primaryJacksonModule.addKeySerializer(clazz, serializer);
    return this;
  }

  /**
   * Registers a custom Jackson key deserializer for the specified class.
   *
   * @param clazz The class for which the key deserializer should be registered.
   * @param deserializer The key deserializer to register.
   * @return this, for method chaining.
   */
  public CRUDAdapters addKeyDeserializer(Class<?> clazz, KeyDeserializer deserializer) {
    this.primaryJacksonModule.addKeyDeserializer(clazz, deserializer);
    return this;
  }

  /**
   * Registers a custom Jackson deserializer for the specified class.
   *
   * @param clazz The class for which the deserializer should be registered.
   * @param deserializer The deserializer to register.
   * @return this, for method chaining.
   */
  public <T> CRUDAdapters addDeserializer(
      Class<T> clazz, JsonDeserializer<? extends T> deserializer) {
    this.primaryJacksonModule.addDeserializer(clazz, deserializer);
    return this;
  }

  // </editor-fold>

  // <editor-fold desc="GSON Methods">

  /**
   * Returns an unmodifiable map of the registered GSON type adapters.
   *
   * @return An unmodifiable map of the registered type adapters.
   */
  public Map<Class<?>, Object> getTypeAdapters() {
    return Collections.unmodifiableMap(gsonTypeAdapters);
  }

  /**
   * Registers a GSON type adapter for the specified class.
   *
   * @param clazz The class for which the adapter should be registered.
   * @param typeAdapter The GSON type adapter instance.
   * @return this, for method chaining.
   */
  public CRUDAdapters addTypeAdapter(Class<?> clazz, Object typeAdapter) {
    this.gsonTypeAdapters.put(clazz, typeAdapter);
    return this;
  }

  /**
   * Adds multiple GSON type adapters from a given map.
   *
   * @param typeAdapters A map containing classes and their corresponding type adapters.
   * @return this, for method chaining.
   */
  public CRUDAdapters addTypeAdapters(Map<Class<?>, Object> typeAdapters) {
    this.gsonTypeAdapters.putAll(typeAdapters);
    return this;
  }
  // </editor-fold>

  /**
   * Merges this CRUDAdapters instance with another, creating a new instance with the combined
   * configurations. Adapters from 'this' instance take precedence over the 'other' instance if
   * there are conflicts.
   *
   * @param other The CRUDAdapters instance to merge with.
   * @return A new CRUDAdapters instance containing the merged adapters and modules.
   */
  public CRUDAdapters mergeWith(CRUDAdapters other) {
    CRUDAdapters merged = new CRUDAdapters();
    merged.addTypeAdapters(other.gsonTypeAdapters);
    merged.addTypeAdapters(this.gsonTypeAdapters); // this overwrites other
    other.additionalJacksonModules.forEach(merged::addModule);
    merged.addModule(other.primaryJacksonModule);
    merged.addModule(this.primaryJacksonModule); // this is added last
    return merged;
  }
}
