package model.instructions;

import model.database.Record;

/**
 * Representa una expresión de filtrado del lenguaje ELI.
 * Puede ser:
 *  - Expresión relacional: campo OP valor  (ej: edad > 20)
 *  - Expresión lógica AND: expr1 && expr2
 *  - Expresión lógica OR:  expr1 || expr2
 *  - Expresión lógica NOT: !expr
 *
 * Implementa el Composite Pattern para construir árboles de expresiones.
 */
public abstract class FilterExpression {

    /**
     * Evalúa la expresión contra un registro dado.
     * @param record El registro a evaluar
     * @return true si el registro cumple la condición
     */
    public abstract boolean evaluar(Record record);

    // =====================================================================
    // EXPRESIÓN RELACIONAL: campo OP valor
    // =====================================================================
    public static class Relacional extends FilterExpression {
        private String campo;
        private String operador; // ==, !=, >, <, >=, <=
        private Object valor;

        public Relacional(String campo, String operador, Object valor) {
            this.campo = campo;
            this.operador = operador;
            this.valor = valor;
        }

        @Override
        public boolean evaluar(Record record) {
            Object campVal = record.getValue(campo);
            if (campVal == null && valor == null) return operador.equals("==");
            if (campVal == null || valor == null) return operador.equals("!=");

            // Comparación numérica
            if (campVal instanceof Number && valor instanceof Number) {
                double a = ((Number) campVal).doubleValue();
                double b = ((Number) valor).doubleValue();
                switch (operador) {
                    case "==": return a == b;
                    case "!=": return a != b;
                    case ">":  return a > b;
                    case "<":  return a < b;
                    case ">=": return a >= b;
                    case "<=": return a <= b;
                }
            }

            // Comparación de strings
            if (campVal instanceof String && valor instanceof String) {
                int cmp = ((String) campVal).compareTo((String) valor);
                switch (operador) {
                    case "==": return cmp == 0;
                    case "!=": return cmp != 0;
                    case ">":  return cmp > 0;
                    case "<":  return cmp < 0;
                    case ">=": return cmp >= 0;
                    case "<=": return cmp <= 0;
                }
            }

            // Comparación booleana
            if (campVal instanceof Boolean && valor instanceof Boolean) {
                boolean a = (Boolean) campVal;
                boolean b = (Boolean) valor;
                switch (operador) {
                    case "==": return a == b;
                    case "!=": return a != b;
                }
            }

            // Fallback: toString comparison para ==
            switch (operador) {
                case "==": return campVal.equals(valor);
                case "!=": return !campVal.equals(valor);
            }

            return false;
        }

        @Override
        public String toString() {
            return campo + " " + operador + " " + valor;
        }
    }

    // =====================================================================
    // EXPRESIÓN AND: expr1 && expr2
    // =====================================================================
    public static class And extends FilterExpression {
        private FilterExpression izq;
        private FilterExpression der;

        public And(FilterExpression izq, FilterExpression der) {
            this.izq = izq;
            this.der = der;
        }

        @Override
        public boolean evaluar(Record record) {
            return izq.evaluar(record) && der.evaluar(record);
        }

        @Override
        public String toString() {
            return "(" + izq + " && " + der + ")";
        }
    }

    // =====================================================================
    // EXPRESIÓN OR: expr1 || expr2
    // =====================================================================
    public static class Or extends FilterExpression {
        private FilterExpression izq;
        private FilterExpression der;

        public Or(FilterExpression izq, FilterExpression der) {
            this.izq = izq;
            this.der = der;
        }

        @Override
        public boolean evaluar(Record record) {
            return izq.evaluar(record) || der.evaluar(record);
        }

        @Override
        public String toString() {
            return "(" + izq + " || " + der + ")";
        }
    }

    // =====================================================================
    // EXPRESIÓN NOT: !expr
    // =====================================================================
    public static class Not extends FilterExpression {
        private FilterExpression expr;

        public Not(FilterExpression expr) {
            this.expr = expr;
        }

        @Override
        public boolean evaluar(Record record) {
            return !expr.evaluar(record);
        }

        @Override
        public String toString() {
            return "!(" + expr + ")";
        }
    }
}
