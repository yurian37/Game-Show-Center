# 📋 Guía de Despliegue en Google Apps Script (Google Forms)
## Estudio de Mercado Oficial — Game Show Center
**YonIA Enterprise © 2026**

Esta carpeta contiene los scripts programados en **Google Apps Script (GAS)** para generar de manera 100% automatizada las **tres encuestas oficiales de validación de mercado** en **Google Forms**.

---

## 📁 Archivos Disponibles en `forms/`

1. **[`Encuesta_1_Profesores.gs`](./Encuesta_1_Profesores.gs)**:
   - Dirigida a: Maestros y profesores de Primaria, Secundaria y Universidad.
   - Temas: Necesidad de material didáctico, horas invertidas en preparar repasos, evaluación de los 10 minijuegos de Game Show Center (gusto/disgusto), utilidad de la IA local con PDFs (RAG) y operación 100% offline.
2. **[`Encuesta_2_Estudiantes.gs`](./Encuesta_2_Estudiantes.gs)**:
   - Dirigida a: Alumnos de Primaria y Secundaria.
   - Temas: Nivel de aburrimiento/diversión en clases ordinarias, aprendizaje jugando, deseo de concursos tipo show de televisión con podio y marcadores, trabajo en equipo vs individual, e interacción con celular y beneficios de patrocinadores.
3. **[`Encuesta_3_Padres_Familia.gs`](./Encuesta_3_Padres_Familia.gs)**:
   - Dirigida a: Padres, madres y tutores de familia.
   - Temas: Satisfacción con métodos tradicionales, pensamiento crítico sobre la tecnología en el aula, interés en software de repaso familiar donde solo cargan el PDF del temario sin ser expertos, y actividades deseadas.
4. **[`Generar_Todas_Las_Encuestas.gs`](./Generar_Todas_Las_Encuestas.gs)**:
   - Función maestra para ejecutar y crear las 3 encuestas al mismo tiempo con un solo clic.

---

## 🚀 Pasos para Crear las Encuestas en Google Forms

### Paso 1: Abrir Google Apps Script
1. Abre tu navegador web e ingresa a: **[https://script.google.com/](https://script.google.com/)**
2. Inicia sesión con la cuenta de Google donde deseas que queden almacenados los formularios en tu **Google Drive**.
3. Haz clic en el botón azul **"Nuevo proyecto"** (New project) en la esquina superior izquierda.

### Paso 2: Copiar el Código
Tienes dos opciones:

- **Opción A (Crear una por una)**:
  1. En el editor de Apps Script, borra la función vacía `myFunction()` que aparece por defecto.
  2. Abre el archivo de la encuesta deseada (ej. [`Encuesta_1_Profesores.gs`](./Encuesta_1_Profesores.gs)), copia todo su contenido y pégalo en el editor.
  3. Haz clic en el icono de guardar (💾) o presiona `Ctrl + S`.
  4. En el selector de funciones (arriba al centro), selecciona la función correspondiente:
     - `crearEncuestaProfesores`
     - `crearEncuestaEstudiantes`
     - `crearEncuestaPadresFamilia`

- **Opción B (Crear las 3 encuestas juntas)**:
  1. Puedes pegar el código de las 3 encuestas en el mismo proyecto (o en archivos `.gs` separados dentro del mismo proyecto de Apps Script).
  2. Pega también el archivo [`Generar_Todas_Las_Encuestas.gs`](./Generar_Todas_Las_Encuestas.gs).
  3. Selecciona la función: `crearTodasLasEncuestas`.

### Paso 3: Ejecutar y Otorgar Permisos
1. Haz clic en el botón **"Ejecutar"** (Run).
2. Si es la primera vez que ejecutas Apps Script, Google mostrará una ventana emergente:
   - Haz clic en **"Revisar permisos"** (Review permissions).
   - Selecciona tu cuenta de Google.
   - Si aparece la pantalla *"Google no ha verificado esta aplicación"*, haz clic en **"Avanzado"** (Advanced) abajo a la izquierda.
   - Haz clic en **"Ir a Proyecto sin título (no seguro)"**.
   - Presiona **"Permitir"** (Allow) para que el script pueda crear los formularios en tu Google Drive.

### Paso 4: Obtener los Enlaces de las Encuestas
1. En la parte inferior de la pantalla de Google Apps Script verás el panel **"Registro de ejecución"** (Execution log).
2. El script imprimirá automáticamente:
   - **Enlace de Edición**: URL para que tú como administrador puedas modificar preguntas, cambiar el tema visual o ver respuestas.
   - **Enlace Público para Responder**: URL directa y limpia que compartirás con profesores, estudiantes y padres de familia.
3. Además, los 3 formularios aparecerán guardados en la raíz de tu **Google Drive** ([drive.google.com](https://drive.google.com)).

---

## 🔒 Privacidad, Anonimato y Control de Duplicados

Todas las encuestas vienen configuradas con las siguientes reglas solicitadas:
- **100% Anónimas**: No se guardan correos electrónicos en las hojas de cálculo (`setCollectEmail(false)`).
- **Control de Duplicidad**: Google valida internamente una respuesta por cuenta sin registrar la identidad en el formulario.
- **Datos Demográficos Estandarizados**:
  - Consulta de **Edad** en años numéricos.
  - El **género NO es consultado**.
  - Opciones de provincia o región:
    1. San José
    2. Alajuela
    3. Cartago
    4. Heredia
    5. Puntarenas
    6. Guanacaste
    7. Limón
    8. Extranjero

---

## 📊 Cómo Conectar las Respuestas a Google Sheets (Excel)

Cuando las personas comiencen a responder las encuestas:
1. Abre el formulario desde tu Google Drive.
2. Ve a la pestaña **"Respuestas"**.
3. Haz clic en el icono verde de **"Vincular con Hojas de cálculo"** (Link to Sheets).
4. Se generará automáticamente una hoja de cálculo con todos los datos tabulados en tiempo real para análisis estadístico y visualización de gráficos.
