package model.instructions;

import controller.ExecutionContext;
import model.database.Field;
import model.database.Record;
import model.database.Table;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instrucción: add <tabla> { campo: valor, ... };
 * Inserta un nuevo registro en la tabla.
 */
public class InstructionAdd extends Instruction {
    private String nombreTabla;
    private LinkedHashMap<String, Object> valores;

    public InstructionAdd(String nombreTabla, LinkedHashMap<String, Object> valores, int linea, int columna) {
        super(linea, columna);
        this.nombreTabla = nombreTabla;
        this.valores = valores;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Verificar base de datos activa
        if (!ctx.hasActiveDatabase()) {
            ctx.addError("Semántico",
                "No hay base de datos activa para ejecutar ADD.",
                linea, columna);
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

        // Validar que los campos existen en el esquema y los tipos son correctos
        for (Map.Entry<String, Object> entry : valores.entrySet()) {
            String campo = entry.getKey();
            Object valor = entry.getValue();

            if (!table.hasField(campo)) {
                ctx.addError("Semántico",
                    "El campo '" + campo + "' no existe en la tabla '" + nombreTabla + "'.",
                    linea, columna);
                ctx.printError("Campo '" + campo + "' no encontrado en tabla '" + nombreTabla + "'.");
                return;
            }

            Field field = table.getField(campo);
            if (!field.validarTipo(valor)) {
                ctx.addError("Semántico",
                    "Tipo incorrecto para el campo '" + campo + "'. Esperado: " + field.getTipo(),
                    linea, columna);
                ctx.printError("Tipo incorrecto para '" + campo + "'.");
                return;
            }
        }

        // Crear y agregar el registro
        Record record = new Record(valores);
        table.addRecord(record);
        ctx.print(">> Registro insertado en tabla '" + nombreTabla + "': " + record.toString());
    }

    public String getNombreTabla() { return nombreTabla; }
    public LinkedHashMap<String, Object> getValores() { return valores; }
}
