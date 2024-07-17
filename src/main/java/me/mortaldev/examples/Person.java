package me.mortaldev.examples;

import me.mortaldev.crudapi.AbstractCRUD;

class Person implements AbstractCRUD.Identifiable {
  private final String firstName;
  private final String lastName;
  private Integer age;

  public Person(String firstName, String lastName, Integer age) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.age = age;
  }

  public void save() {
    PersonCRUD.getPersonCRUD().saveData(this);
  }

  public void delete() {
    PersonCRUD.getPersonCRUD().deleteData(this);
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  @Override
  public String getID() {
    return firstName.toLowerCase() + "_" + lastName.toLowerCase();
  }
}