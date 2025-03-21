package me.mortaldev.examples;

import me.mortaldev.crudapi.CRUD;

class Person implements CRUD.Identifiable {
  private final String firstName;
  private final String lastName;
  private Integer age;

  public Person(String firstName, String lastName, Integer age) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.age = age;
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