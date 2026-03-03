package model.instructions;

import controller.ExecutionContext;
import model.database.Database;

/**
 * Instrucción: database <nombre> { store at "ruta.json"; }
 * Define una nueva base de datos con su ruta de persistencia.
 */
public class InstructionDatabase extends Instruction {
    private String nombreDatabase;
    private String rutaPersistencia; // puede ser null si no se especificó store at

    public InstructionDatabase(String nombreDatabase, String rutaPersistencia, int linea, int columna) {
        super(linea, columna);
        this.nombreDatabase = nombreDatabase;
        this.rutaPersistencia = rutaPersistencia;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Si ya existe la base de datos, no la sobreescribimos (ya persistida)
        if (ctx.hasDatabase(nombreDatabase)) {
            ctx.print(">> Base de datos '" + nombreDatabase + "' ya existe en memoria.");
            return;
        }

        Database db;
        if (rutaPersistencia != null && !rutaPersistencia.isEmpty()) {
            db = new Database(nombreDatabase, rutaPersistencia);
        } else {
            db = new Database(nombreDatabase); // ruta por defecto: nombre.json
        }

        ctx.addDatabase(db);
        ctx.print(">> Base de datos '" + nombreDatabase + "' creada. Ruta: " + db.getRutaPersistencia());
    }

    public String getNombreDatabase() { return nombreDatabase; }
    public String getRutaPersistencia() { return rutaPersistencia; }
}
