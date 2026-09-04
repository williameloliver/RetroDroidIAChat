# RetroAI Messenger 0.1.1

Cliente de IA pensado para Android 4.2 / API 17 y tablets antiguas.

## Qué incluye este MVP

- Java nativo, sin AndroidX ni Compose.
- `minSdk 17` (Android 4.2).
- Portrait y landscape.
- Panel lateral de conversaciones.
- Persistencia local en JSON.
- Exportación de cada conversación a TXT.
- Apertura de TXT.
- Apertura de PDF mediante backend.
- Selector de modelos de OpenRouter.
- Conteo aproximado de tokens antes de enviar.
- Conteo real de tokens devuelto por OpenRouter después de responder.
- 4 modos de contexto: Completo, Inteligente, Ahorro y Sólo pregunta.
- TLS 1.2 habilitado explícitamente para Android 4.2–4.4.
- Backend separado para no guardar la API key de OpenRouter en el APK.

## 1. Crear tu API key de OpenRouter

Crea una API key en OpenRouter. NO la pongas en el código Android.

## 2. Desplegar el backend

La carpeta `/backend` está lista para un servicio Node 20.

### Render

1. Sube este proyecto a GitHub.
2. En Render crea un Web Service desde el repositorio.
3. Root directory: `backend`
4. Build command: `npm install`
5. Start command: `npm start`
6. Variables secretas:
   - `OPENROUTER_API_KEY`: tu clave de OpenRouter.
   - `APP_SHARED_KEY`: inventa una contraseña larga, por ejemplo 32+ caracteres.
   - `APP_URL`: URL pública del backend.
7. Despliega.
8. Abre `/health` en el navegador. Debe devolver `{"ok":true,...}`.

También se incluye `backend/render.yaml`.

## 3. Configurar la app

Al primer inicio la aplicación muestra un cuadro de configuración.

- Backend URL: `https://TU-SERVICIO.onrender.com`
- Clave personal: el mismo valor de `APP_SHARED_KEY`

Puedes volver a abrir los ajustes manteniendo pulsado el texto `Sin documento`.

## 4. Compilar el APK sin Android Studio

### Opción recomendada: GitHub Actions

El repositorio incluye:

`.github/workflows/android-debug.yml`

Cada push a `main` compila automáticamente.

En GitHub:

1. Abre `Actions`.
2. Selecciona `Build Android APK`.
3. Ejecuta `Run workflow` o simplemente haz un push.
4. Al terminar, descarga el artifact `RetroAI-Messenger-debug`.
5. Dentro viene `app-debug.apk`.
6. Copia el APK a la Tab 3 Lite e instálalo.

No necesitas Android Studio local.

### Codemagic

También hay un `codemagic.yaml`.

1. Conecta el repositorio en Codemagic.
2. Selecciona la configuración YAML.
3. Ejecuta `retroai-android-debug`.
4. Descarga el APK de Artifacts.

## 5. Dónde guarda las cosas

Conversaciones:
- almacenamiento privado de la app
- archivo `conversations.json`

Exportaciones:
- `/storage/emulated/0/RetroAI/Exports/`

## 6. Modos de contexto

**Completo**
Manda todos los mensajes. Mayor continuidad y mayor gasto.

**Inteligente**
Manda los últimos 10 mensajes. Recomendado por defecto.

**Ahorro**
Manda los últimos 4 mensajes.

**Sólo pregunta**
Manda únicamente la pregunta actual, más instrucciones y una porción limitada del documento.

El contador de la app es una estimación previa. El servidor devuelve después los valores reales informados por OpenRouter.

## Limitaciones deliberadas de 0.1

- PDF escaneado sin texto: no hay OCR todavía.
- Los documentos se recortan para proteger memoria y contexto.
- El chat no usa streaming todavía.
- El selector muestra como máximo 120 modelos para no castigar la Tab 3 Lite.
- La interfaz es inspirada en Messenger, no una copia de recursos originales.
- El resumen semántico automático de conversaciones largas queda para 0.2; en 0.1 el modo inteligente hace recorte de historial.

## Próxima versión sugerida

0.2:
- mensajes con burbujas personalizadas;
- Markdown básico y bloques de código;
- buscador de conversaciones;
- favoritos de modelos;
- resúmenes persistentes;
- perfiles Profesor / Examen / Programación / Resumen;
- streaming opcional;
- búsqueda por fragmentos del documento para enviar sólo lo relevante.


## Cambio 0.1.1 — HTTPS para Android 4.2

- Conscrypt 2.5.2 embebido para conexiones HTTPS en Android < 5.0.
- APK limitado a armeabi-v7a para la Tab 3 Lite.
- Mensajes de error de red más descriptivos.
- versionCode 2 / versionName 0.1.1.
