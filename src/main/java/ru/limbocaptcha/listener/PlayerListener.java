package ru.limbocaptcha.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.limbocaptcha.CaptchaManager;
import ru.limbocaptcha.LimboCaptcha;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerListener {

    private final LimboCaptcha plugin;
    private final LegacyComponentSerializer serializer;
    private final Set<UUID> authenticatedPlayers;
    private final Set<UUID> captchaPassed;

    public PlayerListener(LimboCaptcha plugin) {
        this.plugin = plugin;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
        this.authenticatedPlayers = new HashSet<>();
        this.captchaPassed = new HashSet<>();
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check if player is already authenticated
        if (authenticatedPlayers.contains(playerId)) {
            return;
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("limbocaptcha.bypass")) {
            authenticatedPlayers.add(playerId);
            captchaPassed.add(playerId);
            return;
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Skip if already authenticated or bypassed
        if (captchaPassed.contains(playerId)) {
            return;
        }
        
        try {
            // Generate captcha link
            String captchaLink = plugin.getCaptchaManager().generateCaptchaLink(player);
            
            // Send player to limbo world
            LimboFactory factory = plugin.getLimboFactory();
            Limbo limbo = factory.createLimbo(factory.createVirtualWorld(
                plugin.getConfig().getLimboWorldName(),
                0, 100, 0,
                0.0f, 0.0f
            ));
            
            // Configure limbo player
            LimboPlayer limboPlayer = factory.createLimboPlayer(player, limbo)
                .setGameMode(GameMode.ADVENTURE)
                .setCanFly(false)
                .setFlyingSpeed(0.0f)
                .setWalkSpeed(0.0f);
            
            // Send messages to player
            player.sendMessage(serializer.deserialize(plugin.getConfig().getMessage("captcha-chat")));
            
            Component linkMessage = serializer.deserialize(
                plugin.getConfig().getMessage("captcha-link")
                    .replace("%link%", captchaLink)
            ).clickEvent(ClickEvent.openUrl(captchaLink));
            
            player.sendMessage(linkMessage);
            
            // Send title
            player.showTitle(net.kyori.adventure.title.Title.title(
                serializer.deserialize(plugin.getConfig().getMessage("captcha-title")),
                serializer.deserialize(plugin.getConfig().getMessage("captcha-subtitle"))
            ));
            
        } catch (Exception e) {
            plugin.getLogger().error("Failed to send player to captcha limbo", e);
            player.disconnect(Component.text("An error occurred during captcha verification"));
        }
    }

    // This method should be called when captcha is verified
    public void onCaptchaVerified(Player player) {
        UUID playerId = player.getUniqueId();
        captchaPassed.add(playerId);
        
        player.sendMessage(serializer.deserialize(
            plugin.getConfig().getMessage("captcha-success")
        ));
        
        // Update session
        CaptchaManager.CaptchaSession session = plugin.getCaptchaManager().getSession(playerId);
        if (session != null) {
            session.setCompleted(true);
        }
        
        // Player can now proceed to LimboAuth
        // The auth server should be configured to handle this player
        player.sendMessage(serializer.deserialize(
            "&aYou can now proceed to authorization!"
        ));
    }

    public void onCaptchaFailed(Player player) {
        player.disconnect(serializer.deserialize(
            plugin.getConfig().getMessage("kick-message")
        ));
    }

    public boolean hasPassedCaptcha(UUID playerId) {
        return captchaPassed.contains(playerId);
    }
}
