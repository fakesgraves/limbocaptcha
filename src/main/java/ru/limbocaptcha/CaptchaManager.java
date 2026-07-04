package ru.limbocaptcha;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class CaptchaManager {

    private final Config config;
    private final Map<UUID, CaptchaSession> sessions;
    private final ScheduledExecutorService scheduler;
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public CaptchaManager(Config config) {
        this.config = config;
        this.sessions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public String generateCaptchaLink(Player player) {
        UUID playerId = player.getUniqueId();
        String token = UUID.randomUUID().toString().substring(0, 8);
        
        // Store session
        CaptchaSession session = new CaptchaSession(player, token);
        sessions.put(playerId, session);
        
        // Schedule timeout
        scheduler.schedule(() -> {
            CaptchaSession s = sessions.get(playerId);
            if (s != null && !s.isCompleted()) {
                player.disconnect(Component.text(
                    serializer.serialize(
                        serializer.deserialize(config.getMessage("captcha-timeout"))
                    )
                ));
                sessions.remove(playerId);
            }
        }, 5, TimeUnit.MINUTES);
        
        return config.getCaptchaDomain() + "?token=" + token + "&player=" + playerId.toString();
    }

    public boolean verifyCaptcha(String token, String recaptchaResponse) {
        try {
            String url = config.getCaptchaVerifyUrl();
            String secret = config.getCaptchaSecretKey();
            
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "LimboCaptcha/1.0");
            
            String postParams = "secret=" + secret + "&response=" + recaptchaResponse;
            
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();
            
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            // Parse JSON response
            com.google.gson.JsonObject jsonResponse = 
                new com.google.gson.Gson().fromJson(response.toString(), com.google.gson.JsonObject.class);
            
            return jsonResponse.get("success").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public CaptchaSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public void removeSession(UUID playerId) {
        sessions.remove(playerId);
    }

    public void shutdown() {
        scheduler.shutdown();
        sessions.clear();
    }

    public static class CaptchaSession {
        private final Player player;
        private final String token;
        private boolean completed;
        
        public CaptchaSession(Player player, String token) {
            this.player = player;
            this.token = token;
            this.completed = false;
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public String getToken() {
            return token;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }
}
