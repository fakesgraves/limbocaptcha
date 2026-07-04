package ru.limbocaptcha;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    private final Path configFile;
    private ConfigurationNode rootNode;

    public Config(Path dataDirectory) throws IOException {
        this.configFile = dataDirectory.resolve("config.yml");
        
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
        
        if (!Files.exists(configFile)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                }
            }
        }
        
        loadConfig();
    }

    private void loadConfig() throws IOException {
        YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
            .setFile(configFile.toFile())
            .build();
        rootNode = loader.load();
    }

    public String getString(String path) {
        return rootNode.getNode((Object[]) path.split("\\.")).getString();
    }

    public String getString(String path, String def) {
        return rootNode.getNode((Object[]) path.split("\\.")).getString(def);
    }

    public int getInt(String path) {
        return rootNode.getNode((Object[]) path.split("\\.")).getInt();
    }

    public int getInt(String path, int def) {
        return rootNode.getNode((Object[]) path.split("\\.")).getInt(def);
    }

    public String getCaptchaSiteKey() {
        return getString("captcha.site-key");
    }

    public String getCaptchaSecretKey() {
        return getString("captcha.secret-key");
    }

    public String getCaptchaVerifyUrl() {
        return getString("captcha.verify-url");
    }

    public String getCaptchaDomain() {
        return getString("captcha.domain");
    }

    public String getLimboWorldName() {
        return getString("limbo.world-name");
    }

    public String getMessage(String path) {
        return getString("messages." + path);
    }
}
