package model.instructions;

import controller.ExecutionContext;

/**
 * Clase abstracta base para todas las instrucciones del lenguaje ELI.
 * Cada instrucción concreta debe implementar el método ejecutar().
 * 
 * El patrón usado es Command Pattern: cada instrucción sabe cómo ejecutarse
 * a sí misma dado un contexto de ejecución.
 */
public abstract class Instruction {
    protected int linea;
    protected int columna;

    public Instruction(int linea, int columna) {
        this.linea = linea;
        this.columna = columna;
    }

    /**
     * Ejecuta la instrucción dentro del contexto dado.
     * @param ctx Contexto de ejecución (contiene bases de datos, tablas, etc.)
     */
    public abstract void ejecutar(ExecutionContext ctx);

    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
}
