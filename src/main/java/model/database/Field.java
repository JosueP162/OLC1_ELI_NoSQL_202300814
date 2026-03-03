package model.database;

/**
 * Representa un campo (columna) dentro de una tabla ELI.
 */
public class Field {
    private String nombre;
    private String tipo;

    public Field(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo.toLowerCase();
    }

    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }

    /**
     * Valida si un valor es compatible con el tipo de este campo.
     */
    public boolean validarTipo(Object valor) {
        if (valor == null) return true; // null es válido para cualquier tipo
        switch (tipo) {
            case "int":
                return valor instanceof Integer || valor instanceof Long;
            case "float":
                return valor instanceof Double || valor instanceof Float;
            case "bool":
                return valor instanceof Boolean;
            case "string":
                return valor instanceof String;
            case "array":
                return valor instanceof java.util.List;
            case "object":
                return valor instanceof java.util.Map;
            case "null":
                return valor == null;
            default:
                return true;
        }
    }

    /**
     * Retorna el valor por defecto según el tipo.
     */
    public Object getValorDefecto() {
        switch (tipo) {
            case "int":    return 0;
            case "float":  return 0.0;
            case "bool":   return false;
            case "string": return "";
            case "array":  return new java.util.ArrayList<>();
            case "object": return new java.util.HashMap<>();
            case "null":   return null;
            default:       return null;
        }
    }

    @Override
    public String toString() {
        return nombre + ": " + tipo;
    }
}
