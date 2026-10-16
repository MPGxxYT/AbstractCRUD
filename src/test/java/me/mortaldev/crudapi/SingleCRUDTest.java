package me.mortaldev.crudapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import me.mortaldev.abstractcrud.crudapi.CRUD;
import me.mortaldev.abstractcrud.crudapi.SingleCRUD;
import me.mortaldev.abstractcrud.crudapi.codec.GsonCodec;
import me.mortaldev.abstractcrud.crudapi.loading.CRUDRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleCRUDTest {

  /** A settings object that does not implement Identifiable. */
  public static class Settings {
    public String motd = "Welcome";
    public int maxPlayers = 20;
  }

  private final InMemoryCRUD<Settings> crud = new InMemoryCRUD<>(Settings.class);
  private final SingleCRUD<Settings> single =
      new SingleCRUD<>(crud, "settings", Settings::new, null);

  @Test
  void getReturnsDefaultWhenNothingStoredAndDoesNotSaveIt() {
    assertEquals("Welcome", single.get().motd);
    assertEquals(0, crud.saves);
  }

  @Test
  void getLoadsOnceAndCaches() {
    Settings stored = new Settings();
    stored.motd = "Stored";
    crud.stored.put("settings", stored);

    assertSame(stored, single.get());
    crud.stored.put("settings", new Settings());
    assertSame(stored, single.get());
  }

  @Test
  void saveReplacesAndPersistsUnderFixedId() {
    Settings changed = new Settings();
    changed.maxPlayers = 50;

    assertTrue(single.save(changed));
    assertSame(changed, single.get());
    assertSame(changed, crud.stored.get("settings"));
  }

  @Test
  void deleteThenLoadFallsBackToDefault() {
    Settings changed = new Settings();
    changed.motd = "Changed";
    single.save(changed);

    assertTrue(single.delete());
    single.load();

    assertEquals("Welcome", single.get().motd);
    assertFalse(single.delete());
  }

  @Test
  void registryInitializeLoadsIt() {
    CRUDRegistry registry = new CRUDRegistry();
    SingleCRUD<Settings> registered = new SingleCRUD<>(crud, "settings", Settings::new, registry);
    Settings stored = new Settings();
    crud.stored.put("settings", stored);

    registry.initialize();

    assertSame(stored, registered.get());
  }

  @Test
  void roundTripsThroughFiles(@TempDir Path dir) {
    CRUD<Settings> fileCrud = new CRUD<>(Settings.class, dir, new GsonCodec());
    Settings changed = new Settings();
    changed.motd = "On disk";
    new SingleCRUD<>(fileCrud, "settings", Settings::new, null).save(changed);

    SingleCRUD<Settings> fresh = new SingleCRUD<>(fileCrud, "settings", Settings::new, null);

    assertEquals("On disk", fresh.get().motd);
  }
}
