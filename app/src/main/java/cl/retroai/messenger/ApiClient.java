package cl.retroai.messenger;

import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.security.Provider;
import org.conscrypt.Conscrypt;

public class ApiClient {
    private final String baseUrl;
    private final String appKey;

    public ApiClient(String baseUrl, String appKey) {
        this.baseUrl = trimSlash(baseUrl);
        this.appKey = appKey;
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        while (s.endsWith("/")) s = s.substring(0, s.length()-1);
        return s;
    }

    private HttpURLConnection open(String path, String method) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(90000);
        c.setRequestProperty("X-RetroAI-Key", appKey == null ? "" : appKey);

        if (c instanceof HttpsURLConnection) {
            HttpsURLConnection https = (HttpsURLConnection)c;
            if (Build.VERSION.SDK_INT < 21) {
                Provider provider = Conscrypt.newProvider();
                SSLContext context = SSLContext.getInstance("TLS", provider);
                context.init(null, null, null);
                https.setSSLSocketFactory(context.getSocketFactory());
            }
        }
        return c;
    }

    private String readResponse(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String body = ConversationStore.readAll(in);
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + ": " + body);
        return body;
    }

    public List<String> getModels() throws Exception {
        HttpURLConnection c = open("/models", "GET");
        JSONObject root = new JSONObject(readResponse(c));
        JSONArray a = root.optJSONArray("data");
        ArrayList<String> out = new ArrayList<String>();
        if (a != null) {
            for (int i=0;i<a.length();i++) {
                JSONObject m = a.optJSONObject(i);
                if (m != null) {
                    String id = m.optString("id", "");
                    if (id.length() > 0) out.add(id);
                }
            }
        }
        if (out.size() == 0) out.add("openrouter/free");
        return out;
    }

    public JSONObject chat(String model, List<Message> messages, int maxTokens) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        JSONArray a = new JSONArray();
        for (Message m : messages) a.put(m.toJson());
        body.put("messages", a);

        HttpURLConnection c = open("/chat", "POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        OutputStream out = c.getOutputStream();
        out.write(body.toString().getBytes("UTF-8"));
        out.close();
        return new JSONObject(readResponse(c));
    }

    public String uploadPdf(String fileName, byte[] data) throws Exception {
        String boundary = "----RetroAI" + System.currentTimeMillis();
        HttpURLConnection c = open("/extract-pdf", "POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        DataOutputStream out = new DataOutputStream(c.getOutputStream());
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName.replace("\"","") + "\"\r\n");
        out.writeBytes("Content-Type: application/pdf\r\n\r\n");
        out.write(data);
        out.writeBytes("\r\n--" + boundary + "--\r\n");
        out.flush();
        out.close();

        JSONObject o = new JSONObject(readResponse(c));
        return o.optString("text", "");
    }
}
