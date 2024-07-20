package me.mortaldev.examples;

import me.mortaldev.crudapi.CRUD;

class PersonCRUD extends CRUD<Person> {

  private static class SingletonHelper {
    private static final PersonCRUD personCRUD = new PersonCRUD();
  }

  public static PersonCRUD getPersonCRUD(){
    return SingletonHelper.personCRUD;
  }

  @Override
  public String getPath() {
    var path = PersonCRUD.class.getResource("PersonCrud.class").getPath();
    // Use `Main.getInstance().getDataFolder().getPath()` for BukkitAPI
    return path + "/people/";
  }

  public Person getData(String id) {
    return super.getData(id, Person.class).orElse(new Person("default", "name", 55));
  }
}
