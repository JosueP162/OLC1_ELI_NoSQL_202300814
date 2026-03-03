package controller;


import model.instructions.Instruction;

import java.util.List;

/**
 * Intérprete del lenguaje ELI.
 * Recibe la lista de instrucciones generadas por el parser y las ejecuta
 * secuencialmente usando el ExecutionContext compartido.
 */
public class Interpreter {

    private ExecutionContext ctx;

    public Interpreter(ExecutionContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Ejecuta todas las instrucciones de la lista.
     * Continúa aunque haya errores (recuperación de errores).
     */
    public void ejecutar(List<Instruction> instrucciones) {
        if (instrucciones == null || instrucciones.isEmpty()) {
            ctx.print(">> No hay instrucciones para ejecutar.");
            return;
        }

        ctx.print(">> Iniciando ejecución: " + instrucciones.size() + " instrucción(es).");

        for (Instruction inst : instrucciones) {
            try {
                inst.ejecutar(ctx);
            } catch (Exception e) {
                ctx.addError("Runtime",
                    "Error inesperado en instrucción: " + e.getMessage(),
                    inst.getLinea(), inst.getColumna());
                ctx.printError("Error inesperado: " + e.getMessage());
                // Continuar con la siguiente instrucción
            }
        }

        ctx.print(">> Ejecución completada.");

        // Auto-persistencia: guardar todas las bases de datos que tengan ruta definida
        autoSave();
    }

    /**
     * Guarda automáticamente todas las bases de datos en sus archivos JSON.
     */
    private void autoSave() {
        for (model.database.Database db : ctx.getDatabases().values()) {
            try {
                PersistenceManager.save(db);
                ctx.print(">> Persistencia: base de datos '" + db.getNombre() + "' guardada en '" + db.getRutaPersistencia() + "'.");
            } catch (Exception e) {
                ctx.printError("No se pudo guardar '" + db.getNombre() + "': " + e.getMessage());
            }
        }
    }
}
