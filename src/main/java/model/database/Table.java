package model.database;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa una tabla dentro de la base de datos ELI.
 * Contiene el esquema (campos) y los registros almacenados.
 */
public class Table {
    private String nombre;
    private LinkedHashMap<String, Field> schema; // nombre_campo -> Field
    private List<Record> records;

    public Table(String nombre) {
        this.nombre = nombre;
        this.schema = new LinkedHashMap<>();
        this.records = new ArrayList<>();
    }

    // ===================== SCHEMA =====================

    /**
     * Agrega un campo al esquema de la tabla.
     */
    public void addField(Field field) {
        schema.put(field.getNombre(), field);
    }

    /**
     * Verifica si un campo existe en el esquema.
     */
    public boolean hasField(String nombreCampo) {
        return schema.containsKey(nombreCampo);
    }

    /**
     * Obtiene un campo del esquema.
     */
    public Field getField(String nombreCampo) {
        return schema.get(nombreCampo);
    }

    /**
     * Retorna todos los campos del esquema.
     */
    public Map<String, Field> getSchema() {
        return schema;
    }

    // ===================== RECORDS =====================

    /**
     * Inserta un nuevo registro en la tabla.
     * Completa los campos faltantes con valores por defecto.
     */
    public void addRecord(Record record) {
        // Completar campos faltantes con valores por defecto
        for (Map.Entry<String, Field> entry : schema.entrySet()) {
            if (!record.hasField(entry.getKey())) {
                record.setValue(entry.getKey(), entry.getValue().getValorDefecto());
            }
        }
        records.add(record);
    }

    /**
     * Retorna todos los registros.
     */
    public List<Record> getRecords() {
        return records;
    }

    /**
     * Elimina todos los registros (instrucción clear).
     */
    public void clearRecords() {
        records.clear();
    }

    /**
     * Cuenta los registros.
     */
    public int countRecords() {
        return records.size();
    }

    // ===================== GETTERS =====================

    public String getNombre() {
        return nombre;
    }

    /**
     * Retorna los nombres de todos los campos en orden.
     */
    public List<String> getFieldNames() {
        return new ArrayList<>(schema.keySet());
    }

    @Override
    public String toString() {
        return "Table{nombre='" + nombre + "', campos=" + schema.keySet() +
               ", registros=" + records.size() + "}";
    }
}
