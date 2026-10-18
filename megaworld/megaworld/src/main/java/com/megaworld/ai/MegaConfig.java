package com.megaworld.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки мода: .minecraft/config/megaworld.json.
 * ИИ-ответы жителей ВЫКЛЮЧЕНЫ по умолчанию. Если включить (aiEnabled = true),
 * текст твоих сообщений в чате рядом с жителем отправляется на адрес aiUrl
 * (по умолчанию локальный сервер Ollama на твоём компьютере).
 */
public final class MegaConfig {
    public boolean aiEnabled = false;
    public String aiUrl = "http://localhost:11434/api/generate";
    public String aiModel = "llama3";
    public int aiTimeoutSeconds = 20;

    private static MegaConfig instance;

    public static synchronized MegaConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static MegaConfig load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("megaworld.json");
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    MegaConfig loaded = gson.fromJson(reader, MegaConfig.class);
                    if (loaded != null) {
                        return loaded;
                    }
                }
            }
            MegaConfig defaults = new MegaConfig();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(defaults), StandardCharsets.UTF_8);
            return defaults;
        } catch (Exception e) {
            return new MegaConfig();
        }
    }
}
