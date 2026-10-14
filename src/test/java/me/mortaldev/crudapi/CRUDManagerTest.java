package me.mortaldev.crudapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CRUDManagerTest {

  private final InMemoryCRUD<TestEntity> crud = new InMemoryCRUD<>(TestEntity.class);

  private final CRUDManager<TestEntity> manager =
      new CRUDManager<>(crud, null) {
        @Override
        public void log(String string) {}

        @Override
        public TestEntity getNewInstance(String id) {
          return TestEntity.create(id);
        }
      };

  @Test
  void loadReplacesCacheWithStoredEntities() {
    crud.stored.put("a", new TestEntity("a", "A", 1));
    manager.add(new TestEntity("stale", "S", 1), false);

    manager.load();

    assertEquals(Optional.of(new TestEntity("a", "A", 1)), manager.getByID("a"));
    assertEquals(Optional.empty(), manager.getByID("stale"));
  }

  @Test
  void loadByIdRefreshesOneEntity() {
    crud.stored.put("a", new TestEntity("a", "Old", 1));
    manager.load();
    crud.stored.put("a", new TestEntity("a", "New", 2));

    assertTrue(manager.loadByID("a"));
    assertEquals("New", manager.getByID("a").orElseThrow().name);
    assertFalse(manager.loadByID("missing"));
  }
}
