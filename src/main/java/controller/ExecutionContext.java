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
 * Contexto de ejecución compartido por todas las instrucciones.
 * Contiene:
 *  - Mapa de bases de datos definidas
 *  - Base de datos activa (USE)
 *  - Lista de errores encontrados
 *  - Lista de tokens reconocidos
 *  - Resultado de la última consulta READ (para EXPORT)
 *  - Salida de consola
 */
public class ExecutionContext {

    // ===================== BASES DE DATOS =====================
    private Map<String, Database> databases;
    private Database activeDatabase;

    // ===================== ULTIMA CONSULTA (para EXPORT) =====================
    private List<Record> lastQueryResult;
    private String lastQueryTable;
    private List<String> lastQueryFields;

    // ===================== REPORTES =====================
    private List<Token> tokens;
    private List<Error> errors;

    // ===================== SALIDA CONSOLA =====================
    private StringBuilder consoleOutput;
    private StringBuilder queryOutput; // tablas READ -> panel Output

    public ExecutionContext() {
        databases = new LinkedHashMap<>();
        activeDatabase = null;
        lastQueryResult = null;
        lastQueryTable = null;
        lastQueryFields = null;
        tokens = new ArrayList<>();
        errors = new ArrayList<>();
        consoleOutput = new StringBuilder();
        queryOutput = new StringBuilder();
    }

    // ===================== DATABASES =====================

    public void addDatabase(Database db) {
        databases.put(db.getNombre(), db);
    }

    public boolean hasDatabase(String nombre) {
        return databases.containsKey(nombre);
    }

    public Database getDatabase(String nombre) {
        return databases.get(nombre);
    }

    public Map<String, Database> getDatabases() {
        return databases;
    }

    // ===================== ACTIVE DATABASE =====================

    /**
     * Selecciona la base de datos activa (instrucción USE).
     * Retorna true si se encontró la base de datos, false si no existe.
     */
    public boolean setActiveDatabase(String nombre) {
        if (databases.containsKey(nombre)) {
            activeDatabase = databases.get(nombre);
            return true;
        }
        return false;
    }

    public Database getActiveDatabase() {
        return activeDatabase;
    }

    public boolean hasActiveDatabase() {
        return activeDatabase != null;
    }

    // ===================== TABLA ACTIVA (helper) =====================

    /**
     * Obtiene una tabla de la base de datos activa.
     * Retorna null si no hay base de datos activa o la tabla no existe.
     */
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
        this.lastQueryTable = tableName;
        this.lastQueryFields = new ArrayList<>(fields);
        this.lastQueryResult = new ArrayList<>(records);
    }

    public List<Record> getLastQueryResult() { return lastQueryResult; }
    public String getLastQueryTable() { return lastQueryTable; }
    public List<String> getLastQueryFields() { return lastQueryFields; }
    public boolean hasLastQuery() { return lastQueryResult != null; }

    // ===================== TOKENS =====================

    public void addToken(Token token) {
        tokens.add(token);
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void clearTokens() {
        tokens.clear();
    }

    // ===================== ERRORS =====================

    public void addError(Error error) {
        errors.add(error);
    }

    public void addError(String tipo, String descripcion, int linea, int columna) {
        errors.add(new Error(errors.size() + 1, tipo, descripcion, linea, columna));
    }

    public List<Error> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void clearErrors() {
        errors.clear();
    }

    // ===================== CONSOLA =====================

    public void print(String mensaje) {
        consoleOutput.append(mensaje).append("\n");
    }

    public void printError(String mensaje) {
        consoleOutput.append("[ERROR] ").append(mensaje).append("\n");
    }

    public String getConsoleOutput() {
        return consoleOutput.toString();
    }

    public void clearConsole() {
        consoleOutput = new StringBuilder();
    }

    // ===================== RESET COMPLETO =====================

    /**
     * Limpia todo para una nueva ejecución pero mantiene las bases de datos en memoria.
     */
    public void resetForExecution() {
        clearErrors();
        clearTokens();
        clearConsole();
        clearQueryOutput();
        lastQueryResult = null;
        lastQueryTable = null;
        lastQueryFields = null;
    }

    // ===================== QUERY OUTPUT (tablas READ -> panel Output) =====================

    /**
     * Registra el resultado de una instruccion READ para mostrarse
     * en el panel Output de la UI, separado de la consola.
     */
    public void printQueryResult(String tableName, int count, String tablaAscii) {
        queryOutput.append("================================================\n");
        queryOutput.append(String.format("  Tabla: %-20s | %d registro(s)\n", tableName, count));
        queryOutput.append("================================================\n");
        queryOutput.append(tablaAscii).append("\n\n");
    }

    public String getQueryOutput()   { return queryOutput.toString(); }
    public void   clearQueryOutput() { queryOutput = new StringBuilder(); }

        /**
     * Limpia absolutamente todo (nueva sesión desde cero).
     */
    public void fullReset() {
        databases.clear();
        activeDatabase = null;
        resetForExecution();
    }
}