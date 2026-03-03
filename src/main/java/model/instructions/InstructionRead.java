package model.instructions;

import controller.ExecutionContext;
import model.database.Record;
import model.database.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Instruccion: read <tabla> { fields: ...; filter: ...; };
 *
 * CAMBIO: el resultado (tabla ASCII) se envia a ctx.printQueryResult()
 * para mostrarse en el panel Output de la UI, separado de la consola.
 */
public class InstructionRead extends Instruction {

    private String            nombreTabla;
    private List<String>      fields;
    private boolean           allFields;
    private FilterExpression  filter;

    public InstructionRead(String nombreTabla, List<String> fields, boolean allFields,
                           FilterExpression filter, int linea, int columna) {
        super(linea, columna);
        this.nombreTabla = nombreTabla;
        this.fields      = fields;
        this.allFields   = allFields;
        this.filter      = filter;
    }

    @Override
    public void ejecutar(ExecutionContext ctx) {

        if (!ctx.hasActiveDatabase()) {
            ctx.addError("Semantico", "No hay base de datos activa para READ.", linea, columna);
            ctx.printError("No hay base de datos activa.");
            return;
        }

        if (!ctx.hasActiveTable(nombreTabla)) {
            ctx.addError("Semantico",
                "La tabla '" + nombreTabla + "' no existe en la base de datos activa.",
                linea, columna);
            ctx.printError("Tabla '" + nombreTabla + "' no encontrada.");
            return;
        }

        Table table = ctx.getActiveTable(nombreTabla);

        // Determinar campos a mostrar
        List<String> camposAMostrar;
        if (allFields || fields == null || fields.isEmpty()) {
            camposAMostrar = table.getFieldNames();
        } else {
            camposAMostrar = new ArrayList<>();
            for (String campo : fields) {
                if (!table.hasField(campo)) {
                    ctx.addError("Semantico",
                        "El campo '" + campo + "' no existe en la tabla '" + nombreTabla + "'.",
                        linea, columna);
                    ctx.printError("Campo '" + campo + "' no encontrado.");
                    return;
                }
                camposAMostrar.add(campo);
            }
        }

        // Filtrar registros
        List<Record> resultado = new ArrayList<>();
        for (Record record : table.getRecords()) {
            if (filter == null || filter.evaluar(record)) {
                resultado.add(record);
            }
        }

        // Guardar resultado para posible EXPORT posterior
        ctx.setLastQueryResult(nombreTabla, camposAMostrar, resultado);

        // Mensaje breve en la consola
        ctx.print(">> READ '" + nombreTabla + "': " + resultado.size() + " registro(s) encontrado(s).");

        // Tabla ASCII -> panel Output de la UI (NO en la consola)
        String tablaAscii = buildTable(camposAMostrar, resultado);
        ctx.printQueryResult(nombreTabla, resultado.size(), tablaAscii);
    }

    /**
     * Construye una representacion en texto tipo tabla ASCII.
     */
    private String buildTable(List<String> campos, List<Record> records) {
        if (records.isEmpty()) {
            return "  (Sin resultados)";
        }

        // Calcular anchos de columnas
        int[] anchos = new int[campos.size()];
        for (int i = 0; i < campos.size(); i++) {
            anchos[i] = campos.get(i).length();
        }
        for (Record r : records) {
            for (int i = 0; i < campos.size(); i++) {
                Object val = r.getValue(campos.get(i));
                String s   = val == null ? "null" : val.toString();
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
                String s   = val == null ? "null" : val.toString();
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

    public String           getNombreTabla() { return nombreTabla; }
    public List<String>     getFields()      { return fields; }
    public boolean          isAllFields()    { return allFields; }
    public FilterExpression getFilter()      { return filter; }
}