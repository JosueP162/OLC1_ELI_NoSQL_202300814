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
 * Coordina analisis lexico, sintactico y ejecucion.
 * Expone getConsoleOutput() y getQueryOutput() para que
 * la vista los muestre en paneles separados.
 */
public class MainController {

    private ExecutionContext ctx;
    private Interpreter interpreter;

    public MainController() {
        this.ctx         = new ExecutionContext();
        this.interpreter = new Interpreter(ctx);
    }

    // =====================================================================
    // EJECUTAR CODIGO
    // =====================================================================

    /**
     * Analiza y ejecuta codigo ELI.
     * Siempre intenta ejecutar aunque haya errores lexicos.
     */
    public void ejecutar(String codigo) {
        ctx.resetForExecution();

        if (codigo == null || codigo.trim().isEmpty()) {
            ctx.print(">> No hay codigo para ejecutar.");
            return;
        }

        List<Instruction> instrucciones = null;

        try {
            StringReader reader = new StringReader(codigo);
            Lexer lexer         = new Lexer(reader, ctx);
            Parser parser       = new Parser(lexer);
            parser.setContext(ctx);

            java_cup.runtime.Symbol result = parser.parse();

            if (result != null && result.value instanceof List) {
                instrucciones = (List<Instruction>) result.value;
            }

        } catch (Exception e) {
            ctx.addError("Sintactico", "Error de analisis: " + e.getMessage(), 0, 0);
            ctx.printError("Error durante el analisis: " + e.getMessage());
        }

        if (instrucciones != null && !instrucciones.isEmpty()) {
            interpreter.ejecutar(instrucciones);
        } else {
            if (!ctx.hasErrors()) {
                ctx.print(">> Analisis completado sin instrucciones ejecutables.");
            } else {
                ctx.print(">> Analisis completado con errores. Ver Reportes > Tabla de Errores.");
            }
        }

        loadPersistedDatabases();
    }

    /**
     * Carga bases de datos desde archivos JSON detectados tras la ejecucion.
     */
    private void loadPersistedDatabases() {
        for (Database db : ctx.getDatabases().values()) {
            String ruta = db.getRutaPersistencia();
            if (ruta != null) {
                java.io.File f = new java.io.File(ruta);
                if (f.exists() && db.getTables().isEmpty()) {
                    try {
                        Database loaded = PersistenceManager.load(ruta);
                        if (loaded != null) {
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
    // SOLO ANALISIS LEXICO
    // =====================================================================

    public void analizarLexicamente(String codigo) {
        ctx.resetForExecution();

        if (codigo == null || codigo.trim().isEmpty()) {
            ctx.print(">> No hay codigo para analizar.");
            return;
        }

        try {
            StringReader reader = new StringReader(codigo);
            Lexer lexer         = new Lexer(reader, ctx);

            java_cup.runtime.Symbol token;
            do {
                token = lexer.next_token();
            } while (token.sym != analyzer.sym.EOF && token.sym != 0);

            ctx.print(">> Analisis lexico completado. " + ctx.getTokens().size() + " token(s) encontrados.");

        } catch (Exception e) {
            ctx.addError("Lexico", "Error durante el analisis lexico: " + e.getMessage(), 0, 0);
        }
    }

    // =====================================================================
    // CARGA / GUARDADO DE ARCHIVOS
    // =====================================================================

    public String cargarArchivo(java.io.File archivo) throws IOException {
        if (!archivo.getName().endsWith(".code")) {
            throw new IOException("El archivo debe tener extension .code");
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

    public void guardarArchivo(java.io.File archivo, String contenido) throws IOException {
        try (FileWriter fw = new FileWriter(archivo)) {
            fw.write(contenido);
        }
    }

    // =====================================================================
    // CARGA INICIAL DE PERSISTENCIA
    // =====================================================================

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

    /** Mensajes generales del interprete -> panel Consola de la UI. */
    public String getConsoleOutput() {
        return ctx.getConsoleOutput();
    }

    /**
     * Tablas resultado de instrucciones READ -> panel Output de la UI.
     * Si no hay resultados, retorna cadena vacia.
     */
    public String getQueryOutput() {
        return ctx.getQueryOutput();
    }

    public List<Token> getTokens() { return ctx.getTokens(); }
    public List<Error> getErrors() { return ctx.getErrors(); }
    public boolean     hasErrors() { return ctx.hasErrors(); }

    public ExecutionContext getContext() { return ctx; }

    /** Limpia completamente el contexto (nueva sesion). */
    public void limpiarTodo() { ctx.fullReset(); }
}