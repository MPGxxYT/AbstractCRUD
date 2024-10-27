package me.mortaldev.examples;

import me.mortaldev.crudapi.CRUD;
import me.mortaldev.crudapi.CRUDManager;

class PersonManager extends CRUDManager<Person> {

  private static final class SingletonHolder {
    private static final PersonManager INSTANCE = new PersonManager();
  }

  public static PersonManager getInstance() {
    return SingletonHolder.INSTANCE;
  }

  @Override
  public CRUD<Person> getCRUD() {
    return PersonCRUD.getPersonCRUD();
  }

  @Override
  public void log(String string) {
    System.out.println(string);
  }
}
