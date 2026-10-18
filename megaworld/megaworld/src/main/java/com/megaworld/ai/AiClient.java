package com.megaworld.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megaworld.entity.Emotion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Необязательная связь с локальной ИИ-моделью (формат Ollama: POST /api/generate).
 * Работает только если aiEnabled = true в config/megaworld.json.
 * Вызов идёт в отдельном потоке и не тормозит игру.
 */
public final class AiClient {
    private static final Pattern TAG = Pattern.compile("^\\s*\\[(JOY|ANGER|SADNESS|FEAR|NEUTRAL)]\\s*(.*)$",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private AiClient() {}

    /** Вернёт null, если ИИ недоступен или ответил непонятно. */
    public static CompletableFuture<GroxBrain.Reply> ask(String message, Emotion current) {
        MegaConfig cfg = MegaConfig.get();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call(cfg, message, current);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static GroxBrain.Reply call(MegaConfig cfg, String message, Emotion current) throws Exception {
        URL url = new URL(cfg.aiUrl);
        String protocol = url.getProtocol();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            return null;
        }
        String prompt = "Ты — добрый маленький житель Грокса из Minecraft. Твоя текущая эмоция: " + current.name()
                + ". Ответь игроку коротко (одно-два предложения) по-русски. "
                + "Начни ответ с тега эмоции в квадратных скобках: [JOY], [ANGER], [SADNESS], [FEAR] или [NEUTRAL]. "
                + "Игрок говорит: " + message;

        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.aiModel);
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(5000);
        con.setReadTimeout(Math.max(1, cfg.aiTimeoutSeconds) * 1000);
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (con.getResponseCode() != 200) {
            return null;
        }
        String response;
        try (InputStream is = con.getInputStream()) {
            response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String text = JsonParser.parseString(response).getAsJsonObject().get("response").getAsString();

        Emotion emotion = Emotion.NEUTRAL;
        Matcher m = TAG.matcher(text);
        if (m.matches()) {
            Emotion parsed = Emotion.byName(m.group(1));
            if (parsed != null) {
                emotion = parsed;
            }
            text = m.group(2);
        }
        text = text.replaceAll("\\p{Cntrl}", " ").trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > 200) {
            text = text.substring(0, 200);
        }
        return new GroxBrain.Reply(text, emotion, GroxBrain.Action.NONE);
    }
}
