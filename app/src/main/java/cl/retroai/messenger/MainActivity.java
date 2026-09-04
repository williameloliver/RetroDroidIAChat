package cl.retroai.messenger;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    private static final int PICK_DOCUMENT = 77;

    private final List<Conversation> conversations = new ArrayList<Conversation>();
    private Conversation current;
    private ConversationStore store;
    private ArrayAdapter<Conversation> conversationAdapter;
    private ArrayAdapter<Message> messageAdapter;
    private ArrayAdapter<String> modelAdapter;

    private ListView listConversations, listMessages;
    private EditText editMessage;
    private TextView txtTokens, txtDocument;
    private Spinner spinnerModel, spinnerContext;
    private View sidebar;
    private String backendUrl;
    private String appKey;
    private boolean modelSelectionReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        store = new ConversationStore(this);
        conversations.addAll(store.load());

        bindViews();
        setupAdapters();
        setupActions();
        loadSettings();

        if (conversations.size() == 0) createConversation();
        else selectConversation(0);

        loadModelsAsync();

        if (backendUrl.length() == 0) showSettingsDialog();
    }

    private void bindViews() {
        listConversations = (ListView)findViewById(R.id.listConversations);
        listMessages = (ListView)findViewById(R.id.listMessages);
        editMessage = (EditText)findViewById(R.id.editMessage);
        txtTokens = (TextView)findViewById(R.id.txtTokens);
        txtDocument = (TextView)findViewById(R.id.txtDocument);
        spinnerModel = (Spinner)findViewById(R.id.spinnerModel);
        spinnerContext = (Spinner)findViewById(R.id.spinnerContext);
        sidebar = findViewById(R.id.sidebar);
    }

    private void setupAdapters() {
        conversationAdapter = new ArrayAdapter<Conversation>(this, android.R.layout.simple_list_item_1, conversations);
        listConversations.setAdapter(conversationAdapter);

        messageAdapter = new ArrayAdapter<Message>(this, android.R.layout.simple_list_item_1, new ArrayList<Message>());
        listMessages.setAdapter(messageAdapter);

        modelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);

        String[] modes = new String[]{"Completo", "Inteligente", "Ahorro", "Sólo pregunta"};
        ArrayAdapter<String> contextAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modes);
        contextAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContext.setAdapter(contextAdapter);
        spinnerContext.setSelection(1);
    }

    private void setupActions() {
        findViewById(R.id.btnNewChat).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { createConversation(); }
        });

        findViewById(R.id.btnSidebar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sidebar.setVisibility(sidebar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        listConversations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                selectConversation(pos);
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    sidebar.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sendMessage(); }
        });

        findViewById(R.id.btnAttach).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickDocument(); }
        });

        findViewById(R.id.btnExport).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { exportTxt(); }
        });

        findViewById(R.id.txtDocument).setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showSettingsDialog();
                return true;
            }
        });

        editMessage.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { updateTokens(); }
            public void afterTextChanged(Editable e) {}
        });

        spinnerContext.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { updateTokens(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (modelSelectionReady && current != null && pos >= 0 && pos < modelAdapter.getCount()) {
                    current.model = modelAdapter.getItem(pos);
                    store.save(conversations);
                }
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void loadSettings() {
        android.content.SharedPreferences p = getSharedPreferences("settings", MODE_PRIVATE);
        backendUrl = p.getString("backendUrl", "");
        appKey = p.getString("appKey", "");
    }

    private void saveSettings(String url, String key) {
        backendUrl = url.trim();
        appKey = key.trim();
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putString("backendUrl", backendUrl)
                .putString("appKey", appKey)
                .apply();
    }

    private void showSettingsDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(12 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);

        final EditText url = new EditText(this);
        url.setHint("https://tu-backend.onrender.com");
        url.setText(backendUrl);
        final EditText key = new EditText(this);
        key.setHint("Clave personal de la app");
        key.setText(appKey);
        box.addView(url);
        box.addView(key);

        new AlertDialog.Builder(this)
                .setTitle("Configuración RetroAI")
                .setView(box)
                .setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        saveSettings(url.getText().toString(), key.getText().toString());
                        loadModelsAsync();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void createConversation() {
        Conversation c = new Conversation();
        c.id = String.valueOf(System.currentTimeMillis());
        c.title = "Nueva conversación";
        c.model = modelAdapter.getCount() > 0 ? modelAdapter.getItem(0) : "openrouter/free";
        conversations.add(0, c);
        conversationAdapter.notifyDataSetChanged();
        store.save(conversations);
        selectConversation(0);
    }

    private void selectConversation(int pos) {
        if (pos < 0 || pos >= conversations.size()) return;
        current = conversations.get(pos);

        messageAdapter.clear();
        for (Message m : current.messages) messageAdapter.add(m);
        messageAdapter.notifyDataSetChanged();

        txtDocument.setText(current.documentName.length() == 0 ? "Sin documento (mantén pulsado aquí para ajustes)" : "📄 " + current.documentName);
        selectModel(current.model);
        updateTokens();
        if (messageAdapter.getCount() > 0) listMessages.setSelection(messageAdapter.getCount() - 1);
    }

    private void selectModel(String model) {
        for (int i=0;i<modelAdapter.getCount();i++) {
            if (modelAdapter.getItem(i).equals(model)) {
                spinnerModel.setSelection(i);
                return;
            }
        }
    }

    private void loadModelsAsync() {
        if (backendUrl == null || backendUrl.length() == 0) return;
        new AsyncTask<Void,Void,List<String>>() {
            Exception error;
            protected List<String> doInBackground(Void... v) {
                try { return new ApiClient(backendUrl, appKey).getModels(); }
                catch (Exception e) { error = e; return null; }
            }
            protected void onPostExecute(List<String> models) {
                if (models == null) {
                    toast("No pude cargar modelos: " + (error == null ? "error desconocido" : error.getClass().getSimpleName() + " - " + error.getMessage()));
                    return;
                }
                modelSelectionReady = false;
                modelAdapter.clear();
                // Para una tablet antigua, ponemos primero una entrada universal y luego limitamos el listado visual.
                if (!models.contains("openrouter/free")) modelAdapter.add("openrouter/free");
                int limit = Math.min(models.size(), 120);
                for (int i=0;i<limit;i++) {
                    String m = models.get(i);
                    if (!"openrouter/free".equals(m)) modelAdapter.add(m);
                }
                modelAdapter.notifyDataSetChanged();
                if (current != null) selectModel(current.model);
                modelSelectionReady = true;
            }
        }.execute();
    }

    private void sendMessage() {
        if (current == null) return;
        final String text = editMessage.getText().toString().trim();
        if (text.length() == 0) return;
        if (backendUrl.length() == 0) {
            showSettingsDialog();
            return;
        }

        if (current.messages.size() == 0) {
            current.title = text.length() > 34 ? text.substring(0,34) + "…" : text;
            conversationAdapter.notifyDataSetChanged();
        }

        current.messages.add(new Message("user", text));
        messageAdapter.add(current.messages.get(current.messages.size()-1));
        editMessage.setText("");
        store.save(conversations);
        updateTokens();

        findViewById(R.id.btnSend).setEnabled(false);

        final int mode = spinnerContext.getSelectedItemPosition();
        final List<Message> requestMessages = ContextManager.build(current, mode);
        final String selectedModel = spinnerModel.getSelectedItem() == null ? current.model : spinnerModel.getSelectedItem().toString();
        current.model = selectedModel;

        new AsyncTask<Void,Void,JSONObject>() {
            Exception error;
            protected JSONObject doInBackground(Void... v) {
                try {
                    return new ApiClient(backendUrl, appKey).chat(selectedModel, requestMessages, 900);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                findViewById(R.id.btnSend).setEnabled(true);
                if (result == null) {
                    Message m = new Message("assistant", "⚠ Error de conexión: " + (error == null ? "desconocido" : error.getMessage()));
                    current.messages.add(m);
                    messageAdapter.add(m);
                    store.save(conversations);
                    return;
                }

                String reply = result.optString("text", "(respuesta vacía)");
                current.promptTokens = result.optInt("prompt_tokens", current.promptTokens);
                current.completionTokens = result.optInt("completion_tokens", current.completionTokens);
                Message m = new Message("assistant", reply);
                current.messages.add(m);
                messageAdapter.add(m);
                store.save(conversations);
                updateTokens();
                listMessages.setSelection(messageAdapter.getCount()-1);
            }
        }.execute();
    }

    private void updateTokens() {
        if (current == null) return;
        int mode = spinnerContext.getSelectedItemPosition();
        List<Message> temp = ContextManager.build(current, mode);
        String pending = editMessage == null ? "" : editMessage.getText().toString();
        int estimate = TokenEstimator.estimateMessages(temp) + TokenEstimator.estimateText(pending);
        txtTokens.setText("Contexto aprox.: " + estimate + " tokens  | Última API: "
                + current.promptTokens + " entrada + " + current.completionTokens + " salida");
    }

    private void pickDocument() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Abrir TXT o PDF"), PICK_DOCUMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_DOCUMENT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final String name = guessName(uri);
        final boolean pdf = name.toLowerCase(Locale.US).endsWith(".pdf");

        new AsyncTask<Void,Void,String>() {
            Exception error;
            protected String doInBackground(Void... v) {
                try {
                    byte[] bytes = readBytes(getContentResolver().openInputStream(uri), 8 * 1024 * 1024);
                    if (pdf) return new ApiClient(backendUrl, appKey).uploadPdf(name, bytes);
                    return new String(bytes, "UTF-8");
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }
            protected void onPostExecute(String text) {
                if (text == null) {
                    toast("No pude abrir el documento: " + (error == null ? "" : error.getMessage()));
                    return;
                }
                if (text.length() > 100000) text = text.substring(0,100000);
                current.documentName = name;
                current.documentText = text;
                txtDocument.setText("📄 " + name);
                store.save(conversations);
                updateTokens();
                toast("Documento cargado: " + name);
            }
        }.execute();
    }

    private byte[] readBytes(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n, total = 0;
        while ((n = in.read(b)) >= 0) {
            total += n;
            if (total > max) throw new IOException("Archivo demasiado grande (máx. 8 MB en MVP)");
            out.write(b,0,n);
        }
        in.close();
        return out.toByteArray();
    }

    private String guessName(Uri uri) {
        String s = uri.getLastPathSegment();
        if (s == null || s.length() == 0) s = "documento.txt";
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash+1);
        return s;
    }

    private void exportTxt() {
        if (current == null) return;
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "RetroAI/Exports");
            if (!dir.exists()) dir.mkdirs();
            String safe = current.title.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]", "_");
            File f = new File(dir, safe + "_" + System.currentTimeMillis() + ".txt");
            StringBuilder sb = new StringBuilder();
            sb.append("RETRO AI MESSENGER\n");
            sb.append("Conversación: ").append(current.title).append("\n");
            sb.append("Modelo: ").append(current.model).append("\n\n");
            for (Message m : current.messages) {
                sb.append("user".equals(m.role) ? "TÚ:\n" : "RETROAI:\n");
                sb.append(m.content).append("\n\n---------------------------------\n\n");
            }
            FileOutputStream out = new FileOutputStream(f);
            out.write(sb.toString().getBytes("UTF-8"));
            out.close();
            toast("Guardado en: " + f.getAbsolutePath());
        } catch (Exception e) {
            toast("No pude exportar: " + e.getMessage());
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
