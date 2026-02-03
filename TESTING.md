# AbstractCRUD - Testing Guide

## Test Setup

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

### Test Directory Structure

```
src/
├── main/java/...
└── test/java/me/mortaldev/crudapi/
    ├── CRUDTest.java
    ├── CRUDManagerTest.java
    ├── SingleCRUDTest.java
    ├── CRUDAdaptersTest.java
    ├── CRUDRegistryTest.java
    ├── handlers/
    │   ├── JacksonTest.java
    │   └── GSONTest.java
    └── testutil/
        └── TestEntity.java
```

---

## Automated Tests (JUnit/Mockito)

### 1. CRUDTest.java

```java
@ExtendWith(MockitoExtension.class)
class CRUDTest {

    @TempDir
    Path tempDir;

    // --- getData() ---

    @Test
    void getData_withValidId_returnsEntity()

    @Test
    void getData_withNonExistentId_returnsNull()

    @Test
    void getData_withNullId_returnsNull()

    @Test
    void getData_withEmptyId_returnsNull()

    @Test
    void getData_withMalformedJson_returnsNullOrThrows()

    // --- saveData() ---

    @Test
    void saveData_withValidEntity_createsJsonFile()

    @Test
    void saveData_withValidEntity_writesCorrectContent()

    @Test
    void saveData_createsDirectoryIfMissing()

    @Test
    void saveData_withNullEntity_handlesGracefully()

    @Test
    void saveData_overwritesExistingFile()

    @Test
    void saveData_noTmpFileLeftAfterSuccess()

    // --- deleteData() ---

    @Test
    void deleteData_withExistingEntity_deletesFile()

    @Test
    void deleteData_withNonExistentEntity_handlesGracefully()

    @Test
    void deleteData_withNullEntity_handlesGracefully()

    // --- Atomic Write ---

    @Test
    void saveData_usesAtomicWritePattern()

    @Test
    void saveData_cleansTmpFileOnFailure()
}
```

### 2. CRUDManagerTest.java

```java
@ExtendWith(MockitoExtension.class)
class CRUDManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    CRUDRegistry mockRegistry;

    // --- add() ---

    @Test
    void add_withNewEntity_addsToSet()

    @Test
    void add_withNewEntity_savesToFile()

    @Test
    void add_withDuplicateId_doesNotAddDuplicate()

    @Test
    void add_withNullEntity_handlesGracefully()

    @Test
    void add_withSaveFalse_doesNotWriteFile()

    @Test
    void add_updatesIdCache()

    // --- remove() ---

    @Test
    void remove_withExistingEntity_removesFromSet()

    @Test
    void remove_withExistingEntity_deletesFile()

    @Test
    void remove_withNonExistentEntity_handlesGracefully()

    @Test
    void remove_withDeleteFalse_keepsFile()

    @Test
    void remove_updatesIdCache()

    // --- update() ---

    @Test
    void update_withExistingEntity_updatesInSet()

    @Test
    void update_withExistingEntity_savesToFile()

    @Test
    void update_withNonExistentEntity_handlesGracefully()

    @Test
    void update_withUpdateFileFalse_doesNotWriteFile()

    // --- getByID() ---

    @Test
    void getByID_withExistingId_returnsEntity()

    @Test
    void getByID_withNonExistentId_returnsEmpty()

    @Test
    void getByID_withNullId_returnsEmpty()

    @Test
    void getByID_withEmptyId_returnsEmpty()

    @Test
    void getByID_usesCache_O1Lookup()

    // --- getByIDCreate() ---

    @Test
    void getByIDCreate_withExistingId_returnsExisting()

    @Test
    void getByIDCreate_withNewId_createsNew()

    @Test
    void getByIDCreate_withAddTrue_addsToSet()

    @Test
    void getByIDCreate_withAddFalse_doesNotAddToSet()

    // --- load() ---

    @Test
    void load_withJsonFiles_loadsAllEntities()

    @Test
    void load_ignoresNonJsonFiles()

    @Test
    void load_withEmptyDirectory_loadsNothing()

    @Test
    void load_createsDirectoryIfMissing()

    @Test
    void load_populatesIdCache()

    @Test
    void load_calledMultipleTimes_noDuplicates()

    // --- loadByID() ---

    @Test
    void loadByID_withExistingFile_reloadsEntity()

    @Test
    void loadByID_withNonExistentFile_handlesGracefully()

    @Test
    void loadByID_updatesCache()

    // --- getSet() ---

    @Test
    void getSet_returnsAllEntities()

    @Test
    void getSet_withLazyLoadTrue_loadsIfEmpty()

    // --- Thread Safety ---

    @Test
    void add_fromMultipleThreads_noRaceCondition()

    @Test
    void remove_fromMultipleThreads_noRaceCondition()

    @Test
    void getByID_duringConcurrentWrites_noException()
}
```

### 3. SingleCRUDTest.java

```java
@ExtendWith(MockitoExtension.class)
class SingleCRUDTest {

    @TempDir
    Path tempDir;

    // --- get() ---

    @Test
    void get_withNoFile_returnsConstructedDefault()

    @Test
    void get_withExistingFile_returnsLoadedObject()

    @Test
    void get_calledMultipleTimes_returnsSameInstance()

    // --- load() ---

    @Test
    void load_withExistingFile_loadsFromDisk()

    @Test
    void load_withNoFile_usesConstructDefault()

    @Test
    void load_withMalformedJson_usesConstructDefault()

    // --- save() ---

    @Test
    void save_withNewObject_createsFile()

    @Test
    void save_replacesInMemoryObject()

    @Test
    void save_overwritesExistingFile()

    // --- delete() ---

    @Test
    void delete_withExistingFile_deletesFile()

    @Test
    void delete_withNoFile_handlesGracefully()

    @Test
    void delete_clearsInMemoryObject()
}
```

### 4. CRUDAdaptersTest.java

```java
class CRUDAdaptersTest {

    // --- Jackson Adapters ---

    @Test
    void addSerializer_registersJacksonSerializer()

    @Test
    void addDeserializer_registersJacksonDeserializer()

    @Test
    void addKeySerializer_registersJacksonKeySerializer()

    @Test
    void addModule_registersJacksonModule()

    // --- GSON Adapters ---

    @Test
    void addTypeAdapter_registersGsonAdapter()

    // --- Merging ---

    @Test
    void mergeWith_combinesAdapters()

    @Test
    void mergeWith_thisAdaptersTakePrecedence()

    @Test
    void mergeWith_withNull_returnsThis()

    @Test
    void mergeWith_emptyAdapters_returnsCorrectResult()
}
```

### 5. CRUDRegistryTest.java

```java
class CRUDRegistryTest {

    // --- register() ---

    @Test
    void register_addsManagerToRegistry()

    @Test
    void register_duplicateManager_handlesGracefully()

    // --- initialize() ---

    @Test
    void initialize_callsLoadOnAllManagers()

    @Test
    void initialize_calledMultipleTimes_onlyLoadsOnce()

    // --- getGlobalAdapters() ---

    @Test
    void getGlobalAdapters_returnsSharedInstance()

    @Test
    void getGlobalAdapters_adaptersApplyToAllManagers()

    // --- Logging ---

    @Test
    void setLoggingEnabled_true_enablesLogging()

    @Test
    void setLoggingEnabled_false_disablesLogging()
}
```

### 6. JacksonTest.java

```java
class JacksonTest {

    @TempDir
    Path tempDir;

    // --- getJsonObject() ---

    @Test
    void getJsonObject_withValidJson_deserializes()

    @Test
    void getJsonObject_withNestedObjects_deserializes()

    @Test
    void getJsonObject_withLists_deserializes()

    @Test
    void getJsonObject_withMaps_deserializes()

    @Test
    void getJsonObject_withNullFile_returnsNull()

    @Test
    void getJsonObject_withMalformedJson_throws()

    @Test
    void getJsonObject_appliesCustomModules()

    @Test
    void getJsonObject_appliesCustomSerializers()

    // --- saveJsonObject() ---

    @Test
    void saveJsonObject_writesValidJson()

    @Test
    void saveJsonObject_prettyPrints()

    @Test
    void saveJsonObject_handlesNestedObjects()

    @Test
    void saveJsonObject_handlesLists()

    @Test
    void saveJsonObject_handlesMaps()

    @Test
    void saveJsonObject_appliesCustomSerializers()

    @Test
    void saveJsonObject_usesAtomicWrite()

    // --- JavaTimeModule ---

    @Test
    void serialize_instant_correctFormat()

    @Test
    void deserialize_instant_correctValue()

    @Test
    void serialize_localDateTime_correctFormat()

    @Test
    void deserialize_localDateTime_correctValue()

    // --- Optional Handling ---

    @Test
    void serialize_optionalPresent_correctFormat()

    @Test
    void serialize_optionalEmpty_correctFormat()

    @Test
    void deserialize_optionalPresent_correctValue()

    @Test
    void deserialize_optionalEmpty_correctValue()
}
```

### 7. GSONTest.java

```java
class GSONTest {

    @TempDir
    Path tempDir;

    // Same test structure as JacksonTest

    @Test
    void getJsonObject_withValidJson_deserializes()

    @Test
    void getJsonObject_withNestedObjects_deserializes()

    @Test
    void getJsonObject_withLists_deserializes()

    @Test
    void getJsonObject_withMaps_deserializes()

    @Test
    void getJsonObject_appliesTypeAdapters()

    @Test
    void saveJsonObject_writesValidJson()

    @Test
    void saveJsonObject_prettyPrints()

    @Test
    void saveJsonObject_appliesTypeAdapters()

    @Test
    void saveJsonObject_usesAtomicWrite()
}
```

### 8. TestEntity.java (Test Utility)

```java
public class TestEntity implements CRUD.Identifiable {
    private String id;
    private String name;
    private int value;
    private List<String> tags;
    private Optional<String> description;
    private Instant createdAt;

    // Constructors, getters, setters, equals, hashCode
}
```

---

## Manual Tests (Require Paper Server)

These tests cannot be automated with JUnit/Mockito because they require:
- Actual file system behavior under load
- Real concurrent access patterns
- Server crash simulation
- Plugin lifecycle integration

### Server Integration

- [ ] Plugin loads without errors
- [ ] CRUDRegistry initializes all managers on startup
- [ ] Data persists across server restarts
- [ ] Hot reload (`/reload`) doesn't corrupt data

### Real Concurrency

- [ ] Multiple players triggering saves simultaneously
- [ ] No file corruption under heavy load
- [ ] Server TPS stable during bulk operations

### Crash Recovery

- [ ] Kill server mid-save - no orphaned `.tmp` files
- [ ] Kill server mid-save - no corrupted JSON files
- [ ] Restart after crash - all valid data loads

### File System Edge Cases

- [ ] Very large entities (1MB+ JSON) save/load correctly
- [ ] 1000+ entities in single directory loads correctly
- [ ] Special characters in IDs create valid filenames
- [ ] Read-only file system - graceful error handling

### Memory & Performance

- [ ] No memory leaks over extended uptime
- [ ] Large datasets don't cause OOM
- [ ] Cache improves lookup performance (profile with timings)

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CRUDManagerTest

# Run with coverage
mvn test jacoco:report
```

---

## Version Checklist

Before tagging a release:

- [ ] All automated tests pass (`mvn test`)
- [ ] Manual server tests pass
- [ ] No compiler warnings
- [ ] README.md version updated
- [ ] CHANGELOG.md updated
