package controller;

import model.database.Database;
import model.database.Field;
import model.database.Record;
import model.database.Table;

import java.io.*;
import java.util.*;

/**
 * Maneja la persistencia de bases de datos ELI en formato JSON.
 * Lee y escribe el archivo JSON de persistencia.
 * 
 * No usa librerías externas JSON para no agregar dependencias.
 * Implementa un parser JSON mínimo suficiente para el formato de ELI.
 */
public class PersistenceManager {

    /**
     * Guarda una base de datos en su archivo JSON de persistencia.
     */
    public static void save(Database db) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"database\": \"").append(db.getNombre()).append("\",\n");
        json.append("  \"tables\": {\n");

        List<String> tableNames = new ArrayList<>(db.getTables().keySet());
        for (int ti = 0; ti < tableNames.size(); ti++) {
            String tableName = tableNames.get(ti);
            Table table = db.getTable(tableName);

            json.append("    \"").append(tableName).append("\": {\n");

            // Schema
            json.append("      \"schema\": {\n");
            List<String> fieldNames = table.getFieldNames();
            for (int fi = 0; fi < fieldNames.size(); fi++) {
                String fname = fieldNames.get(fi);
                String ftype = table.getField(fname).getTipo();
                json.append("        \"").append(fname).append("\": \"").append(ftype).append("\"");
                if (fi < fieldNames.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("      },\n");

            // Records
            json.append("      \"records\": [\n");
            List<Record> records = table.getRecords();
            for (int ri = 0; ri < records.size(); ri++) {
                Record record = records.get(ri);
                json.append("        {\n");
                for (int fi = 0; fi < fieldNames.size(); fi++) {
                    String fname = fieldNames.get(fi);
                    Object val = record.getValue(fname);
                    json.append("          \"").append(fname).append("\": ").append(toJsonValue(val));
                    if (fi < fieldNames.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("        }");
                if (ri < records.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("      ]\n");
            json.append("    }");
            if (ti < tableNames.size() - 1) json.append(",");
            json.append("\n");
        }

        json.append("  }\n");
        json.append("}");

        try (FileWriter fw = new FileWriter(db.getRutaPersistencia())) {
            fw.write(json.toString());
        }
    }

    /**
     * Carga una base de datos desde su archivo JSON.
     * Retorna null si el archivo no existe.
     */
    public static Database load(String rutaArchivo) throws IOException {
        File file = new File(rutaArchivo);
        if (!file.exists()) return null;

        // Leer el archivo completo
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        String content = sb.toString().trim();
        return parseDatabase(content, rutaArchivo);
    }

    // =====================================================================
    // PARSER JSON MÍNIMO
    // =====================================================================

    private static Database parseDatabase(String json, String rutaArchivo) {
        // Extraer nombre de la base de datos
        String dbName = extractStringValue(json, "database");
        if (dbName == null) return null;

        Database db = new Database(dbName, rutaArchivo);

        // Extraer bloque de tables
        String tablesBlock = extractBlock(json, "tables");
        if (tablesBlock == null) return db;

        // Parsear cada tabla
        parseTablesBlock(tablesBlock, db);

        return db;
    }

    private static void parseTablesBlock(String block, Database db) {
        // Encontrar nombres de tablas (claves del objeto)
        int pos = 0;
        while (pos < block.length()) {
            // Buscar siguiente clave (nombre de tabla)
            int keyStart = block.indexOf("\"", pos);
            if (keyStart == -1) break;
            int keyEnd = block.indexOf("\"", keyStart + 1);
            if (keyEnd == -1) break;

            String tableName = block.substring(keyStart + 1, keyEnd);
            pos = keyEnd + 1;

            // Encontrar el bloque { } de esta tabla
            int braceStart = block.indexOf("{", pos);
            if (braceStart == -1) break;
            int braceEnd = findMatchingBrace(block, braceStart);
            if (braceEnd == -1) break;

            String tableBlock = block.substring(braceStart + 1, braceEnd);
            Table table = parseTable(tableName, tableBlock);
            if (table != null) {
                db.addTable(table);
            }

            pos = braceEnd + 1;
        }
    }

    private static Table parseTable(String tableName, String block) {
        Table table = new Table(tableName);

        // Parsear schema
        String schemaBlock = extractBlock(block, "schema");
        if (schemaBlock != null) {
            parseSchema(schemaBlock, table);
        }

        // Parsear records
        String recordsArray = extractArray(block, "records");
        if (recordsArray != null) {
            parseRecords(recordsArray, table);
        }

        return table;
    }

    private static void parseSchema(String schemaBlock, Table table) {
        int pos = 0;
        while (pos < schemaBlock.length()) {
            int keyStart = schemaBlock.indexOf("\"", pos);
            if (keyStart == -1) break;
            int keyEnd = schemaBlock.indexOf("\"", keyStart + 1);
            if (keyEnd == -1) break;
            String fieldName = schemaBlock.substring(keyStart + 1, keyEnd);
            pos = keyEnd + 1;

            int colonPos = schemaBlock.indexOf(":", pos);
            if (colonPos == -1) break;
            pos = colonPos + 1;

            int valStart = schemaBlock.indexOf("\"", pos);
            if (valStart == -1) break;
            int valEnd = schemaBlock.indexOf("\"", valStart + 1);
            if (valEnd == -1) break;
            String fieldType = schemaBlock.substring(valStart + 1, valEnd);
            pos = valEnd + 1;

            table.addField(new Field(fieldName, fieldType));
        }
    }

    private static void parseRecords(String arrayContent, Table table) {
        // Encontrar cada { } objeto
        int pos = 0;
        while (pos < arrayContent.length()) {
            int braceStart = arrayContent.indexOf("{", pos);
            if (braceStart == -1) break;
            int braceEnd = findMatchingBrace(arrayContent, braceStart);
            if (braceEnd == -1) break;

            String recordBlock = arrayContent.substring(braceStart + 1, braceEnd);
            Record record = parseRecord(recordBlock, table);
            if (record != null) {
                table.getRecords().add(record); // Agregar directamente para evitar doble default
            }

            pos = braceEnd + 1;
        }
    }

    private static Record parseRecord(String block, Table table) {
        Record record = new Record();
        int pos = 0;

        while (pos < block.length()) {
            int keyStart = block.indexOf("\"", pos);
            if (keyStart == -1) break;
            int keyEnd = block.indexOf("\"", keyStart + 1);
            if (keyEnd == -1) break;
            String fieldName = block.substring(keyStart + 1, keyEnd);
            pos = keyEnd + 1;

            int colonPos = block.indexOf(":", pos);
            if (colonPos == -1) break;
            pos = colonPos + 1;

            // Saltar espacios
            while (pos < block.length() && Character.isWhitespace(block.charAt(pos))) pos++;

            if (pos >= block.length()) break;
            char next = block.charAt(pos);

            Object valor;
            if (next == '"') {
                // String
                int valEnd = block.indexOf("\"", pos + 1);
                valor = block.substring(pos + 1, valEnd);
                pos = valEnd + 1;
            } else if (next == 't' || next == 'f') {
                // Boolean
                if (block.startsWith("true", pos)) {
                    valor = true;
                    pos += 4;
                } else {
                    valor = false;
                    pos += 5;
                }
            } else if (next == 'n') {
                // null
                valor = null;
                pos += 4;
            } else if (next == '[') {
                // Array - simplificado
                int arrEnd = block.indexOf("]", pos);
                valor = new ArrayList<>(); // simplificado
                pos = arrEnd + 1;
            } else {
                // Número
                int numEnd = pos;
                while (numEnd < block.length() && (Character.isDigit(block.charAt(numEnd)) ||
                       block.charAt(numEnd) == '.' || block.charAt(numEnd) == '-')) {
                    numEnd++;
                }
                String numStr = block.substring(pos, numEnd).trim();
                try {
                    if (numStr.contains(".")) {
                        valor = Double.parseDouble(numStr);
                    } else {
                        valor = Integer.parseInt(numStr);
                    }
                } catch (NumberFormatException e) {
                    valor = 0;
                }
                pos = numEnd;
            }

            record.setValue(fieldName, valor);
        }

        return record;
    }

    // =====================================================================
    // HELPERS JSON
    // =====================================================================

    private static String extractStringValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return null;
        int q1 = json.indexOf("\"", colon + 1);
        if (q1 == -1) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 == -1) return null;
        return json.substring(q1 + 1, q2);
    }

    private static String extractBlock(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return null;
        int braceStart = json.indexOf("{", colon);
        if (braceStart == -1) return null;
        int braceEnd = findMatchingBrace(json, braceStart);
        if (braceEnd == -1) return null;
        return json.substring(braceStart + 1, braceEnd);
    }

    private static String extractArray(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return null;
        int arrStart = json.indexOf("[", colon);
        if (arrStart == -1) return null;
        int arrEnd = findMatchingBracket(json, arrStart);
        if (arrEnd == -1) return null;
        return json.substring(arrStart + 1, arrEnd);
    }

    private static int findMatchingBrace(String s, int openPos) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int findMatchingBracket(String s, int openPos) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            if (s.charAt(i) == '[') depth++;
            else if (s.charAt(i) == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String toJsonValue(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof String) return "\"" + ((String) valor).replace("\"", "\\\"") + "\"";
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
        return "\"" + valor + "\"";
    }
}
