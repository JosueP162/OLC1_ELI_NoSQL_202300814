package model.instructions;

import controller.ExecutionContext;
import model.database.Database;
import model.database.Field;
import model.database.Table;

import java.util.List;

/**
 * Instrucción: table <nombre> { campo: tipo; ... }
 * Define una nueva tabla en la base de datos activa.
 */
public class InstructionTable extends Instruction {
    private String nombreTabla;
    private List<Field> campos;

    public InstructionTable(String nombreTabla, List<Field> campos, int linea, int columna) {
        super(linea, columna);
        this.nombreTabla = nombreTabla;
        this.campos = campos;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Verificar que haya una base de datos activa
        if (!ctx.hasActiveDatabase()) {
            ctx.addError("Semántico",
                "No hay base de datos activa. Use 'use <database>;' antes de crear tablas.",
                linea, columna);
            ctx.printError("No hay base de datos activa para la tabla '" + nombreTabla + "'.");
            return;
        }

        Database db = ctx.getActiveDatabase();

        // Si la tabla ya existe, no la sobreescribimos
        if (db.hasTable(nombreTabla)) {
            ctx.print(">> Tabla '" + nombreTabla + "' ya existe en '" + db.getNombre() + "'.");
            return;
        }

        Table table = new Table(nombreTabla);
        for (Field f : campos) {
            table.addField(f);
        }

        db.addTable(table);
        ctx.print(">> Tabla '" + nombreTabla + "' creada en base de datos '" + db.getNombre() + "'.");
    }

    public String getNombreTabla() { return nombreTabla; }
    public List<Field> getCampos() { return campos; }
}
