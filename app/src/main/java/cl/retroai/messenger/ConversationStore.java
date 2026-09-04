package cl.retroai.messenger;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationStore {
    private final File file;

    public ConversationStore(Context context) {
        file = new File(context.getFilesDir(), "conversations.json");
    }

    public List<Conversation> load() {
        ArrayList<Conversation> out = new ArrayList<Conversation>();
        if (!file.exists()) return out;
        try {
            String json = readAll(new FileInputStream(file));
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) out.add(Conversation.fromJson(o));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public void save(List<Conversation> conversations) {
        try {
            JSONArray a = new JSONArray();
            for (Conversation c : conversations) a.put(c.toJson());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(a.toString(2).getBytes("UTF-8"));
            fos.close();
        } catch (Exception ignored) {}
    }

    public static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        in.close();
        return new String(out.toByteArray(), "UTF-8");
    }
}
