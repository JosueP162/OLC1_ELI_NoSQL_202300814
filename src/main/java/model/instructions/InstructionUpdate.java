package model.instructions;

import controller.ExecutionContext;
import model.database.Field;
import model.database.Record;
import model.database.Table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Instrucción: update <tabla> { set: campo=valor,...; filter: expr; };
 * Modifica registros que cumplan el filtro (o todos si no hay filtro).
 */
public class InstructionUpdate extends Instruction {
    private String nombreTabla;
    private LinkedHashMap<String, Object> setValues; // campo -> nuevo valor
    private FilterExpression filter; // null = actualizar todos

    public InstructionUpdate(String nombreTabla, LinkedHashMap<String, Object> setValues,
                             FilterExpression filter, int linea, int columna) {
        super(linea, columna);
        this.nombreTabla = nombreTabla;
        this.setValues = setValues;
        this.filter = filter;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Verificar base de datos activa
        if (!ctx.hasActiveDatabase()) {
            ctx.addError("Semántico", "No hay base de datos activa para UPDATE.", linea, columna);
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

        // Validar campos del SET
        for (Map.Entry<String, Object> entry : setValues.entrySet()) {
            if (!table.hasField(entry.getKey())) {
                ctx.addError("Semántico",
                    "El campo '" + entry.getKey() + "' no existe en la tabla '" + nombreTabla + "'.",
                    linea, columna);
                ctx.printError("Campo '" + entry.getKey() + "' no encontrado.");
                return;
            }
            Field field = table.getField(entry.getKey());
            if (!field.validarTipo(entry.getValue())) {
                ctx.addError("Semántico",
                    "Tipo incorrecto para el campo '" + entry.getKey() + "'.",
                    linea, columna);
                return;
            }
        }

        // Aplicar actualización
        List<Record> records = table.getRecords();
        int actualizados = 0;

        for (Record record : records) {
            if (filter == null || filter.evaluar(record)) {
                for (Map.Entry<String, Object> entry : setValues.entrySet()) {
                    record.setValue(entry.getKey(), entry.getValue());
                }
                actualizados++;
            }
        }

        ctx.print(">> UPDATE en tabla '" + nombreTabla + "': " + actualizados + " registro(s) actualizado(s).");
    }

    public String getNombreTabla() { return nombreTabla; }
    public LinkedHashMap<String, Object> getSetValues() { return setValues; }
    public FilterExpression getFilter() { return filter; }
}
