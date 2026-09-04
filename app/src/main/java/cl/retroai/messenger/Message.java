package cl.retroai.messenger;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    public String role;
    public String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("role", role);
        o.put("content", content);
        return o;
    }

    public static Message fromJson(JSONObject o) {
        return new Message(o.optString("role", "user"), o.optString("content", ""));
    }

    @Override
    public String toString() {
        return ("assistant".equals(role) ? "RetroAI" : "Tú") + ":\n" + content;
    }
}
