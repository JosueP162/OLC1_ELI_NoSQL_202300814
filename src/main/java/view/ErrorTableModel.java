package view;

import model.reports.Error;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;

/**
 * TableModel para mostrar la lista de Errores en un JTable.
 */
public class ErrorTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"#", "Tipo", "Descripción", "Línea", "Columna"};
    private List<Error> errors;

    public ErrorTableModel() {
        this.errors = new ArrayList<>();
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return errors.size();
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
        Error e = errors.get(row);
        switch (col) {
            case 0: return e.getNumero();
            case 1: return e.getTipo();
            case 2: return e.getDescripcion();
            case 3: return e.getLinea();
            case 4: return e.getColumna();
            default: return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (col == 0 || col == 3 || col == 4) return Integer.class;
        return String.class;
    }
}
