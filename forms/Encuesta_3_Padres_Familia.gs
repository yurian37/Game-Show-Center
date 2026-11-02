/**
 * ============================================================================
 * GAME SHOW CENTER - ESTUDIO DE MERCADO OFICIAL
 * ENCUESTA 3: PADRES, MADRES Y TUTORES DE FAMILIA
 * ============================================================================
 * 
 * INSTRUCCIONES PARA EJECUTAR EN GOOGLE APPS SCRIPT:
 * 1. Entra a https://script.google.com/ y crea un "Nuevo proyecto".
 * 2. Borra el código existente y pega este archivo completo.
 * 3. En la barra superior, asegúrate de que la función seleccionada sea:
 *    "crearEncuestaPadresFamilia"
 * 4. Haz clic en "Ejecutar" (Run) y acepta los permisos de Google si es la primera vez.
 * 5. Revisa el "Registro de ejecución" (Execution log) para obtener los enlaces
 *    del formulario generado (URL de Edición y URL Pública para compartir).
 * ============================================================================
 */

function crearEncuestaPadresFamilia() {
  var form = FormApp.create("Consulta a Familias: Aprendizaje, Tecnología y Apoyo Escolar en el Hogar");

  // Configuración general
  form.setDescription(
    "Estimados padres, madres y tutores de familia:\n\n" +
    "Su perspectiva es fundamental para comprender cómo mejorar la motivación académica de sus hijos, " +
    "tanto en las aulas como durante las horas de estudio en el hogar.\n\n" +
    "🔒 ENCUESTA 100% ANÓNIMA Y CONFIDENCIAL:\n" +
    "No solicitamos nombres ni datos personales identificables. El sistema de Google puede solicitar " +
    "que inicie sesión únicamente para garantizar una sola respuesta por persona, pero su dirección " +
    "de correo electrónico NO SERÁ registrada ni almacenada junto a sus respuestas."
  );
  
  form.setCollectEmail(false);
  form.setProgressBar(true);
  form.setConfirmationMessage(
    "¡Agradecemos sinceramente su tiempo y aportes! Sus opiniones nos orientarán para desarrollar " +
    "tecnologías educativas que fortalezcan el vínculo familiar y el éxito académico de sus hijos."
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
  // SECCIÓN 1: DATOS GENERALES DEL HOGAR
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 1: Información General del Hogar")
    .setHelpText("Datos estadísticos básicos totalmente anónimos.");

  // 1. Edad del padre/madre/tutor
  form.addTextItem()
    .setTitle("1. ¿Cuál es su edad (en años cumplidos)?")
    .setHelpText("Por favor, escriba únicamente el número (ejemplo: 42).")
    .setRequired(true);

  // 2. Provincia de residencia
  form.addMultipleChoiceItem()
    .setTitle("2. ¿En qué provincia o región reside su núcleo familiar?")
    .setChoiceValues(PROVINCIAS)
    .setRequired(true);

  // 3. Nivel escolar de sus hijos
  form.addCheckboxItem()
    .setTitle("3. ¿En qué nivel(es) educativo(s) se encuentran sus hijos actualmente?")
    .setHelpText("Puede marcar más de una opción si tiene varios hijos en edad escolar.")
    .setChoiceValues([
      "Preescolar / Materno / Kínder",
      "Primaria (1° a 3° grado)",
      "Primaria (4° a 6° grado)",
      "Secundaria / Colegio (7° a 9° año)",
      "Educación Diversificada / Bachillerato (10° a 12° año)",
      "Educación Técnica o Universitaria"
    ])
    .setRequired(true);

  // 4. Tipo de institución educativa
  form.addMultipleChoiceItem()
    .setTitle("4. ¿A qué tipo de centro educativo asisten sus hijos principalmente?")
    .setChoiceValues([
      "Educación Pública",
      "Educación Privada",
      "Educación Subvencionada / Semioficial"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 2: SATISFACCIÓN CON LAS CLASES Y MOTIVACIÓN ESCOLAR
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 2: Satisfacción con la Metodología Escolar Actual")
    .setHelpText("Evaluación de la dinámica ordinaria de clases y horas de estudio.");

  // 5. Satisfacción con clases ordinarias
  form.addScaleItem()
    .setTitle("5. ¿Qué tan satisfecho/a se siente con los métodos tradicionales de enseñanza y repaso que reciben sus hijos en sus clases ordinarias?")
    .setBounds(1, 5)
    .setLabels("1 = Muy insatisfecho / Muy monótonas", "5 = Totalmente satisfecho / Dinámicas excelentes")
    .setRequired(true);

  // 6. Nivel de motivación al estudiar en casa
  form.addMultipleChoiceItem()
    .setTitle("6. Cuando llega el momento de estudiar o repasar para exámenes en casa, ¿cómo describiría la actitud y motivación de sus hijos?")
    .setChoiceValues([
      "😫 Muy desmotivados / Se convierte en un momento de tensión o conflicto familiar",
      "🥱 Cuesta mucho que se concentren, se distraen con el celular, pantallas o videojuegos",
      "😐 Regular / Estudian por obligación sin mucho entusiasmo",
      "😊 Motivados / Estudian con relativa facilidad y gusto"
    ])
    .setRequired(true);

  // 7. Tiempo dedicado a apoyar el estudio
  form.addMultipleChoiceItem()
    .setTitle("7. Como padre o madre, ¿cuánto tiempo a la semana acompaña o ayuda activamente a sus hijos en tareas y repasos?")
    .setChoiceValues([
      "Menos de 2 horas a la semana",
      "Entre 2 y 5 horas a la semana",
      "Más de 5 horas a la semana",
      "Casi no dispongo de tiempo por motivos de trabajo"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 3: PENSAMIENTO CRÍTICO SOBRE LA TECNOLOGÍA EN LA EDUCACIÓN
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 3: Pensamiento Crítico sobre la Tecnología y Pantallas")
    .setHelpText("Su criterio sobre el rol de la tecnología en el aprendizaje.");

  // 8. Criterio de tecnología en el aula
  form.addMultipleChoiceItem()
    .setTitle("8. Frente al uso de pantallas en el aula y el hogar, ¿cuál refleja mejor su postura?")
    .setChoiceValues([
      "La tecnología es muy positiva si se usa para juegos educativos, trivias guiadas y aprendizaje activo",
      "La tecnología es buena, pero me preocupa que distraiga si no hay una supervisión muy estricta del docente",
      "Prefiero métodos tradicionales con libros físicos y cuadernos",
      "Depende totalmente del tipo de programa o juego que se utilice"
    ])
    .setRequired(true);

  // 9. Alternativas educativas a las redes sociales
  form.addScaleItem()
    .setTitle("9. Ante el bombardeo de redes sociales (TikTok, YouTube, etc.) y videojuegos comerciales, ¿qué tan necesario considera que existan juegos para PC diseñados específicamente para retar la mente, la cultura y el aprendizaje familiar?")
    .setBounds(1, 5)
    .setLabels("1 = Poco necesario", "5 = Extremadamente necesario y urgente")
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 4: SOFTWARE DE REPASO FAMILIAR SIN SER EXPERTO EN LA MATERIA
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 4: Software Educativo para el Hogar (Game Show Center)")
    .setHelpText("Apoyo a los padres sin necesidad de dominar los temarios complejos.");

  // 10. Dificultad para dominar materias avanzadas
  form.addMultipleChoiceItem()
    .setTitle("10. A medida que sus hijos avanzan de grado (matemáticas avanzadas, química, historia detallada, etc.), ¿qué tan difícil le resulta a usted como padre/madre formular preguntas y evaluar si dominan la materia?")
    .setChoiceValues([
      "Muy difícil, no domino todos los temas ni tengo tiempo para diseñar preguntas",
      "Moderadamente difícil, requiere que me siente a leer y repasar con ellos previamente",
      "Fácil, manejo bien los temarios escolares"
    ])
    .setRequired(true);

  // 11. Carga automática de temarios / PDFs
  form.addScaleItem()
    .setTitle("11. Si existiera un software para computadora donde usted simplemente arrastra el archivo PDF del temario o libro escolar de su hijo y el programa crea automáticamente un concurso de televisión interactivo para repasar en familia, ¿qué tan útil le resultaría?")
    .setBounds(1, 5)
    .setLabels("1 = No me interesaría", "5 = ¡Sería una solución maravillosa para mi hogar!")
    .setRequired(true);

  // 12. Disposición de adquisición
  form.addMultipleChoiceItem()
    .setTitle("12. ¿Estaría dispuesto/a a adquirir una licencia anual de bajo costo para su hogar si esta herramienta garantiza aumentar el interés y mejorar el rendimiento académico de sus hijos?")
    .setChoiceValues([
      "Sí, con total seguridad",
      "Probablemente sí, si el costo es razonable y accesible",
      "Dependería mucho del precio y de probar una versión demostrativa",
      "No estaría dispuesto/a a pagar"
    ])
    .setRequired(true);

  // --------------------------------------------------------------------------
  // SECCIÓN 5: ACTIVIDADES QUE LES GUSTARÍA INCORPORAR
  // --------------------------------------------------------------------------
  form.addPageBreakItem()
    .setTitle("Sección 5: Actividades y Dinámicas Deseadas para el Hogar")
    .setHelpText("Ayúdenos a priorizar qué contenidos son los más urgentes para sus hijos.");

  // 13. Áreas prioritarias
  form.addCheckboxItem()
    .setTitle("13. ¿Qué tipo de dinámicas o actividades le gustaría especialmente que incluyera este software para sus hijos? (Marque las que considere prioritarias)")
    .setChoiceValues([
      "Práctica de cálculo mental rápido y resolución de problemas matemáticos",
      "Comprensión de lectura interactiva con preguntas de deducción",
      "Ortografía, vocabulario y juegos de palabras sinónimas/antónimas",
      "Cultura general, geografía nacional y del mundo con fotografías reales",
      "Lógica espacial, memoria visual y razonamiento abstracto",
      "Refuerzo de inglés u otros idiomas extranjeros de manera conversacional",
      "Valores cívicos, seguridad digital y educación vial"
    ])
    .showOtherOption(true)
    .setRequired(true);

  // 14. Sugerencias abiertas
  form.addParagraphTextItem()
    .setTitle("14. ¿Qué consejo, sugerencia o necesidad particular le gustaría compartir con el equipo de desarrollo de Game Show Center?")
    .setHelpText("Su opinión nos ayuda a crear la herramienta que las familias realmente necesitan.")
    .setRequired(false);

  Logger.log("==================================================================");
  Logger.log(" [ÉXITO] Formulario 3 (Padres de Familia) Creado Exitosamente");
  Logger.log(" Título: " + form.getTitle());
  Logger.log(" Enlace de Edición: " + form.getEditUrl());
  Logger.log(" Enlace Público para Responder: " + form.getPublishedUrl());
  Logger.log("==================================================================");
}
