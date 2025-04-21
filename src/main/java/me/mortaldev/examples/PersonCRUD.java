package me.mortaldev.examples;

import me.mortaldev.crudapi.CRUD;
import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.GSON;
import me.mortaldev.crudapi.interfaces.Handler;

import java.util.HashMap;
import java.util.Optional;

class PersonCRUD extends CRUD<Person> {

  public PersonCRUD(Handler handler) {
    super(handler);
  }

  private static class SingletonHelper {
    private static final PersonCRUD personCRUD = new PersonCRUD(GSON.getInstance());
  }

  public static PersonCRUD getPersonCRUD(){
    return SingletonHelper.personCRUD;
  }

  @Override
  public Class<Person> getClazz() {
    return Person.class;
  }

  @Override
  public CRUDAdapters getCRUDAdapters() {
    return new CRUDAdapters().addTypeAdapters(new HashMap<>());
  }

  @Override
  public String getPath() {
    var path = PersonCRUD.class.getResource("PersonCrud.class").getPath();
    // Use `Main.getInstance().getDataFolder().getPath()` for BukkitAPI
    return path + "/people/";
  }

  public Optional<Person> getData(String id) {
    return super.getData(id);
  }
}
