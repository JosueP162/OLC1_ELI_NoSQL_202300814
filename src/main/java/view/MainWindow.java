package view;

import controller.MainController;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * Ventana principal de ELI-NOSQL.
 * Layout:
 *  - MenuBar: Archivo | Reportes | Ejecutar
 *  - Panel izquierdo: Editor de código (con pestañas para múltiples archivos)
 *  - Panel derecho: Output (resultado de consultas)
 *  - Panel inferior: Consola (mensajes del intérprete)
 *  - Panel reporte (modal): Errores o Tokens (se muestra en diálogo)
 */
public class MainWindow extends JFrame {

    // ===================== CONTROLADOR =====================
    private MainController controller;

    // ===================== COMPONENTES UI =====================
    // Editor con pestañas
    private JTabbedPane editorTabs;

    // Área de output (resultados de consultas)
    private JTextArea outputArea;

    // Consola
    private JTextArea consoleArea;

    // Modelos de tabla para reportes
    private TokenTableModel tokenTableModel;
    private ErrorTableModel errorTableModel;

    // Barra de estado
    private JLabel statusLabel;

    // Archivo activo por pestaña
    private java.util.Map<Integer, File> tabFiles = new java.util.HashMap<>();

    // ===================== COLORES =====================
    private static final Color COLOR_BG         = new Color(30, 30, 30);
    private static final Color COLOR_EDITOR_BG  = new Color(40, 44, 52);
    private static final Color COLOR_EDITOR_FG  = new Color(220, 220, 220);
    private static final Color COLOR_CONSOLE_BG = new Color(20, 20, 20);
    private static final Color COLOR_CONSOLE_FG = new Color(0, 255, 100);
    private static final Color COLOR_ACCENT     = new Color(97, 175, 239);
    private static final Color COLOR_ERROR      = new Color(224, 108, 117);
    private static final Color COLOR_SUCCESS    = new Color(152, 195, 121);
    private static final Font  FONT_CODE        = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font  FONT_UI          = new Font("Segoe UI", Font.PLAIN, 12);

    public MainWindow() {
        controller = new MainController();
        tokenTableModel = new TokenTableModel();
        errorTableModel = new ErrorTableModel();
        initUI();
        setVisible(true);
    }

    // =====================================================================
    // INICIALIZACIÓN UI
    // =====================================================================

    private void initUI() {
        setTitle("ELI-NOSQL IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        // Icono (si existe)
        // setIconImage(...)

        // Menú
        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);

        // Layout principal
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(COLOR_BG);

        // Panel central: editor + output lado a lado
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildEditorPanel(),
                buildOutputPanel());
        centerSplit.setDividerLocation(650);
        centerSplit.setResizeWeight(0.6);
        centerSplit.setBorder(null);
        centerSplit.setBackground(COLOR_BG);

        // Panel inferior: consola
        JPanel bottomPanel = buildConsolePanel();
        bottomPanel.setPreferredSize(new Dimension(0, 180));

        // Split vertical: editor/output arriba, consola abajo
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                centerSplit, bottomPanel);
        mainSplit.setDividerLocation(520);
        mainSplit.setResizeWeight(0.75);
        mainSplit.setBorder(null);

        add(mainSplit, BorderLayout.CENTER);

        // Barra de estado
        statusLabel = new JLabel("  Listo.");
        statusLabel.setFont(FONT_UI);
        statusLabel.setForeground(COLOR_ACCENT);
        statusLabel.setBackground(COLOR_BG);
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        add(statusLabel, BorderLayout.SOUTH);

        // Abrir pestaña inicial en blanco
        agregarPestanaVacia();
    }

    // =====================================================================
    // MENÚ
    // =====================================================================

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(50, 50, 50));
        menuBar.setBorder(BorderFactory.createEmptyBorder());

        // ---- ARCHIVO ----
        JMenu menuArchivo = styledMenu("Archivo");
        JMenuItem miNuevo   = styledMenuItem("Nuevo",  KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        JMenuItem miAbrir   = styledMenuItem("Abrir",  KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
        JMenuItem miGuardar = styledMenuItem("Guardar", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        JMenuItem miSalir   = styledMenuItem("Salir",  KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);

        miNuevo.addActionListener(e -> agregarPestanaVacia());
        miAbrir.addActionListener(e -> abrirArchivo());
        miGuardar.addActionListener(e -> guardarArchivo());
        miSalir.addActionListener(e -> System.exit(0));

        menuArchivo.add(miNuevo);
        menuArchivo.add(miAbrir);
        menuArchivo.add(miGuardar);
        menuArchivo.addSeparator();
        menuArchivo.add(miSalir);

        // ---- EJECUTAR ----
        JMenu menuEjecutar = styledMenu("Ejecutar");
        JMenuItem miEjecutar = styledMenuItem("Ejecutar", KeyEvent.VK_F5, 0);
        miEjecutar.addActionListener(e -> ejecutar());
        menuEjecutar.add(miEjecutar);

        // ---- REPORTES ----
        JMenu menuReportes = styledMenu("Reportes");
        JMenuItem miTokens = styledMenuItem("Tabla de Tokens", 0, 0);
        JMenuItem miErrores = styledMenuItem("Tabla de Errores", 0, 0);

        miTokens.addActionListener(e -> mostrarReporteTokens());
        miErrores.addActionListener(e -> mostrarReporteErrores());

        menuReportes.add(miTokens);
        menuReportes.add(miErrores);

        menuBar.add(menuArchivo);
        menuBar.add(menuEjecutar);
        menuBar.add(menuReportes);

        return menuBar;
    }

    private JMenu styledMenu(String texto) {
        JMenu menu = new JMenu(texto);
        menu.setForeground(Color.WHITE);
        menu.setFont(FONT_UI);
        menu.setBackground(new Color(50, 50, 50));
        return menu;
    }

    private JMenuItem styledMenuItem(String texto, int keyCode, int modifiers) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(FONT_UI);
        item.setBackground(new Color(60, 60, 60));
        item.setForeground(Color.WHITE);
        if (keyCode != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        }
        return item;
    }

    // =====================================================================
    // PANEL EDITOR
    // =====================================================================


    private JToolBar buildToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(45, 45, 45));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)));

        JButton btnEjecutar = new JButton("  ▶  Ejecutar  ");
        btnEjecutar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnEjecutar.setBackground(new Color(40, 160, 80));
        btnEjecutar.setForeground(Color.WHITE);
        btnEjecutar.setFocusPainted(false);
        btnEjecutar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 130, 60), 1),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        btnEjecutar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEjecutar.setToolTipText("Ejecutar (F5)");
        btnEjecutar.addActionListener(e -> ejecutar());
        btnEjecutar.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btnEjecutar.setBackground(new Color(50, 185, 95)); }
            @Override public void mouseExited(MouseEvent e)  { btnEjecutar.setBackground(new Color(40, 160, 80)); }
        });

        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(btnEjecutar);
        toolbar.add(Box.createHorizontalGlue());
        return toolbar;
    }

    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_ACCENT, 1),
            "Editor de Código",
            TitledBorder.LEFT, TitledBorder.TOP,
            FONT_UI, COLOR_ACCENT
        );
        panel.setBorder(border);

        // Pestañas
        editorTabs = new JTabbedPane();
        editorTabs.setBackground(COLOR_BG);
        editorTabs.setForeground(Color.WHITE);
        editorTabs.setFont(FONT_UI);

        // Botón para cerrar pestañas con clic derecho
        editorTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = editorTabs.indexAtLocation(e.getX(), e.getY());
                    if (idx >= 0 && editorTabs.getTabCount() > 1) {
                        editorTabs.remove(idx);
                        tabFiles.remove(idx);
                    }
                }
            }
        });

        panel.add(editorTabs, BorderLayout.CENTER);

        // Barra inferior del editor con info de línea/columna
        JLabel lineInfo = new JLabel("Línea: 1  Columna: 1");
        lineInfo.setFont(FONT_UI);
        lineInfo.setForeground(new Color(150, 150, 150));
        lineInfo.setBackground(COLOR_BG);
        lineInfo.setOpaque(true);
        lineInfo.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        panel.add(lineInfo, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea un nuevo editor de código (JTextArea) para una pestaña.
     */
    private JTextArea crearEditor() {
        JTextArea editor = new JTextArea();
        editor.setFont(FONT_CODE);
        editor.setBackground(COLOR_EDITOR_BG);
        editor.setForeground(COLOR_EDITOR_FG);
        editor.setCaretColor(Color.WHITE);
        editor.setTabSize(4);
        editor.setLineWrap(false);
        editor.setMargin(new Insets(5, 5, 5, 5));

        // Números de línea
        editor.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        return editor;
    }

    /**
     * Agrega una nueva pestaña en blanco al editor.
     */
    private void agregarPestanaVacia() {
        int num = editorTabs.getTabCount() + 1;
        String titulo = "Sin título " + num;
        JTextArea editor = crearEditor();
        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBackground(COLOR_EDITOR_BG);
        scroll.getVerticalScrollBar().setBackground(COLOR_BG);
        editorTabs.addTab(titulo, scroll);
        editorTabs.setSelectedIndex(editorTabs.getTabCount() - 1);
    }

    /**
     * Obtiene el editor de la pestaña activa.
     */
    private JTextArea getEditorActivo() {
        int idx = editorTabs.getSelectedIndex();
        if (idx < 0) return null;
        JScrollPane scroll = (JScrollPane) editorTabs.getComponentAt(idx);
        return (JTextArea) scroll.getViewport().getView();
    }

    // =====================================================================
    // PANEL OUTPUT
    // =====================================================================

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_ACCENT, 1),
            "Output (Resultados)",
            TitledBorder.LEFT, TitledBorder.TOP,
            FONT_UI, COLOR_ACCENT
        );
        panel.setBorder(border);

        outputArea = new JTextArea();
        outputArea.setFont(FONT_CODE);
        outputArea.setBackground(COLOR_EDITOR_BG);
        outputArea.setForeground(COLOR_EDITOR_FG);
        outputArea.setEditable(false);
        outputArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBackground(COLOR_EDITOR_BG);
        panel.add(scroll, BorderLayout.CENTER);

        // Botón limpiar output
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setFont(FONT_UI);
        btnLimpiar.setBackground(new Color(60, 60, 60));
        btnLimpiar.setForeground(Color.WHITE);
        btnLimpiar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btnLimpiar.addActionListener(e -> outputArea.setText(""));
        panel.add(btnLimpiar, BorderLayout.SOUTH);

        return panel;
    }

    // =====================================================================
    // PANEL CONSOLA
    // =====================================================================

    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_CONSOLE_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
            "Consola",
            TitledBorder.LEFT, TitledBorder.TOP,
            FONT_UI, new Color(150, 150, 150)
        );
        panel.setBorder(border);

        consoleArea = new JTextArea();
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleArea.setBackground(COLOR_CONSOLE_BG);
        consoleArea.setForeground(COLOR_CONSOLE_FG);
        consoleArea.setEditable(false);
        consoleArea.setMargin(new Insets(4, 6, 4, 6));

        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBackground(COLOR_CONSOLE_BG);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // =====================================================================
    // ACCIONES
    // =====================================================================

    /**
     * Ejecuta el código del editor activo.
     */
    private void ejecutar() {
        JTextArea editor = getEditorActivo();
        if (editor == null) {
            setStatus("No hay editor activo.", COLOR_ERROR);
            return;
        }

        String codigo = editor.getText();
        setStatus("Ejecutando...", COLOR_ACCENT);

        // Ejecutar en hilo separado para no bloquear la UI
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                controller.ejecutar(codigo);
                return null;
            }

            @Override
            protected void done() {
                // Actualizar UI en el EDT
                actualizarUI();
            }
        };
        worker.execute();
    }

    /**
     * Actualiza todas las áreas de la UI con los resultados de la ejecución.
     */
    private void actualizarUI() {
        // Consola
        consoleArea.setText(controller.getConsoleOutput());
        consoleArea.setCaretPosition(0);

        // Output: extraer las tablas del output de consola
        // (las tablas generadas por READ aparecen en la consola)
        // Output: solo resultados de READ
        String qout = controller.getQueryOutput();
        if (qout != null && !qout.isEmpty()) {
            outputArea.setText(qout);
        } else {
            outputArea.setText("(Sin resultados de consulta en esta ejecucion)");
        }
        outputArea.setCaretPosition(0);

        // Actualizar modelos de reporte
        tokenTableModel.setTokens(controller.getTokens());
        errorTableModel.setErrors(controller.getErrors());

        // Status
        if (controller.hasErrors()) {
            setStatus("Ejecución completada con " + controller.getErrors().size() + " error(es).", COLOR_ERROR);
        } else {
            setStatus("Ejecución exitosa.", COLOR_SUCCESS);
        }
    }

    /**
     * Abre un archivo .code desde el sistema de archivos.
     */
    private void abrirArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ELI (*.code)", "code"));
        chooser.setDialogTitle("Abrir archivo ELI");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String contenido = controller.cargarArchivo(file);
                JTextArea editor = crearEditor();
                editor.setText(contenido);
                JScrollPane scroll = new JScrollPane(editor);
                scroll.setBackground(COLOR_EDITOR_BG);
                editorTabs.addTab(file.getName(), scroll);
                editorTabs.setSelectedIndex(editorTabs.getTabCount() - 1);
                tabFiles.put(editorTabs.getSelectedIndex(), file);
                setStatus("Archivo abierto: " + file.getName(), COLOR_SUCCESS);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al abrir el archivo:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                setStatus("Error al abrir archivo.", COLOR_ERROR);
            }
        }
    }

    /**
     * Guarda el archivo de la pestaña activa.
     */
    private void guardarArchivo() {
        JTextArea editor = getEditorActivo();
        if (editor == null) return;

        int idx = editorTabs.getSelectedIndex();
        File file = tabFiles.get(idx);

        if (file == null) {
            // Guardar como nuevo archivo
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Archivos ELI (*.code)", "code"));
            chooser.setDialogTitle("Guardar archivo ELI");
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".code")) {
                    file = new File(file.getAbsolutePath() + ".code");
                }
                tabFiles.put(idx, file);
                editorTabs.setTitleAt(idx, file.getName());
            } else {
                return;
            }
        }

        try {
            controller.guardarArchivo(file, editor.getText());
            setStatus("Archivo guardado: " + file.getName(), COLOR_SUCCESS);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al guardar:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // REPORTES (diálogos)
    // =====================================================================

    private void mostrarReporteTokens() {
        JDialog dialog = new JDialog(this, "Reporte de Tokens", true);
        dialog.setSize(700, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTable table = new JTable(tokenTableModel);
        estilizarTabla(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(COLOR_BG);

        JLabel titulo = new JLabel("  Tokens reconocidos: " + tokenTableModel.getRowCount());
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(COLOR_ACCENT);
        titulo.setBackground(COLOR_BG);
        titulo.setOpaque(true);
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        dialog.add(titulo, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.getContentPane().setBackground(COLOR_BG);
        dialog.setVisible(true);
    }

    private void mostrarReporteErrores() {
        JDialog dialog = new JDialog(this, "Reporte de Errores", true);
        dialog.setSize(750, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTable table = new JTable(errorTableModel);
        estilizarTabla(table);

        JScrollPane scroll = new JScrollPane(table);

        JLabel titulo = new JLabel("  Errores encontrados: " + errorTableModel.getRowCount());
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(COLOR_ERROR);
        titulo.setBackground(COLOR_BG);
        titulo.setOpaque(true);
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        dialog.add(titulo, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.getContentPane().setBackground(COLOR_BG);
        dialog.setVisible(true);
    }

    private void estilizarTabla(JTable table) {
        table.setBackground(COLOR_EDITOR_BG);
        table.setForeground(COLOR_EDITOR_FG);
        table.setFont(FONT_CODE);
        table.setRowHeight(24);
        table.setGridColor(new Color(70, 70, 70));
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setBackground(new Color(60, 60, 60));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Centrar columnas numéricas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(COLOR_EDITOR_BG);
        centerRenderer.setForeground(COLOR_EDITOR_FG);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private void setStatus(String mensaje, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  " + mensaje);
            statusLabel.setForeground(color);
        });
    }

    // =====================================================================
    // MAIN
    // =====================================================================

    public static void main(String[] args) {
        // Look and feel del sistema o Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Usar el L&F por defecto si Nimbus no está disponible
        }

        SwingUtilities.invokeLater(MainWindow::new);
    }
}