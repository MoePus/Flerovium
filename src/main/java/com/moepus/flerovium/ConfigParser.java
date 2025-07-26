package com.moepus.flerovium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class ConfigParser {
    private static Config config;
    static Supplier<String> config_path = ()->String.valueOf(Paths.get(String.valueOf(FMLPaths.CONFIGDIR.get()),
            "flerovium.json"));

    public static void loadConfig() {
        Gson gson = new Gson();
        File configFile = new File(config_path.get());

        if (!configFile.exists()) {
            config = new Config();
            saveConfig();
        } else {
            // Load the existing config
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, Config.class);
                saveConfig();
            } catch (JsonIOException | JsonSyntaxException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(config_path.get())) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            Flerovium.LOGGER.error(e.getMessage());;
        }
    }

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
}