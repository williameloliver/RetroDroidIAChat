const express = require("express");
const multer = require("multer");
const pdf = require("pdf-parse");

const app = express();
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 8 * 1024 * 1024 }
});

app.use(express.json({ limit: "2mb" }));

function auth(req, res, next) {
  const expected = process.env.APP_SHARED_KEY || "";
  if (!expected) return res.status(500).json({ error: "APP_SHARED_KEY no configurada" });
  if ((req.header("X-RetroAI-Key") || "") !== expected)
    return res.status(401).json({ error: "Clave de app inválida" });
  next();
}

app.get("/health", (req, res) => res.json({ ok: true, app: "RetroAI Backend" }));

app.get("/models", auth, async (req, res) => {
  try {
    const r = await fetch("https://openrouter.ai/api/v1/models", {
      headers: {
        "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`
      }
    });
    const body = await r.text();
    res.status(r.status).type("application/json").send(body);
  } catch (e) {
    res.status(500).json({ error: String(e) });
  }
});

app.post("/chat", auth, async (req, res) => {
  try {
    const { model, messages, max_tokens } = req.body;
    if (!model || !Array.isArray(messages))
      return res.status(400).json({ error: "model y messages son obligatorios" });

    const payload = {
      model,
      messages,
      max_tokens: Math.max(64, Math.min(Number(max_tokens || 900), 4096)),
      temperature: 0.5,
      stream: false
    };

    const r = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
        "Content-Type": "application/json",
        "HTTP-Referer": process.env.APP_URL || "https://localhost",
        "X-OpenRouter-Title": "RetroAI Messenger"
      },
      body: JSON.stringify(payload)
    });

    const data = await r.json();
    if (!r.ok) return res.status(r.status).json(data);

    const text = data?.choices?.[0]?.message?.content || "";
    const usage = data?.usage || {};

    res.json({
      text,
      model: data.model || model,
      prompt_tokens: usage.prompt_tokens || 0,
      completion_tokens: usage.completion_tokens || 0,
      total_tokens: usage.total_tokens || 0
    });
  } catch (e) {
    res.status(500).json({ error: String(e) });
  }
});

app.post("/extract-pdf", auth, upload.single("file"), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: "Falta PDF" });
    const data = await pdf(req.file.buffer);
    const text = (data.text || "").trim();
    res.json({
      name: req.file.originalname,
      pages: data.numpages || 0,
      text: text.slice(0, 150000)
    });
  } catch (e) {
    res.status(400).json({ error: "No se pudo extraer el PDF", detail: String(e) });
  }
});

const port = process.env.PORT || 3000;
app.listen(port, "0.0.0.0", () => console.log(`RetroAI backend en puerto ${port}`));
