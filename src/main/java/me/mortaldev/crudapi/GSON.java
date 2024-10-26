package me.mortaldev.crudapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;

public class GSON {

    /**
     * Retrieves a JSON object from a file and converts it to the specified class.
     *
     * @param file the file from which to retrieve the JSON object
     * @param clazz the class to which the JSON object should be converted
     * @return an instance of the specified class with the data from the JSON object, or null if the file does not exist
     * @param <T> the type of the class to which the JSON object should be converted
     */
    public static <T> T getJsonObject(File file, Class<T> clazz){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!file.exists()){
            return null;
        }

        try (Reader reader = new FileReader(file)){
            return gson.fromJson(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getJsonObject(File file, Class<T> clazz, HashMap<Class<?>, Object> typeAdapterHashMap){
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        if (!typeAdapterHashMap.isEmpty()) {
            typeAdapterHashMap.forEach(gsonBuilder::registerTypeAdapter);
        }
        Gson gson = gsonBuilder.create();
        if (!file.exists()){
            return null;
        }

        try (Reader reader = new FileReader(file)){
            return gson.fromJson(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves a JSON object to a file in a pretty-printed format.
     *
     * @param file the file to save the JSON object to
     * @param object the JSON object to be saved
     */
    public static void saveJsonObject(File file, Object object){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            try (Writer writer = new FileWriter(file, false)) {
                gson.toJson(object, writer);
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveJsonObject(File file, Object object, HashMap<Class<?>, Object> typeAdapterHashMap){
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        typeAdapterHashMap.forEach(gsonBuilder::registerTypeAdapter);
        Gson gson = gsonBuilder.create();
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            try (Writer writer = new FileWriter(file, false)) {
                gson.toJson(object, writer);
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}