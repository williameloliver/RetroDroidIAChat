package cl.retroai.messenger;

import java.util.ArrayList;
import java.util.List;

public class ContextManager {
    public static final int FULL = 0;
    public static final int SMART = 1;
    public static final int SAVE = 2;
    public static final int QUESTION_ONLY = 3;

    public static List<Message> build(Conversation c, int mode) {
        ArrayList<Message> out = new ArrayList<Message>();

        String system = "Eres RetroAI, un asistente de estudio claro, preciso y útil. "
                + "Responde en el idioma del usuario. Si hay un documento adjunto, úsalo como fuente principal.";
        out.add(new Message("system", system));

        if (c.documentText != null && c.documentText.length() > 0) {
            String doc = c.documentText;
            int maxChars = mode == SAVE ? 12000 : 30000;
            if (mode == QUESTION_ONLY) maxChars = 8000;
            if (doc.length() > maxChars) doc = doc.substring(0, maxChars);
            out.add(new Message("system", "DOCUMENTO: " + c.documentName + "\n\n" + doc));
        }

        if (c.messages.size() == 0) return out;

        if (mode == QUESTION_ONLY) {
            out.add(c.messages.get(c.messages.size() - 1));
            return out;
        }

        int keep;
        if (mode == FULL) keep = c.messages.size();
        else if (mode == SMART) keep = Math.min(10, c.messages.size());
        else keep = Math.min(4, c.messages.size());

        int start = Math.max(0, c.messages.size() - keep);
        for (int i = start; i < c.messages.size(); i++) out.add(c.messages.get(i));
        return out;
    }
}
