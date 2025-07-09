A library I made to make simple GSON CRUD Operations with classes. It's built to allow any type of CRUD based on the interface. 

The `examples` package has an example of how to use this library.
I'll go into detail explaining it in the future.

# Usage

Into your JavaPlugin class, register with this:
```java
    CRUDRegistry.getInstance()
        .scanAndRegister(this.getClass().getClassLoader(), "me.mortaldev.<dir>"); // example: me.mortaldev.jbjuly4th
    CRUDRegistry.getInstance().initialize();
```

Registering the Managers [works the same with both types]
```java
@AutoRegister // Makes it discoverable
public class Manager extends SingleCRUD<MyClazz> {
  private Manager() {
    super(Jackson.getInstance());
    CRUDRegistry.getInstance().register(this); // Registers it with the CRUD system, otherwise data wont load.
  }
}
```
or
```java
@AutoRegister // Makes it discoverable
public class Manager extends CRUDManager<MyClazz> {
  private Manager() {
    super(Jackson.getInstance());
    CRUDRegistry.getInstance().register(this); // Registers it with the CRUD system, otherwise data wont load.
  }
}
```
