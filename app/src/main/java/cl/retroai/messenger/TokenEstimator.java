package cl.retroai.messenger;

import java.util.List;

public class TokenEstimator {
    // Estimación deliberadamente conservadora para varios tokenizadores.
    public static int estimateText(String text) {
        if (text == null || text.length() == 0) return 0;
        return Math.max(1, (int)Math.ceil(text.length() / 3.6));
    }

    public static int estimateMessages(List<Message> messages) {
        int total = 0;
        for (Message m : messages) total += 4 + estimateText(m.content);
        return total;
    }
}
