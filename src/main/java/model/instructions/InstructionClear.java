package model.instructions;

import controller.ExecutionContext;
import model.database.Table;

/**
 * Instrucción: clear <tabla>;
 * Elimina todos los registros de una tabla sin borrar su estructura.
 */
public class InstructionClear extends Instruction {
    private String nombreTabla;

    public InstructionClear(String nombreTabla, int linea, int columna) {
        super(linea, columna);
        this.nombreTabla = nombreTabla;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Verificar base de datos activa
        if (!ctx.hasActiveDatabase()) {
            ctx.addError("Semántico", "No hay base de datos activa para CLEAR.", linea, columna);
            ctx.printError("No hay base de datos activa.");
            return;
        }

        // Verificar que la tabla existe
        if (!ctx.hasActiveTable(nombreTabla)) {
            ctx.addError("Semántico",
                "La tabla '" + nombreTabla + "' no existe en la base de datos activa.",
                linea, columna);
            ctx.printError("Tabla '" + nombreTabla + "' no encontrada.");
            return;
        }

        Table table = ctx.getActiveTable(nombreTabla);
        int cantidadAntes = table.countRecords();
        table.clearRecords();

        ctx.print(">> CLEAR tabla '" + nombreTabla + "': " + cantidadAntes + " registro(s) eliminado(s). La estructura se mantiene.");
    }

    public String getNombreTabla() { return nombreTabla; }
}
