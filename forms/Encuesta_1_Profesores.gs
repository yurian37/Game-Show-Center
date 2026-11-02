/**
 * ============================================================================
 * GAME SHOW CENTER - ESTUDIO DE MERCADO OFICIAL
 * ENCUESTA 1: PROFESORES Y DOCENTES (PRIMARIA, SECUNDARIA Y UNIVERSIDAD)
 * ============================================================================
 * 
 * INSTRUCCIONES PARA EJECUTAR EN GOOGLE APPS SCRIPT:
 * 1. Entra a https://script.google.com/ y crea un "Nuevo proyecto".
 * 2. Borra el código existente y pega este archivo completo.
 * 3. En la barra superior, asegúrate de que la función seleccionada sea:
 *    "crearEncuestaProfesores"
 * 4. Haz clic en "Ejecutar" (Run). Si te pide autorizaciones de cuenta de Google,
 *    acóptalas (Avanzado -> Ir a Proyecto -> Permitir).
 * 5. Revisa el "Registro de ejecución" (Execution log) abajo para obtener los enlaces
 *    del formulario generado (URL de Edición y URL Pública para compartir).
 * ============================================================================
 */

function crearEncuestaProfesores() {
  var form = FormApp.create("Estudio de Mercado: Herramientas Lúdicas y Repaso en el Aula (Docentes)");

  // Configuración general: Anónimo, barra de progreso y mensaje de confirmación
  form.setDescription(
    "Estimado/a docente:\n\n" +
    "Agradecemos su valioso tiempo para responder esta consulta académica sobre las necesidades, " +
    "tiempos y desafíos al preparar dinámicas de clase y sesiones de repaso.\n\n" +
    "🔒 CONFIDENCIALIDAD Y ANONIMATO:\n" +
    "Esta encuesta es 100% ANÓNIMA. Para evitar respuestas duplicadas, el sistema puede solicitar " +
    "que inicie sesión con su cuenta de Google, pero su dirección de correo electrónico NO SERÁ " +
    "almacenada ni vinculada a sus respuestas en ningún momento."
  );
  
  form.setCollectEmail(false);
  form.setProgressBar(true);
  form.setConfirmationMessage(
    "¡Muchas gracias por su valiosa participación! Sus respuestas ayudarán a transformar " +
    "la pedagogía lúdica y a diseñar herramientas que faciliten la labor docente."
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
  // SECCIÓN 1: DATOS GENERALES Y CONTEXTO DOCENTE
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 1: Información General y Contexto Docente")
    .setHelpText("Información básica para fines estadísticos (totalmente anónima).");

  // 1. Edad
  form.addTextItem()
    .setTitle("1. ¿Cuál es su edad actual (en años cumplidos)?")
    .setHelpText("Por favor, escriba únicamente el número (ejemplo: 35).")
    .setRequired(true);

  // 2. Provincia o Región
  form.addMultipleChoiceItem()
    .setTitle("2. ¿En cuál provincia o región imparte clases o reside?")
    .setChoiceValues(PROVINCIAS)
    .setRequired(true);

  // 3. Nivel Educativo
  form.addCheckboxItem()
    .setTitle("3. ¿En qué nivel(es) educativo(s) imparte lecciones actualmente?")
    .setHelpText("Puede marcar más de una opción si aplica.")
    .setChoiceValues([
      "Primaria (I y II Ciclo)",
      "Secundaria Académica (III Ciclo y Educación Diversificada)",
      "Colegios Técnicos Profesionales (CTP)",
      "Educación Superior / Universidad",
      "Educación Especial / Abierta / Nocturna"
    ])
    .setRequired(true);

  // 4. Materias
  form.addCheckboxItem()
    .setTitle("4. ¿Cuáles materias o áreas del conocimiento imparte principalmente?")
    .setChoiceValues([
      "Ciencias (Física, Química, Biología, Ciencias Generales)",
      "Matemáticas",
      "Español / Lengua y Literatura",
      "Estudios Sociales / Historia / Educación Cívica",
      "Idiomas Extranjeros (Inglés, Francés, etc.)",
      "Informática Educativa / Robótica / Tecnología",
      "Artes / Música / Educación Física",
      "Otra"
    ])
    .showOtherOption(true)
    .setRequired(true);

  // 5. Tipo de Centro Educativo
  form.addMultipleChoiceItem()
    .setTitle("5. ¿A qué tipo de centro educativo pertenece?")
    .setChoiceValues([
      "Público",
      "Privado",
      "Subvencionado / Semioficial"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 2: PREPARACIÓN DE MATERIAL DIDÁCTICO Y TIEMPOS DE REPASO
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 2: Preparación de Material Didáctico y Tiempos de Repaso")
    .setHelpText("Queremos conocer cómo gestiona el repaso de contenidos y la preparación de clases.");

  // 6. Frecuencia de actividades de repaso o juegos
  form.addMultipleChoiceItem()
    .setTitle("6. ¿Con qué frecuencia implementa actividades lúdicas, cuestionarios o dinámicas interactivas para repasar antes de exámenes?")
    .setChoiceValues([
      "Todas las semanas",
      "Cada 15 días",
      "Una vez al mes / Al cierre de cada unidad",
      "Rara vez (1 o 2 veces al año)",
      "Casi nunca o nunca por falta de tiempo o herramientas"
    ])
    .setRequired(true);

  // 7. Horas dedicadas a preparar material
  form.addMultipleChoiceItem()
    .setTitle("7. En promedio, ¿cuántas horas semanales invierte fuera del aula en buscar, redactar o preparar material didáctico y preguntas de repaso?")
    .setChoiceValues([
      "Menos de 1 hora semanal",
      "Entre 1 y 2 horas semanales",
      "Entre 3 y 5 horas semanales",
      "Más de 5 horas semanales"
    ])
    .setRequired(true);

  // 8. Desafío de atención en clases ordinarias
  form.addScaleItem()
    .setTitle("8. ¿Qué tan desafiante le resulta mantener la atención e interés activo de sus estudiantes durante las clases ordinarias tradicionales?")
    .setBounds(1, 5)
    .setLabels("1 = Muy fácil mantener la atención", "5 = Extremadamente difícil / Se distraen mucho")
    .setRequired(true);

  // 9. Principales barreras o dificultades
  form.addCheckboxItem()
    .setTitle("9. ¿Cuáles son las mayores dificultades que enfrenta para implementar dinámicas interactivas en su aula?")
    .setHelpText("Seleccione todas las que considere pertinentes.")
    .setChoiceValues([
      "Falta de conexión a Internet o Wi-Fi inestable en la institución",
      "El tiempo excesivo que toma redactar preguntas una por una",
      "No todos los alumnos tienen teléfonos celulares o tablets en el aula",
      "Plataformas existentes en la nube cobran suscripciones en dólares muy caras",
      "Preocupación por la privacidad de datos de los menores de edad",
      "Falta de proyectores o equipo de cómputo apto"
    ])
    .showOtherOption(true)
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 3: EVALUACIÓN DE LOS 10 MINIJUEGOS DE GAME SHOW CENTER
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 3: Evaluación de Minijuegos Didácticos (Game Show Center)")
    .setHelpText(
      "Game Show Center es un motor para proyectar dinámicas tipo concurso televisivo en pantalla grande " +
      "100% offline. A continuación se presentan 10 minijuegos adaptables a cualquier materia. " +
      "Indique su nivel de interés pedagógico en cada uno:"
    );

  var juegos = [
    {
      num: "10.1",
      nombre: "TimeLine (Línea de Tiempo Histórica)",
      desc: "Los estudiantes o equipos deben ordenar cronológicamente eventos, fechas históricas, pasos de un método científico o etapas de un proceso que aparecen en pantalla."
    },
    {
      num: "10.2",
      nombre: "Hangman (Ahorcado Educativo con Pistas)",
      desc: "Descubrimiento de términos clave, vocabulario y conceptos técnicos mediante teclado en pantalla gigante, con pistas descriptivas para reforzar la memoria."
    },
    {
      num: "10.3",
      nombre: "TicTacToe (Tres en Raya Conceptual)",
      desc: "Duelo de estrategia en cuadrícula donde, para marcar una casilla, el equipo o alumno debe responder correctamente una pregunta de la materia."
    },
    {
      num: "10.4",
      nombre: "Trivia Quiz (Cuestionario Televisivo de Opción Múltiple)",
      desc: "Preguntas tipo examen con 4 opciones de respuesta en pantalla gigante, temporizador contrarreloj y explicación didáctica inmediata tras responder."
    },
    {
      num: "10.5",
      nombre: "Zero Margin (Precisión Temporal y Estimación)",
      desc: "Desafío de agilidad mental y cronómetro a ciegas para cálculos, estimaciones de cantidades numéricas y toma rápida de decisiones."
    },
    {
      num: "10.6",
      nombre: "GeoLocation (Geografía Visual y Cultura del Mundo)",
      desc: "Adivinar países, capitales, relieves, ecosistemas o monumentos mediante fotografías reales en alta resolución proyectadas en el aula."
    },
    {
      num: "10.7",
      nombre: "Guess Character (Adivina el Personaje Histórico / Científico)",
      desc: "Descubrir a figuras históricas, científicas, literarias o autores emblemáticos a través de pistas progresivas reveladas una por una."
    },
    {
      num: "10.8",
      nombre: "Rapid Rhythm (Reconocimiento Auditivo y Musical)",
      desc: "Desafío basado en fragmentos de audio para identificar canciones patrias, obras clásicas, instrumentos, sonidos de la naturaleza o conceptos fonéticos."
    },
    {
      num: "10.9",
      nombre: "Snap Solve (Identificación Visual con Revelado Progresivo)",
      desc: "Imágenes de arte, células, esquemas anatómicos o estructuras que se revelan poco a poco o tras filtros visuales para premiar la deducción rápida."
    },
    {
      num: "10.10",
      nombre: "Topic Takedown (Tablero de Categorías tipo Jeopardy)",
      desc: "Panel gigante dividido en temas curriculares y valores de puntos (100, 200, 300, etc.), donde los equipos escogen estratégicamente qué reto responder."
    }
  ];

  for (var i = 0; i < juegos.length; i++) {
    var j = juegos[i];
    form.addMultipleChoiceItem()
      .setTitle(j.num + ". " + j.nombre)
      .setHelpText(j.desc)
      .setChoiceValues([
        "⭐⭐⭐⭐⭐ Excelente / Muy útil para mis clases",
        "⭐⭐⭐⭐ Bueno / Lo utilizaría frecuentemente",
        "⭐⭐⭐ Regular / Dependería del tema",
        "⭐⭐ Poco útil / No se adapta a mi materia",
        "⭐ No me gusta / No lo usaría"
      ])
      .setRequired(true);
  }

  // Campo de sugerencias abiertas sobre juegos
  form.addParagraphTextItem()
    .setTitle("11. ¿Qué sugerencias pedagógicas, mecánicas o dinámicas adicionales le gustaría que se agregaran a estos minijuegos?")
    .setHelpText("Escriba cualquier idea o necesidad específica de su asignatura.")
    .setRequired(false);

  // --------------------------------------------------------------------------
  // SECCIÓN 4: INTELIGENCIA ARTIFICIAL LOCAL (RAG) Y OPERACIÓN OFFLINE
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 4: Inteligencia Artificial Local (RAG) y Privacidad")
    .setHelpText("Innovación pedagógica asistida por Inteligencia Artificial sin conexión a Internet.");

  // 12. Utilidad de IA con PDFs propios
  form.addScaleItem()
    .setTitle("12. Si el software le permitiera cargar sus propios libros de texto o programas de estudio en PDF para que una IA genere automáticamente las preguntas y actividades del juego en segundos, ¿qué tan valioso sería para usted?")
    .setBounds(1, 5)
    .setLabels("1 = Nada valioso / Prefiero hacerlas a mano", "5 = Extremadamente valioso / Me ahorraría mucho tiempo")
    .setRequired(true);

  // 13. Importancia de operar 100% Offline
  form.addScaleItem()
    .setTitle("13. ¿Qué tan importante considera que el software funcione 100% sin Internet en el aula (garantizando cero fallas de red y total privacidad de datos de los menores)?")
    .setBounds(1, 5)
    .setLabels("1 = Poco importante", "5 = Fundamental e indispensable")
    .setRequired(true);

  // 14. Disposición a uso
  form.addMultipleChoiceItem()
    .setTitle("14. Si su institución educativa o usted tuviera acceso a esta herramienta con una licencia accesible, ¿la adoptaría en su plan de clases?")
    .setChoiceValues([
      "Definitivamente sí",
      "Probablemente sí",
      "Tal vez, si me capacitan previamente",
      "Poco probable",
      "No me interesa"
    ])
    .setRequired(true);

  // Comentarios finales
  form.addParagraphTextItem()
    .setTitle("15. Comentarios adicionales o expectativas:")
    .setHelpText("Cualquier sugerencia que nos ayude a crear la mejor herramienta didáctica para su labor.")
    .setRequired(false);

  // Mensaje en consola con enlaces
  Logger.log("==================================================================");
  Logger.log(" [ÉXITO] Formulario 1 (Profesores) Creado Exitosamente");
  Logger.log(" Título: " + form.getTitle());
  Logger.log(" Enlace de Edición: " + form.getEditUrl());
  Logger.log(" Enlace Público para Responder: " + form.getPublishedUrl());
  Logger.log("==================================================================");
}
