# Shading

A Paper plugin usually ships as one jar that contains its libraries. The Maven Shade plugin builds that jar. **Relocating** moves the library's classes into your own package while shading, so two plugins that both include AbstractCRUD, possibly different versions, don't load each other's copy.

Since v2.0.1 every class in AbstractCRUD is under `me.mortaldev.abstractcrud`, so one relocation rule covers the whole library:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.3</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>me.mortaldev.abstractcrud</pattern>
                        <shadedPattern>com.example.myplugin.libs.abstractcrud</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Replace `com.example.myplugin` with your plugin's own package. Your source code keeps importing `me.mortaldev.abstractcrud...`; Shade rewrites those references in the built jar.

> [!IMPORTANT]
> The relocation pattern has to match the library's package. v2.0.0 used `me.mortaldev.crudapi` and `me.mortaldev.typeadapters`. With v2.0.0 the rule above matches nothing, and the library is shaded without being relocated. Use v2.0.1 or later.

## Check that it worked

Build the plugin and list the classes in the jar:

```bash
jar tf target/MyPlugin-1.0.jar
```

The AbstractCRUD classes should appear under `com/example/myplugin/libs/abstractcrud/`, and nothing should remain under `me/mortaldev/abstractcrud/`.

## Jackson and Gson

The relocation above moves AbstractCRUD only. Jackson and Gson come along as dependencies and are shaded under their own packages (`com.fasterxml.jackson`, `com.google.gson`). Paper already provides Gson to plugins. If two plugins on one server shade different Jackson versions and you see `NoSuchMethodError` or `ClassCastException` naming Jackson classes, relocate `com.fasterxml.jackson` as well.
