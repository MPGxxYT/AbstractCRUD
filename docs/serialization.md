# Serialization and custom types

A `Codec` turns your objects into JSON text and back. AbstractCRUD ships two: `JacksonCodec` and `GsonCodec`. This guide covers which one to pick, the rules each one has for your classes, and how to add serializers for types like Bukkit's `Location`.

## Jackson or Gson

Both write pretty-printed JSON and both work for plugin data. They read your classes differently:

| | Jackson (`JacksonCodec`) | Gson (`GsonCodec`) |
| --- | --- | --- |
| Which fields are saved | Public fields, and every property with a public getter (`getX()` / `isX()`) | Every field, private ones included, except `transient` and `static` |
| How an object is rebuilt | A no-argument constructor (may be private), a `@JsonCreator` constructor, or a record's constructor; then setters or public fields | A no-argument constructor (may be private) or a record's constructor; then fields directly |
| Skipping a field | `@JsonIgnore` on the field or getter | `transient` keyword |
| A field in the file that the class doesn't have | **Fails the read** by default (see below) | Ignored |
| Custom serializers | `JsonSerializer` / `JsonDeserializer`, grouped in a `SimpleModule` | `JsonSerializer` / `JsonDeserializer` or `TypeAdapter` |

If you don't know which to use, Gson needs less code in your classes. If you already have Jackson serializers, like the `Location` ones below, use Jackson.

> [!WARNING]
> **Jackson needs a getter and a setter for every private field.** A private field with neither is not saved, and no error is shown.
>
> **Gson without a no-argument constructor skips your field defaults.** Gson then creates the object without running any constructor, so `private int maxPlayers = 8;` reads as `0` when the file doesn't mention it. Give the class a no-argument constructor (it can be private) or make it a record. See [Constructors](#constructors).

## Constructors

AbstractCRUD itself never needs a particular constructor. The JSON library does: when it reads a file, it has to create your object before filling it in. These are the ways to give it one:

| Option | Jackson | Gson |
| --- | --- | --- |
| No-argument constructor | Works, `private` included | Works, `private` included |
| A constructor with arguments, annotated | `@JsonCreator` on the constructor and `@JsonProperty("name")` on each parameter | Not supported |
| Java `record` | Works | Works |
| None of the above | The read fails | Creates the object without a constructor, so field defaults are skipped |

A private no-argument constructor lets the JSON library build the object while your own code keeps using the constructor with arguments:

```java
public class AdminData {
    private Location spawn;

    private AdminData() {}                       // for the JSON library only

    public AdminData(Location spawn) { this.spawn = spawn; }

    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
}
```

The same class with Jackson annotations instead, and no empty constructor:

```java
@JsonCreator
public AdminData(@JsonProperty("spawn") Location spawn) { this.spawn = spawn; }
```

A record needs nothing extra. It has no setters, so you save a new record to change a value:

```java
public record AdminData(Location spawn) {}

adminData.save(new AdminData(player.getLocation()));
```

The default object for a `SingleCRUD` and the factory for a `CRUDManager` are separate from all this. They can call any constructor: `() -> new AdminData(null)`, `id -> new Arena(id, 8)`.

## Configure a codec

Settings go in the constructor, as a callback that receives the library's own builder. The codec is built once, so create it in `onEnable` and pass the same instance to every `CRUD` that needs the same settings.

```java
Codec jackson = new JacksonCodec(mapper -> {
    mapper.registerModule(BukkitJson.module());    // your serializers
    mapper.registerModule(new JavaTimeModule());   // java.time types
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
});

Codec gson = new GsonCodec(builder -> builder
    .registerTypeAdapter(Location.class, new LocationGsonAdapter())
    .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
```

`mapper` is a Jackson [`ObjectMapper`](https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/latest/com/fasterxml/jackson/databind/ObjectMapper.html) and `builder` is a Gson [`GsonBuilder`](https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/GsonBuilder.html), so anything their own docs describe works here.

`FAIL_ON_UNKNOWN_PROPERTIES` set to `false` lets Jackson read files that contain a field your class no longer has, for example after you rename or remove one. With the default `true`, such a file fails to read, and a `SingleCRUD` falls back to its default.

## Bukkit Location

`Location` holds a reference to a live `World`, so neither library can save it on its own. Bukkit already knows how to turn a location into simple values: `location.serialize()` returns a map with the world name, `x`, `y`, `z`, `yaw` and `pitch`, and `Location.deserialize(map)` turns that map back into a location. The serializers below use those two methods.

### With Jackson

A serializer writes the map, a deserializer reads it back:

```java
public class LocationSerializer extends JsonSerializer<Location> {
    @Override
    public void serialize(Location location, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeObject(location == null ? null : location.serialize());
    }
}
```

```java
public class LocationDeserializer extends JsonDeserializer<Location> {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    @Override
    public Location deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        Map<String, Object> values = parser.readValueAs(MAP);
        return values.isEmpty() ? null : Location.deserialize(values);
    }
}
```

Jackson takes serializers in a **module**. Put yours in one place so every codec uses the same set:

```java
public final class BukkitJson {
    private BukkitJson() {}

    public static SimpleModule module() {
        return new SimpleModule("BukkitJson")
            .addSerializer(Location.class, new LocationSerializer())
            .addDeserializer(Location.class, new LocationDeserializer());
    }
}
```

```java
Codec codec = new JacksonCodec(mapper -> mapper.registerModule(BukkitJson.module()));
```

Any field of type `Location`, at any depth in the objects you save, now goes through these classes. The file looks like this:

```json
{
  "spawn" : {
    "world" : "world",
    "x" : 12.5,
    "y" : 64.0,
    "z" : -3.5,
    "pitch" : 0.0,
    "yaw" : 90.0
  }
}
```

To use `Location` as a **map key** (`Map<Location, String>`), Jackson also needs a key serializer and key deserializer that turn a location into one string. Add them to the same module with `addKeySerializer(Location.class, ...)` and `addKeyDeserializer(Location.class, ...)`.

### With Gson

One class can do both directions:

```java
public class LocationGsonAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
    private static final Type MAP = new TypeToken<Map<String, Object>>() {}.getType();

    @Override
    public JsonElement serialize(Location location, Type type, JsonSerializationContext context) {
        return context.serialize(location.serialize());
    }

    @Override
    public Location deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        Map<String, Object> values = context.deserialize(json, MAP);
        return Location.deserialize(values);
    }
}
```

```java
Codec codec = new GsonCodec(builder -> builder.registerTypeAdapter(Location.class, new LocationGsonAdapter()));
```

### The world must be loaded

`Location.deserialize` looks the world up by name and fails if it isn't loaded. Two things follow:

- Load your data after the worlds exist. `load: POSTWORLD` in `plugin.yml`, which is the default, makes `onEnable` run after the worlds load.
- A location in a world that was deleted or renamed makes the whole file fail to read. The console shows `Failed to read <file>`, and a `SingleCRUD` falls back to its default. See the warning in [Storing one object](single-crud.md#when-is-the-default-saved).

## Other common types

| Type | Jackson | Gson |
| --- | --- | --- |
| `String`, numbers, `boolean`, enums, `List`, `Map`, arrays | Works | Works |
| `UUID` | Works, saved as a string | Works, saved as a string |
| `LocalDate`, `Instant` and other `java.time` types | Register `new JavaTimeModule()` (the dependency is included) | Needs your own adapter |
| `Optional` fields | Needs the `jackson-datatype-jdk8` dependency and `Jdk8Module` | Register `OptionalTypeAdapter.FACTORY` from `me.mortaldev.abstractcrud.typeadapters` |
| `Location` | Serializer module (above) | Adapter (above) |
| `ItemStack` | Needs a serializer. On Paper, `itemStack.serializeAsBytes()` and `ItemStack.deserializeBytes(bytes)` stored as a Base64 string keep every item detail. | Same approach, as an adapter |
| `Player`, `World`, `Entity`, `Block` | Don't store these. Store the UUID or name and look the object up when you need it. | Same |

## Check a serializer before relying on it

A codec can be called directly, without files. A quick round trip catches most mistakes:

```java
Codec codec = new JacksonCodec(mapper -> mapper.registerModule(BukkitJson.module()));
String json = codec.encode(serverData);
getLogger().info(json);
ServerData back = codec.decode(json, ServerData.class);
```

`encode` and `decode` throw an `IOException` that says what went wrong, which is easier to read than the logged failure from a `CRUD`.
