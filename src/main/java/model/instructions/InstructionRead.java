package model.instructions;

import controller.ExecutionContext;
import model.database.Record;
import model.database.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Instrucción: read <tabla> { fields: ...; filter: ...; };
 * Consulta registros de una tabla con filtros opcionales.
 */
public class InstructionRead extends Instruction {
    private String nombreTabla;
    private List<String> fields;      // null o lista vacía = todos (*)
    private boolean allFields;        // true si fields: *
    private FilterExpression filter;  // null si no hay filtro

    public InstructionRead(String nombreTabla, List<String> fields, boolean allFields,
                           FilterExpression filter, int linea, int columna) {
        super(linea, columna);
        this.nombreTabla = nombreTabla;
        this.fields = fields;
        this.allFields = allFields;
        this.filter = filter;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {
        // Verificar base de datos activa
        if (!ctx.hasActiveDatabase()) {
            ctx.addError("Semántico", "No hay base de datos activa para READ.", linea, columna);
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

        // Determinar los campos a mostrar
        List<String> camposAMostrar;
        if (allFields || fields == null || fields.isEmpty()) {
            camposAMostrar = table.getFieldNames();
        } else {
            // Validar que los campos existen
            camposAMostrar = new ArrayList<>();
            for (String campo : fields) {
                if (!table.hasField(campo)) {
                    ctx.addError("Semántico",
                    "El campo '" + campo + "' no existe en la tabla '" + nombreTabla + "'.",
                     linea, columna);
                     // ← NO hacer return, solo saltar este campo
                    continue;
                }
            camposAMostrar.add(campo);
            }

            // Si no quedó ningún campo válido, usar todos
            if (camposAMostrar.isEmpty()) {
            camposAMostrar = table.getFieldNames();
            }
        }

        // Filtrar registros
        List<Record> resultado = new ArrayList<>();
        for (Record record : table.getRecords()) {
            if (filter == null || filter.evaluar(record)) {
                resultado.add(record);
            }
        }

        // Guardar resultado para posible EXPORT
        ctx.setLastQueryResult(nombreTabla, camposAMostrar, resultado);

        // Mensaje breve en la consola
        ctx.print(">> READ '" + nombreTabla + "': " + resultado.size() + " registro(s).");
        ctx.printQueryResult(nombreTabla, resultado.size(), buildTable(camposAMostrar, resultado));
        
    }

    /**
     * Construye una representación en texto tipo tabla para la consola.
     */
    private String buildTable(List<String> campos, List<Record> records) {
        if (records.isEmpty()) {
            return "(Sin resultados)";
        }

        // Calcular anchos de columnas
        int[] anchos = new int[campos.size()];
        for (int i = 0; i < campos.size(); i++) {
            anchos[i] = campos.get(i).length();
        }
        for (Record r : records) {
            for (int i = 0; i < campos.size(); i++) {
                Object val = r.getValue(campos.get(i));
                String s = val == null ? "null" : val.toString();
                if (s.length() > anchos[i]) anchos[i] = s.length();
            }
        }

        StringBuilder sb = new StringBuilder();

        // Encabezado
        sb.append(buildSeparator(anchos)).append("\n");
        sb.append("| ");
        for (int i = 0; i < campos.size(); i++) {
            sb.append(padRight(campos.get(i), anchos[i])).append(" | ");
        }
        sb.append("\n");
        sb.append(buildSeparator(anchos)).append("\n");

        // Filas
        for (Record r : records) {
            sb.append("| ");
            for (int i = 0; i < campos.size(); i++) {
                Object val = r.getValue(campos.get(i));
                String s = val == null ? "null" : val.toString();
                sb.append(padRight(s, anchos[i])).append(" | ");
            }
            sb.append("\n");
        }
        sb.append(buildSeparator(anchos));

        return sb.toString();
    }

    private String buildSeparator(int[] anchos) {
        StringBuilder sb = new StringBuilder("+");
        for (int ancho : anchos) {
            sb.append("-".repeat(ancho + 2)).append("+");
        }
        return sb.toString();
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public String getNombreTabla() { return nombreTabla; }
    public List<String> getFields() { return fields; }
    public boolean isAllFields() { return allFields; }
    public FilterExpression getFilter() { return filter; }
}