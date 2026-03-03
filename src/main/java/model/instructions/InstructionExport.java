package model.instructions;

import controller.ExecutionContext;
import model.database.Field;
import model.database.Record;
import model.database.Table;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Instrucción: export "archivo.json";
 * Guarda el resultado de la última consulta READ en un archivo JSON.
 */
public class InstructionExport extends Instruction {
    private String nombreArchivo;

    public InstructionExport(String nombreArchivo, int linea, int columna) {
        super(linea, columna);
        this.nombreArchivo = nombreArchivo;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Verificar que exista una consulta previa
        if (!ctx.hasLastQuery()) {
            ctx.addError("Semántico",
                "No existe una consulta previa. EXPORT debe ejecutarse después de un READ válido.",
                linea, columna);
            ctx.printError("No hay consulta previa para exportar.");
            return;
        }

        String tableName = ctx.getLastQueryTable();
        List<String> fields = ctx.getLastQueryFields();
        List<Record> records = ctx.getLastQueryResult();

        // Obtener tipos de campos desde la tabla
        Table table = null;
        if (ctx.hasActiveDatabase()) {
            table = ctx.getActiveTable(tableName);
        }

        // Construir JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"table\": \"").append(tableName).append("\",\n");

        // Fields con tipos
        json.append("  \"fields\": {\n");
        for (int i = 0; i < fields.size(); i++) {
            String campo = fields.get(i);
            String tipo = "string"; // por defecto
            if (table != null && table.hasField(campo)) {
                tipo = table.getField(campo).getTipo();
            }
            json.append("    \"").append(campo).append("\": \"").append(tipo).append("\"");
            if (i < fields.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  },\n");

        // Records
        json.append("  \"records\": [\n");
        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            json.append("    {\n");
            for (int j = 0; j < fields.size(); j++) {
                String campo = fields.get(j);
                Object valor = record.getValue(campo);
                json.append("      \"").append(campo).append("\": ");
                json.append(toJsonValue(valor));
                if (j < fields.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    }");
            if (i < records.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}");

        // Escribir archivo
        try (FileWriter fw = new FileWriter(nombreArchivo)) {
            fw.write(json.toString());
            ctx.print(">> EXPORT exitoso: " + records.size() + " registro(s) exportado(s) a '" + nombreArchivo + "'.");
        } catch (IOException e) {
            ctx.addError("Semántico",
                "No se pudo escribir el archivo '" + nombreArchivo + "': " + e.getMessage(),
                linea, columna);
            ctx.printError("Error al exportar: " + e.getMessage());
        }
    }

    /**
     * Convierte un valor Java a su representación JSON.
     */
    private String toJsonValue(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof String) return "\"" + escapeJson((String) valor) + "\"";
        if (valor instanceof Boolean) return valor.toString();
        if (valor instanceof Number) return valor.toString();
        if (valor instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) valor;
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJsonValue(list.get(i)));
                if (i < list.size() - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
        if (valor instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) valor;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(", ");
                sb.append("\"").append(entry.getKey()).append("\": ").append(toJsonValue(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        return "\"" + escapeJson(valor.toString()) + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public String getNombreArchivo() { return nombreArchivo; }
}
