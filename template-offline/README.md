# 🎮 Game Show Center - Standalone Offline Engine (v1.0.0)

> **Release Oficial 1.0.0** — Motor ejecutable nativo 100% offline desarrollado por **Yuyi Studios**.  
> Plataforma interactiva de concursos televisivos para educación, auditorios, salones de clase y eventos en vivo.

---

## 🌟 Características Principales

- **Ejecución 100% Offline**: Cero latencia, sin dependencia de internet, sin APIs externas obligatorias y con máxima privacidad para escuelas y empresas.
- **Ejecutable Nativo para Windows (`GameShowCenter.exe`)**: Empaquetado con Java runtime integrado vía `jpackage`. Listo para doble clic sin requerir instalación previa de Java en la máquina host.
- **Inteligencia Artificial Local con RAG (PDFs)**: Inferencia local con modelos GGUF cuantizados. Permite cargar hasta 10 libros de texto o manuales en PDF para generar preguntas y dinámicas educativas contextualizadas.
- **Arquitectura de Minijuegos Modulares**: Soporte para carga dinámica en caliente de plugins de minijuegos mediante `GamePluginClassLoader`.
- **Modo Battle Royale por Juego (`[⚔️ BR]`)**:
  - Configurable de manera individual e independiente dentro del setup de cada minijuego.
  - Elimina la restricción estricta de `# de rondas por jugador`: el objetivo de la partida es que **todos los elementos del banco aparezcan en la arena** hasta agotarse.
  - Validación optimizada: requiere al menos 1 elemento ($\ge 1$) y valida rigurosamente que todos los archivos multimedia (imágenes y audios) existan en disco y funcionen.
- **Soporte Multilingüe en Caliente (🌐)**: Español, Inglés, Francés y Portugués seleccionables al instante sin reiniciar.
- **Accesibilidad y Escalado de Fuentes**: Control granular de tamaño de fuente (+25%, -20%, etc.) con ajuste automático de saltos de línea para pantallas gigantes y proyectores.
- **Temas y Fondos Persistentes**: Paletas cromáticas de alto contraste y soporte de imágenes de fondo personalizadas sin pantallas blancas accidentales durante transiciones.

---

## 📁 Estructura del Proyecto

```text
template-offline/
├── assets/                    # Iconos, logotipos, efectos de sonido y recursos visuales
├── games/                     # Minijuegos dinámicos y manifiestos JSON
│   ├── Geo Location/          # Adivina la ubicación geográfica
│   ├── Guess Character/       # Pistas visuales para adivinar personajes
│   ├── Hangman/               # Clásico ahorcado con banco de palabras
│   ├── Rapid Rhythm/          # Desafío musical y reconocimiento auditivo
│   ├── Snap Solve/            # Identificación de imágenes con filtros dinámicos
│   ├── TimeLine/              # Ordenamiento cronológico de hitos históricos
│   ├── Topic Takedown/        # Panel tipo Jeopardy con categorías y puntajes
│   ├── Trivia Quiz/           # Cuestionario clásico de opción múltiple
│   └── Zero Margin/           # Cronómetro de precisión a ciegas
├── release/                   # Artefactos compilados finales
│   ├── GameShowCenter/        # Carpeta portable con GameShowCenter.exe y runtime
│   └── GameShowCenter-Offline-v1.0.zip
├── scripts/                   # Worker Python para IA local GGUF y RAG
├── src/                       # Código fuente JavaFX (Motor, Vistas, Modelos)
├── build-exe.bat              # Script lanzador en batch para Windows
├── build-exe.ps1              # Script de compilación automatizada y jpackage
├── installer-setup.iss        # Script de Inno Setup para instalador estándar
└── pom.xml                    # Configuración Maven (Java 17/21, JavaFX, Jackson, Shade)
```

---

## 🚀 Compilación y Generación del Ejecutable

### Requisitos de Desarrollo
- **JDK 17 o JDK 21+** (Oracle JDK o Eclipse Temurin)
- **Maven 3.9+** (o el wrapper incluido `mvnw.cmd`)
- **Windows 10 / 11 (64-bit)** para empaquetado nativo `.exe`

### 1. Ejecutar en Modo Desarrollo (Hot Reload)
```powershell
cd template-offline
.\mvnw.cmd javafx:run
```
*Clave Maestra de Desarrollador para Pruebas Pro: `YUYI-STUDIO-PRO-2026`*

### 2. Generar el JAR Standalone
```powershell
.\mvnw.cmd package -DskipTests
```
Genera el archivo `target/template-offline-1.0.0-standalone.jar` con todas las dependencias sombreadas (uber-jar).

### 3. Generar el Ejecutable Nativo `.EXE` y Paquete ZIP
Ejecuta el script automatizado:
```powershell
.\build-exe.ps1 -AppVersion "1.0.0" -ReleaseTag "v1.0"
```
O simplemente haz doble clic sobre:
```text
build-exe.bat
```
El script generará automáticamente:
1. `template-offline/release/GameShowCenter/GameShowCenter.exe`
2. `template-offline/release/GameShowCenter-Offline-v1.0.zip`

### 4. Generar Instalador con Inno Setup (Opcional)
Compila `installer-setup.iss` con Inno Setup Compiler para crear `GameShowCenter-Offline-Setup.exe`.

---

## 🛠️ Contrato de Desarrollo para Nuevos Minijuegos

Para agregar un nuevo minijuego al motor, crea una subcarpeta en `games/<Nombre>/`:

1. **`description.json`**:
```json
{
  "name": "Nombre Del Minijuego",
  "author": "Tu Nombre o Estudio",
  "version": "1.0.0",
  "stageClass": "com.tuorganizacion.MiJuegoStage",
  "editorClass": "com.tuorganizacion.MiJuegoSetupEditor",
  "available": ["1vs1", "team", "FREE_FOR_ALL"],
  "translations": { ... }
}
```

2. **Interfaz del Editor (`IGameSetupEditor`)**:
```java
public interface IGameSetupEditor {
    Node createEditorPanel(JsonNode currentSetup, List<Competitor> profiles, Palette palette);
    default String validateSetupData(JsonNode setupData, List<Competitor> profiles, boolean battleRoyale) { ... }
    JsonNode getUpdatedSetup();
}
```

---

## 📜 Licencia y Créditos
- Desarrollado por **Yuyi Studios** © 2026.
- Todos los derechos reservados.
