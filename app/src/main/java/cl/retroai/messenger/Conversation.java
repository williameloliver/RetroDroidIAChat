package cl.retroai.messenger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Conversation {
    public String id;
    public String title;
    public String model;
    public String documentName = "";
    public String documentText = "";
    public int promptTokens = 0;
    public int completionTokens = 0;
    public final List<Message> messages = new ArrayList<Message>();

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("model", model);
        o.put("documentName", documentName);
        o.put("documentText", documentText);
        o.put("promptTokens", promptTokens);
        o.put("completionTokens", completionTokens);
        JSONArray a = new JSONArray();
        for (Message m : messages) a.put(m.toJson());
        o.put("messages", a);
        return o;
    }

    public static Conversation fromJson(JSONObject o) {
        Conversation c = new Conversation();
        c.id = o.optString("id", String.valueOf(System.currentTimeMillis()));
        c.title = o.optString("title", "Conversación");
        c.model = o.optString("model", "openrouter/free");
        c.documentName = o.optString("documentName", "");
        c.documentText = o.optString("documentText", "");
        c.promptTokens = o.optInt("promptTokens", 0);
        c.completionTokens = o.optInt("completionTokens", 0);
        JSONArray a = o.optJSONArray("messages");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject m = a.optJSONObject(i);
                if (m != null) c.messages.add(Message.fromJson(m));
            }
        }
        return c;
    }

    @Override
    public String toString() {
        return title;
    }
}
