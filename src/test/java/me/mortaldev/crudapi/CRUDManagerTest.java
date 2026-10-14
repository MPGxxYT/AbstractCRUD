package me.mortaldev.crudapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import org.junit.jupiter.api.Test;

class CRUDManagerTest {

  private final InMemoryCRUD<TestEntity> crud = new InMemoryCRUD<>(TestEntity.class);
  private final CRUDManager<TestEntity> manager =
      new CRUDManager<>(crud, TestEntity::create, null);

  @Test
  void getReadsOnlyTheCache() {
    crud.stored.put("a", TestEntity.create("a"));

    assertEquals(Optional.empty(), manager.get("a"));
    assertEquals(Optional.empty(), manager.get(null));
  }

  @Test
  void getOrCreateCreatesCachesAndSavesOnce() {
    TestEntity first = manager.getOrCreate("a");
    TestEntity second = manager.getOrCreate("a");

    assertSame(first, second);
    assertEquals(TestEntity.create("a"), crud.stored.get("a"));
    assertEquals(1, crud.saves);
  }

  @Test
  void getOrCreateRejectsFactoryReturningWrongId() {
    CRUDManager<TestEntity> broken =
        new CRUDManager<>(crud, id -> TestEntity.create("other"), null);

    assertThrows(IllegalStateException.class, () -> broken.getOrCreate("a"));
    assertFalse(broken.contains("a"));
  }

  @Test
  void saveInsertsThenReplaces() {
    assertTrue(manager.save(new TestEntity("a", "Old", 1)));
    assertTrue(manager.save(new TestEntity("a", "New", 2)));

    assertEquals("New", manager.get("a").orElseThrow().name);
    assertEquals("New", crud.stored.get("a").name);
    assertEquals(1, manager.all().size());
  }

  @Test
  void saveRejectsNullAndNullId() {
    assertFalse(manager.save(null));
    assertFalse(manager.save(new TestEntity(null, "x", 1)));
    assertEquals(0, crud.saves);
  }

  @Test
  void removeClearsCacheAndStore() {
    manager.save(TestEntity.create("a"));

    assertTrue(manager.remove("a"));
    assertFalse(manager.contains("a"));
    assertFalse(crud.stored.containsKey("a"));
    assertFalse(manager.remove("a"));
  }

  @Test
  void allIsReadOnlyLiveView() {
    var all = manager.all();
    manager.save(TestEntity.create("a"));

    assertEquals(List.of(TestEntity.create("a")), List.copyOf(all));
    assertThrows(UnsupportedOperationException.class, () -> all.clear());
  }

  @Test
  void loadReplacesCacheWithStoredEntities() {
    manager.save(TestEntity.create("stale"));
    crud.stored.clear();
    crud.stored.put("a", new TestEntity("a", "A", 1));

    manager.load();

    assertEquals(Optional.of(new TestEntity("a", "A", 1)), manager.get("a"));
    assertFalse(manager.contains("stale"));
  }

  @Test
  void loadKeysByEntityId() {
    crud.stored.put("file-name", new TestEntity("entity-id", "A", 1));

    manager.load();

    assertTrue(manager.contains("entity-id"));
  }

  @Test
  void reloadRefreshesOneEntityAndLeavesCacheWhenMissing() {
    manager.save(new TestEntity("a", "Old", 1));
    crud.stored.put("a", new TestEntity("a", "New", 2));

    assertTrue(manager.reload("a"));
    assertEquals("New", manager.get("a").orElseThrow().name);

    manager.save(TestEntity.create("b"));
    crud.stored.remove("b");
    assertFalse(manager.reload("b"));
    assertTrue(manager.contains("b"));
  }

  @Test
  void registryInitializeLoadsManager() {
    CRUDRegistry registry = new CRUDRegistry();
    CRUDManager<TestEntity> registered = new CRUDManager<>(crud, TestEntity::create, registry);
    crud.stored.put("a", TestEntity.create("a"));

    registry.initialize();

    assertTrue(registered.contains("a"));
  }

  @Test
  void iteratingAllWhileWritingNeverThrows() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      Future<?> writer =
          pool.submit(
              () -> {
                for (int i = 0; i < 5_000; i++) {
                  manager.save(TestEntity.create("e" + i));
                  if (i % 3 == 0) {
                    manager.remove("e" + (i / 2));
                  }
                }
              });
      Future<?> reader =
          pool.submit(
              () -> {
                while (!writer.isDone()) {
                  for (TestEntity e : manager.all()) {
                    manager.get(e.getId());
                  }
                }
              });
      writer.get(30, TimeUnit.SECONDS);
      reader.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }
  }
}
