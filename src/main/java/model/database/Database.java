package model.database;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa una base de datos dentro del lenguaje ELI.
 * Contiene tablas y tiene una ruta de persistencia JSON.
 */
public class Database {
    private String nombre;
    private String rutaPersistencia; // ruta del archivo .json
    private LinkedHashMap<String, Table> tables;

    public Database(String nombre) {
        this.nombre = nombre;
        this.tables = new LinkedHashMap<>();
        this.rutaPersistencia = nombre + ".json"; // ruta por defecto
    }

    public Database(String nombre, String rutaPersistencia) {
        this.nombre = nombre;
        this.rutaPersistencia = rutaPersistencia;
        this.tables = new LinkedHashMap<>();
    }

    // ===================== TABLES =====================

    /**
     * Agrega una tabla a la base de datos.
     */
    public void addTable(Table table) {
        tables.put(table.getNombre(), table);
    }

    /**
     * Verifica si una tabla existe.
     */
    public boolean hasTable(String nombreTabla) {
        return tables.containsKey(nombreTabla);
    }

    /**
     * Obtiene una tabla por nombre.
     */
    public Table getTable(String nombreTabla) {
        return tables.get(nombreTabla);
    }

    /**
     * Retorna todas las tablas.
     */
    public Map<String, Table> getTables() {
        return tables;
    }

    // ===================== GETTERS / SETTERS =====================

    public String getNombre() { return nombre; }

    public String getRutaPersistencia() { return rutaPersistencia; }

    public void setRutaPersistencia(String ruta) { this.rutaPersistencia = ruta; }

    @Override
    public String toString() {
        return "Database{nombre='" + nombre + "', ruta='" + rutaPersistencia +
               "', tablas=" + tables.keySet() + "}";
    }
}
