package ru.limbocaptcha;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.elytrium.limboapi.api.LimboFactory;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "limbocaptcha",
    name = "LimboCaptcha",
    version = "1.0.0",
    description = "Captcha verification before LimboAuth authorization",
    authors = {"YourName"},
    dependencies = {
        @Plugin.Dependency(id = "limboapi"),
        @Plugin.Dependency(id = "limboauth")
    }
)
public class LimboCaptcha {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private Config config;
    private CaptchaManager captchaManager;
    private LimboFactory limboFactory;

    @Inject
    public LimboCaptcha(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            this.config = new Config(dataDirectory);
            this.captchaManager = new CaptchaManager(config);
            
            // Get LimboFactory from LimboAPI
            this.limboFactory = (LimboFactory) server.getPluginManager()
                .getPlugin("limboapi")
                .orElseThrow(() -> new RuntimeException("LimboAPI not found"))
                .getInstance()
                .orElseThrow(() -> new RuntimeException("LimboAPI instance not found"));

            // Register listener
            server.getEventManager().register(this, new listener.PlayerListener(this));

            logger.info("LimboCaptcha plugin enabled!");
        } catch (Exception e) {
            logger.error("Failed to initialize LimboCaptcha", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (captchaManager != null) {
            captchaManager.shutdown();
        }
        logger.info("LimboCaptcha plugin disabled!");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Config getConfig() {
        return config;
    }

    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    public LimboFactory getLimboFactory() {
        return limboFactory;
    }
}
