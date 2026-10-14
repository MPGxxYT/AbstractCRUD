# Glossary

**Entity**: a value that knows its own id by implementing `CRUD.Identifiable`. A `CRUDManager` holds entities. A `SingleCRUD` value does not need to be an entity.

**Id**: the key a value is stored under. It is also the file name without `.json`, so it must be non-empty, must not be `.` or `..`, and must not contain `/` or `\`.

**CRUD**: the store for one type in one directory, one `<id>.json` file per value. It owns the file layout, atomic writes and error logging. It does not cache.

**Codec**: converts objects to and from JSON text. `JacksonCodec` and `GsonCodec` are the two implementations. All serialization settings (Jackson modules, Gson type adapters) belong to the codec. Settings shared across CRUDs come from sharing one codec instance.

**Manager** (`CRUDManager`): an in-memory cache of entities in front of a CRUD. Every write goes to both the cache and the CRUD. The factory it is given creates new entities for `getOrCreate`.

**SingleCRUD**: one value, such as plugin settings, stored under a fixed id in a CRUD, with a default used when no file exists.

**Registry** (`CRUDRegistry`): collects managers and SingleCRUDs so `initialize()` can load them all, typically in a plugin's `onEnable()`. Registration happens in their constructors.

**Load**: replace in-memory state with what the CRUD holds (`load()` for everything, `reload(id)` for one entity). Reading from the cache (`get`) never loads.
