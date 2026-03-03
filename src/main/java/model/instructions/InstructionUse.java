package model.instructions;

import controller.ExecutionContext;

/**
 * Instrucción: use <nombreDatabase>;
 * Selecciona la base de datos activa.
 */
public class InstructionUse extends Instruction {
    private String nombreDatabase;

    public InstructionUse(String nombreDatabase, int linea, int columna) {
        super(linea, columna);
        this.nombreDatabase = nombreDatabase;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        boolean found = ctx.setActiveDatabase(nombreDatabase);
        if (!found) {
            ctx.addError("Semántico",
                "La base de datos '" + nombreDatabase + "' no ha sido definida.",
                linea, columna);
            ctx.printError("Base de datos '" + nombreDatabase + "' no encontrada.");
        } else {
            ctx.print(">> Usando base de datos: " + nombreDatabase);
        }
    }

    public String getNombreDatabase() { return nombreDatabase; }
}
