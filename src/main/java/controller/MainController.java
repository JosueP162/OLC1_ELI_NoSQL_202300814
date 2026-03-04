package controller;

import analyzer.Lexer;
import analyzer.Parser;
import model.database.Database;
import model.instructions.Instruction;
import model.reports.Error;
import model.reports.Token;

import java.io.*;
import java.util.List;

/**
 * Controlador principal del sistema ELI-NOSQL.
 * Coordina el análisis léxico, sintáctico y la ejecución.
 * 
 * Este es el punto de entrada para la vista (Swing).
 * La vista solo llama a métodos de este controlador.
 */
public class MainController {

    private ExecutionContext ctx;
    private Interpreter interpreter;

    public MainController() {
        this.ctx = new ExecutionContext();
        this.interpreter = new Interpreter(ctx);
    }

    // =====================================================================
    // MÉTODO PRINCIPAL: EJECUTAR CÓDIGO
    // =====================================================================

    /**
     * Analiza y ejecuta código ELI.
     * Siempre intenta ejecutar aunque haya errores léxicos.
     * 
     * @param codigo El código fuente ELI a ejecutar
     */
    public void ejecutar(String codigo) {
        // Limpiar resultados anteriores (pero mantener bases de datos en memoria)
        ctx.fullReset();

        if (codigo == null || codigo.trim().isEmpty()) {
            ctx.print(">> No hay código para ejecutar.");
            return;
        }

        List<Instruction> instrucciones = null;

        try {
            // Crear el lexer y parser
            StringReader reader = new StringReader(codigo);
            Lexer lexer = new Lexer(reader, ctx);
            Parser parser = new Parser(lexer);
            parser.setContext(ctx);

            // Análisis léxico + sintáctico
            java_cup.runtime.Symbol result = parser.parse();

            if (result != null && result.value instanceof List) {
                instrucciones = (List<Instruction>) result.value;
            }

        } catch (Exception e) {
            String _em = e.getMessage();
            if (_em != null && _em.contains("instead expected token")) {
            ctx.print(">> Análisis completado con errores léxicos. Ver tabla de errores.");
                } else {
                    ctx.addError("Sintáctico", "Error: " + _em, 0, 0);
                    ctx.printError("Error de análisis: " + _em);
                }
             }

        // IMPORTANTE: Ejecutar aunque haya errores léxicos/sintácticos
        // Solo detenemos si no hay instrucciones en absoluto
        if (instrucciones != null && !instrucciones.isEmpty()) {
            interpreter.ejecutar(instrucciones);
        } else {
            if (!ctx.hasErrors()) {
                ctx.print(">> Análisis completado sin instrucciones ejecutables.");
            } else {
                ctx.print(">> Análisis completado con errores. Ver tabla de errores.");
            }
        }

        // Cargar persistencias detectadas
        loadPersistedDatabases();
    }

    /**
     * Carga bases de datos desde archivos JSON detectados.
     */
    private void loadPersistedDatabases() {
        for (Database db : ctx.getDatabases().values()) {
            String ruta = db.getRutaPersistencia();
            if (ruta != null) {
                File f = new File(ruta);
                if (f.exists() && db.getTables().isEmpty()) {
                    try {
                        Database loaded = PersistenceManager.load(ruta);
                        if (loaded != null) {
                            // Fusionar tablas cargadas
                            for (model.database.Table t : loaded.getTables().values()) {
                                if (!db.hasTable(t.getNombre())) {
                                    db.addTable(t);
                                }
                            }
                        }
                    } catch (Exception e) {
                        ctx.printError("No se pudo cargar persistencia: " + e.getMessage());
                    }
                }
            }
        }
    }

    // =====================================================================
    // SOLO ANÁLISIS LÉXICO (para reporte de tokens sin ejecutar)
    // =====================================================================

    /**
     * Realiza solo el análisis léxico sin ejecutar.
     * Útil para generar únicamente el reporte de tokens.
     */
    public void analizarLexicamente(String codigo) {
        ctx.resetForExecution();

        if (codigo == null || codigo.trim().isEmpty()) {
            ctx.print(">> No hay código para analizar.");
            return;
        }

        try {
            StringReader reader = new StringReader(codigo);
            Lexer lexer = new Lexer(reader, ctx);

            java_cup.runtime.Symbol token;
            do {
            token = lexer.next_token();
            } while (token.sym != analyzer.sym.EOF && token.sym != 0);

            ctx.print(">> Análisis léxico completado. " + ctx.getTokens().size() + " token(s) encontrados.");

        } catch (Exception e) {
            ctx.addError("Léxico", "Error durante el análisis léxico: " + e.getMessage(), 0, 0);
        }
    }

    // =====================================================================
    // CARGA DE ARCHIVO
    // =====================================================================

    /**
     * Carga el contenido de un archivo .code
     * @param archivo El archivo a cargar
     * @return El contenido del archivo como String
     */
    public String cargarArchivo(File archivo) throws IOException {
        if (!archivo.getName().endsWith(".code")) {
            throw new IOException("El archivo debe tener extensión .code");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Guarda código en un archivo.
     */
    public void guardarArchivo(File archivo, String contenido) throws IOException {
        try (FileWriter fw = new FileWriter(archivo)) {
            fw.write(contenido);
        }
    }

    // =====================================================================
    // CARGA INICIAL DE PERSISTENCIA
    // =====================================================================

    /**
     * Carga una base de datos persistida al iniciar la aplicación.
     */
    public boolean cargarPersistencia(String rutaJson) {
        try {
            Database db = PersistenceManager.load(rutaJson);
            if (db != null) {
                ctx.addDatabase(db);
                ctx.print(">> Persistencia cargada: '" + db.getNombre() + "' desde '" + rutaJson + "'.");
                return true;
            }
        } catch (Exception e) {
            ctx.printError("Error al cargar persistencia: " + e.getMessage());
        }
        return false;
    }

    // =====================================================================
    // GETTERS PARA LA VISTA
    // =====================================================================

    public String getConsoleOutput() {
        return ctx.getConsoleOutput();
    }

    /**
     * Resultado de consultas READ -> panel Output de la UI.
     */
    public String getQueryOutput() {
        return ctx.getQueryOutput();
    }

    public List<Token> getTokens() {
        return ctx.getTokens();
    }

    public List<Error> getErrors() {
        return ctx.getErrors();
    }

    public boolean hasErrors() {
        return ctx.hasErrors();
    }

    public ExecutionContext getContext() {
        return ctx;
    }

    /**
     * Limpia completamente el contexto (nueva sesión).
     */
    public void limpiarTodo() {
        ctx.fullReset();
    }
}