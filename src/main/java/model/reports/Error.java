package model.reports;

/**
 * Representa un Error encontrado durante el análisis léxico o sintáctico.
 */
public class Error {
    private int numero;
    private String tipo;
    private String descripcion;
    private int linea;
    private int columna;

    public Error(int numero, String tipo, String descripcion, int linea, int columna) {
        this.numero = numero;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.linea = linea;
        this.columna = columna;
    }

    public int getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    @Override
    public String toString() {
        return "[Error #" + numero + " | Tipo: " + tipo + " | Descripción: " + descripcion +
               " | Línea: " + linea + " | Columna: " + columna + "]";
    }
}
