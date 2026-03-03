package view;

import model.reports.Token;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;

/**
 * TableModel para mostrar la lista de Tokens en un JTable.
 */
public class TokenTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"#", "Lexema", "Tipo", "Línea", "Columna"};
    private List<Token> tokens;

    public TokenTableModel() {
        this.tokens = new ArrayList<>();
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return tokens.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMNAS[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        Token t = tokens.get(row);
        switch (col) {
            case 0: return t.getNumero();
            case 1: return t.getLexema();
            case 2: return t.getTipo();
            case 3: return t.getLinea();
            case 4: return t.getColumna();
            default: return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (col == 0 || col == 3 || col == 4) return Integer.class;
        return String.class;
    }
}
