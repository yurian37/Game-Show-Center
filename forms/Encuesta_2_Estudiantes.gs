/**
 * ============================================================================
 * GAME SHOW CENTER - ESTUDIO DE MERCADO OFICIAL
 * ENCUESTA 2: ESTUDIANTES (PRIMARIA Y SECUNDARIA)
 * ============================================================================
 * 
 * INSTRUCCIONES PARA EJECUTAR EN GOOGLE APPS SCRIPT:
 * 1. Entra a https://script.google.com/ y crea un "Nuevo proyecto".
 * 2. Borra el código existente y pega este archivo completo.
 * 3. En la barra superior, asegúrate de que la función seleccionada sea:
 *    "crearEncuestaEstudiantes"
 * 4. Haz clic en "Ejecutar" (Run) y acepta los permisos de Google si es la primera vez.
 * 5. Revisa el "Registro de ejecución" (Execution log) para obtener los enlaces
 *    del formulario generado (URL de Edición y URL Pública para compartir).
 * ============================================================================
 */

function crearEncuestaEstudiantes() {
  var form = FormApp.create("Encuesta Estudiantil: ¿Cómo te gustaría aprender y jugar en tus clases?");

  // Configuración general
  form.setDescription(
    "¡Hola! 👋 Queremos saber tu opinión sincera sobre cómo son tus clases y cómo te gustaría que " +
    "fueran tus repasos y lecciones con juegos interactivos en pantalla grande.\n\n" +
    "🔒 TU RESPUESTA ES 100% ANÓNIMA:\n" +
    "No te preocupes, tus profesores ni nadie sabrán qué respondiste. Tu correo electrónico " +
    "NO se guardará ni aparecerá en tus respuestas; solo sirve para que cada estudiante vote una vez."
  );
  
  form.setCollectEmail(false);
  form.setProgressBar(true);
  form.setConfirmationMessage(
    "¡Muchas gracias por participar! 🎉 Tus respuestas nos ayudarán a crear juegos y actividades " +
    "geniales para que aprender sea tan divertido como estar en un concurso de televisión."
  );

  var PROVINCIAS = [
    "San José",
    "Alajuela",
    "Cartago",
    "Heredia",
    "Puntarenas",
    "Guanacaste",
    "Limón",
    "Extranjero"
  ];

  // --------------------------------------------------------------------------
  // SECCIÓN 1: DATOS BÁSICOS
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 1: Cuéntanos un poco sobre ti")
    .setHelpText("Datos rápidos y totalmente anónimos.");

  // 1. Edad
  form.addTextItem()
    .setTitle("1. ¿Cuántos años tienes?")
    .setHelpText("Escribe únicamente tu edad en números (ejemplo: 12).")
    .setRequired(true);

  // 2. Provincia
  form.addMultipleChoiceItem()
    .setTitle("2. ¿En qué provincia o lugar estudias o vives?")
    .setChoiceValues(PROVINCIAS)
    .setRequired(true);

  // 3. Nivel educativo
  form.addMultipleChoiceItem()
    .setTitle("3. ¿En qué nivel escolar estás actualmente?")
    .setChoiceValues([
      "Primaria (1° a 3° grado)",
      "Primaria (4° a 6° grado)",
      "Secundaria / Colegio (7° a 9° año)",
      "Educación Diversificada / Bachillerato (10° a 12° año)"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 2: SATISFACCIÓN CON LAS CLASES ORDINARIAS
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 2: ¿Cómo sientes tus clases normales hoy en día?")
    .setHelpText("Sé totalmente sincero/a, nadie sabrá tu nombre.");

  // 4. Nivel de diversión en clases tradicionales
  form.addMultipleChoiceItem()
    .setTitle("4. En general, ¿qué tan entretenidas o interesantes te parecen las clases normales de todos los días?")
    .setChoiceValues([
      "😴 Muy aburridas, me cuesta mucho poner atención",
      "😐 Un poco aburridas, a veces me da sueño o me distraigo",
      "🙂 Normales / Regulares, ni muy divertidas ni muy aburridas",
      "😀 Entretenidas, me gusta cómo enseñan",
      "🔥 Muy divertidas e interesantes todo el tiempo"
    ])
    .setRequired(true);

  // 5. Facilidad para concentrarse con métodos tradicionales
  form.addMultipleChoiceItem()
    .setTitle("5. Cuando el profesor explica la materia solo leyendo libros, hablando mucho rato o dictando en la pizarra, ¿qué te pasa?")
    .setChoiceValues([
      "Me distraigo fácilmente con cualquier cosa o me pongo a dibujar",
      "Intento prestar atención pero me canso rápido",
      "Entiendo la materia pero preferiría que fuera más visual y dinámica",
      "No tengo problema, me concentro bien"
    ])
    .setRequired(true);

  // 6. Aprender jugando
  form.addScaleItem()
    .setTitle("6. ¿Sientes que aprendes y memorizas mucho mejor la materia cuando se usan juegos, trivias o competencias en lugar de solo leer o hacer fotocopias?")
    .setBounds(1, 5)
    .setLabels("1 = Me da igual / Aprendo igual", "5 = ¡Sí, aprendo 100 veces mejor jugando!")
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 3: AMBIENTES INTERACTIVOS, COMPETENCIA Y TRABAJO EN EQUIPO
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 3: Concursos en Pantalla Grande y Dinámicas en Vivo")
    .setHelpText("Imagina convertir tu aula en un auténtico set de televisión.");

  // 7. Show de TV en el aula
  form.addMultipleChoiceItem()
    .setTitle("7. ¿Te gustaría que en tus clases tuvieran dinámicas tipo concurso de televisión (con pantalla gigante, sonido festivo, cronómetro contrarreloj, marcador en vivo y podio con medallas para los ganadores)?")
    .setChoiceValues([
      "😍 ¡Me encantaría, las clases serían increíbles!",
      "👍 Sí, sería muy divertido de vez en cuando",
      "🤔 Tal vez, dependiendo de los juegos",
      "❌ No me llama la atención"
    ])
    .setRequired(true);

  // 8. Deseo de sana competencia
  form.addMultipleChoiceItem()
    .setTitle("8. ¿Qué sientes cuando hay competencias o retos por puntos en el aula?")
    .setChoiceValues([
      "🏆 Me motiva muchísimo ganar puntos y subir al podio de honor",
      "⭐ Me gusta divertirme aunque no gane el primer lugar",
      "😐 Me es indiferente",
      "😰 No me gusta competir porque me da pena o nervios fallar"
    ])
    .setRequired(true);

  // 9. Trabajo en equipo vs Individual
  form.addMultipleChoiceItem()
    .setTitle("9. ¿Cómo prefieres participar en los juegos educativos?")
    .setChoiceValues([
      "👥 En EQUIPOS: Me encanta juntarme con mis compañeros, apoyarnos y ganar juntos",
      "🤝 Me gustan AMBAS modalidades por igual (por equipos y solo)",
      "👤 INDIVIDUAL (1 vs 1 o Todos contra Todos): Prefiero competir solo con mis propios conocimientos"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 4: INTEROPERABILIDAD ENTRE EL CELULAR Y PATROCINADORES
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 4: Uso del Celular, Códigos e Interacción")
    .setHelpText("Queremos saber cómo te gustaría interactuar con las actividades.");

  // 10. Acceso a celular
  form.addMultipleChoiceItem()
    .setTitle("10. ¿Cuentas con teléfono celular propio o tienes acceso a uno durante ciertas actividades?")
    .setChoiceValues([
      "Sí, tengo mi propio teléfono celular inteligente",
      "Tengo acceso al celular de mis papás o me lo prestan",
      "No tengo teléfono celular"
    ])
    .setRequired(true);

  // 11. Interacción celular con la pantalla del juego
  form.addMultipleChoiceItem()
    .setTitle("11. Si pudieras usar tu celular para enviar respuestas a la pantalla grande del aula, escanear códigos de premios, o recibir recompensas escolares de patrocinadores juveniles (ej. descuentos en librerías, entradas de cine, stickers digitales o snacks), ¿qué te parecería?")
    .setChoiceValues([
      "🤩 ¡Genial, me motivaría muchísimo participar!",
      "👌 Me gustaría, siempre que sea fácil y no gaste muchos datos",
      "🤷 Me daría igual los premios",
      "🚫 Prefiero que todo se juegue en la pantalla grande sin necesidad de usar el celular"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 5: TUS JUEGOS FAVORITOS E IDEAS
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 5: ¿Cuáles minijuegos te emocionarían más?")
    .setHelpText("Selecciona los que más te llamen la atención.");

  // 12. Preferencia de tipos de juegos
  form.addCheckboxItem()
    .setTitle("12. ¿Cuáles de estas dinámicas te gustaría que tuvieran tus clases? (Marca todas las que te gusten)")
    .setChoiceValues([
      "Preguntas rápidas contrarreloj tipo trivia con 4 opciones",
      "Descubrir palabras secretas estilo ahorcado antes de que se acabe el tiempo",
      "Línea de tiempo para ordenar fechas históricas o pasos de ciencia",
      "Adivinar qué país o lugar es viendo fotografías reales del mundo",
      "Descubrir personajes famosos o científicos con pistas ocultas",
      "Adivinar canciones, ritmos o instrumentos con sonidos",
      "Adivinar imágenes borrosas o con filtros que se van aclarando poco a poco",
      "Tres en raya gigante respondiendo preguntas para poner tu ficha",
      "Tablero con casillas de 100, 200 y 500 puntos según la dificultad"
    ])
    .setRequired(true);

  // 13. Ideas locas o sugerencias
  form.addParagraphTextItem()
    .setTitle("13. ¿Qué juego o idea genial te gustaría que inventáramos para que tus clases sean inolvidables?")
    .setHelpText("Escribe libremente cualquier sugerencia.")
    .setRequired(false);

  Logger.log("==================================================================");
  Logger.log(" [ÉXITO] Formulario 2 (Estudiantes) Creado Exitosamente");
  Logger.log(" Título: " + form.getTitle());
  Logger.log(" Enlace de Edición: " + form.getEditUrl());
  Logger.log(" Enlace Público para Responder: " + form.getPublishedUrl());
  Logger.log("==================================================================");
}
