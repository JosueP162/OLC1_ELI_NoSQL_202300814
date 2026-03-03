package controller;

import model.database.Database;
import model.database.Record;
import model.database.Table;
import model.reports.Error;
import model.reports.Token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contexto de ejecucion compartido por todas las instrucciones.
 *
 * Separacion de salidas:
 *  - consoleOutput : mensajes generales del interprete  (panel Consola de la UI)
 *  - queryOutput   : tablas resultado de READ           (panel Output de la UI)
 */
public class ExecutionContext {

    private Map<String, Database> databases;
    private Database activeDatabase;

    private List<Record> lastQueryResult;
    private String       lastQueryTable;
    private List<String> lastQueryFields;

    private List<Token> tokens;
    private List<Error> errors;

    private StringBuilder consoleOutput;
    private StringBuilder queryOutput;

    public ExecutionContext() {
        databases       = new LinkedHashMap<>();
        activeDatabase  = null;
        lastQueryResult = null;
        lastQueryTable  = null;
        lastQueryFields = null;
        tokens          = new ArrayList<>();
        errors          = new ArrayList<>();
        consoleOutput   = new StringBuilder();
        queryOutput     = new StringBuilder();
    }

    // ===================== DATABASES =====================

    public void addDatabase(Database db) { databases.put(db.getNombre(), db); }
    public boolean hasDatabase(String nombre) { return databases.containsKey(nombre); }
    public Database getDatabase(String nombre) { return databases.get(nombre); }
    public Map<String, Database> getDatabases() { return databases; }

    // ===================== ACTIVE DATABASE =====================

    public boolean setActiveDatabase(String nombre) {
        if (databases.containsKey(nombre)) {
            activeDatabase = databases.get(nombre);
            return true;
        }
        return false;
    }

    public Database getActiveDatabase()   { return activeDatabase; }
    public boolean  hasActiveDatabase()   { return activeDatabase != null; }

    // ===================== TABLA ACTIVA =====================

    public Table getActiveTable(String nombreTabla) {
        if (activeDatabase == null) return null;
        return activeDatabase.getTable(nombreTabla);
    }

    public boolean hasActiveTable(String nombreTabla) {
        if (activeDatabase == null) return false;
        return activeDatabase.hasTable(nombreTabla);
    }

    // ===================== ULTIMA CONSULTA =====================

    public void setLastQueryResult(String tableName, List<String> fields, List<Record> records) {
        this.lastQueryTable  = tableName;
        this.lastQueryFields = new ArrayList<>(fields);
        this.lastQueryResult = new ArrayList<>(records);
    }

    public List<Record> getLastQueryResult()  { return lastQueryResult; }
    public String       getLastQueryTable()   { return lastQueryTable; }
    public List<String> getLastQueryFields()  { return lastQueryFields; }
    public boolean      hasLastQuery()        { return lastQueryResult != null; }

    // ===================== TOKENS =====================

    public void        addToken(Token token) { tokens.add(token); }
    public List<Token> getTokens()           { return tokens; }
    public void        clearTokens()         { tokens.clear(); }

    // ===================== ERRORS =====================

    public void addError(Error error) { errors.add(error); }

    public void addError(String tipo, String descripcion, int linea, int columna) {
        errors.add(new Error(errors.size() + 1, tipo, descripcion, linea, columna));
    }

    public List<Error> getErrors()  { return errors; }
    public boolean          hasErrors()  { return !errors.isEmpty(); }
    public void             clearErrors(){ errors.clear(); }

    // ===================== CONSOLA =====================

    /** Mensaje informativo -> panel Consola de la UI. */
    public void print(String mensaje) {
        consoleOutput.append(mensaje).append("\n");
    }

    /** Mensaje de error -> panel Consola de la UI. */
    public void printError(String mensaje) {
        consoleOutput.append("[ERROR] ").append(mensaje).append("\n");
    }

    public String getConsoleOutput() { return consoleOutput.toString(); }
    public void   clearConsole()     { consoleOutput = new StringBuilder(); }

    // ===================== QUERY OUTPUT (tablas READ -> Output UI) =====================

    /**
     * Registra el resultado visual de una instruccion READ.
     * Se muestra en el panel "Output" de la UI (no en la consola).
     */
    public void printQueryResult(String tableName, int count, String tablaAscii) {
        queryOutput.append("===================================================\n");
        queryOutput.append(String.format("  Tabla: %-22s  %d registro(s)\n", tableName, count));
        queryOutput.append("===================================================\n");
        queryOutput.append(tablaAscii).append("\n\n");
    }

    public String getQueryOutput()  { return queryOutput.toString(); }
    public void   clearQueryOutput(){ queryOutput = new StringBuilder(); }

    // ===================== RESET =====================

    public void resetForExecution() {
        clearErrors();
        clearTokens();
        clearConsole();
        clearQueryOutput();
        lastQueryResult = null;
        lastQueryTable  = null;
        lastQueryFields = null;
    }

    public void fullReset() {
        databases.clear();
        activeDatabase = null;
        resetForExecution();
    }
}