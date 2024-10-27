package me.mortaldev.examples;

import me.mortaldev.crudapi.CRUD;

import java.util.HashMap;
import java.util.Optional;

class PersonCRUD extends CRUD<Person> {

  private static class SingletonHelper {
    private static final PersonCRUD personCRUD = new PersonCRUD();
  }

  public static PersonCRUD getPersonCRUD(){
    return SingletonHelper.personCRUD;
  }

  @Override
  public Class<Person> getClazz() {
    return Person.class;
  }

  @Override
  public HashMap<Class<?>, Object> getTypeAdapterHashMap() {
    return new HashMap<>();
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
