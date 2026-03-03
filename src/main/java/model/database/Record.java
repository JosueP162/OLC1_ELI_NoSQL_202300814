package model.database;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa un registro (fila) dentro de una tabla ELI.
 * Usa LinkedHashMap para mantener el orden de inserción de los campos.
 */
public class Record {
    private LinkedHashMap<String, Object> valores;

    public Record() {
        this.valores = new LinkedHashMap<>();
    }

    public Record(LinkedHashMap<String, Object> valores) {
        this.valores = new LinkedHashMap<>(valores);
    }

    /**
     * Establece el valor de un campo.
     */
    public void setValue(String campo, Object valor) {
        valores.put(campo, valor);
    }

    /**
     * Obtiene el valor de un campo.
     */
    public Object getValue(String campo) {
        return valores.get(campo);
    }

    /**
     * Verifica si el registro contiene un campo.
     */
    public boolean hasField(String campo) {
        return valores.containsKey(campo);
    }

    /**
     * Retorna todos los pares campo-valor.
     */
    public Map<String, Object> getValores() {
        return valores;
    }

    /**
     * Crea una copia del registro.
     */
    public Record copia() {
        return new Record(new LinkedHashMap<>(valores));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : valores.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
