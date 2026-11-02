/**
 * ============================================================================
 * GAME SHOW CENTER - GENERADOR MAESTRO DE ENCUESTAS
 * CREACIÓN AUTOMATIZADA DE LAS 3 ENCUESTAS EN GOOGLE FORMS CON 1 SOLO CLIC
 * ============================================================================
 * 
 * INSTRUCCIONES:
 * 1. Abre https://script.google.com/
 * 2. Pega este archivo en tu editor de Google Apps Script.
 * 3. Selecciona la función: "crearTodasLasEncuestas"
 * 4. Presiona "Ejecutar" (Run).
 * 5. Revisa el Registro de ejecución (Execution Log). Verás las URLs de edición
 *    y los enlaces públicos de las 3 encuestas listos para compartir.
 * ============================================================================
 */

function crearTodasLasEncuestas() {
  Logger.log("=================================================================");
  Logger.log(" INICIANDO GENERACIÓN MASIVA DE ENCUESTAS DE GAME SHOW CENTER    ");
  Logger.log("=================================================================\n");

  // 1. Ejecutar Encuesta de Profesores
  Logger.log(">>> [1/3] Creando Encuesta 1: Profesores y Docentes...");
  try {
    crearEncuestaProfesores();
  } catch (e) {
    Logger.log("Error al crear Encuesta 1: " + e.toString());
  }

  // 2. Ejecutar Encuesta de Estudiantes
  Logger.log("\n>>> [2/3] Creando Encuesta 2: Estudiantes (Primaria y Secundaria)...");
  try {
    crearEncuestaEstudiantes();
  } catch (e) {
    Logger.log("Error al crear Encuesta 2: " + e.toString());
  }

  // 3. Ejecutar Encuesta de Padres de Familia
  Logger.log("\n>>> [3/3] Creando Encuesta 3: Padres, Madres y Tutores de Familia...");
  try {
    crearEncuestaPadresFamilia();
  } catch (e) {
    Logger.log("Error al crear Encuesta 3: " + e.toString());
  }

  Logger.log("\n=================================================================");
  Logger.log(" ¡PROCESO FINALIZADO CON ÉXITO!                                  ");
  Logger.log(" Revisa los enlaces arriba y en tu Google Drive (drive.google.com)");
  Logger.log("=================================================================");
}
