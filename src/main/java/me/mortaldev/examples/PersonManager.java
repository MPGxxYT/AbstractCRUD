package me.mortaldev.examples;

class PersonManager {

  static PersonCRUD personCRUD = new PersonCRUD();

  static void loadPeople(){
    Person johnnyRockets = personCRUD.getData("johnny_rockets");
    System.out.println(johnnyRockets.getFirstName());
    System.out.println(johnnyRockets.getLastName());
    System.out.println(johnnyRockets.getAge());
    johnnyRockets.setAge(26);
    personCRUD.saveData(johnnyRockets);
    personCRUD.deleteData(johnnyRockets);
    // Or you can use these if you added it to your class.
    johnnyRockets.save();
    johnnyRockets.delete();
  }
}
