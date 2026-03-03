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
 *  - MenuBar: Archivo | Ejecutar | Reportes
 *  - Toolbar: botón ▶ Ejecutar visible
 *  - Panel izquierdo: Editor de código (con pestañas para múltiples archivos)
 *  - Panel derecho: Output (resultado de consultas READ)
 *  - Panel inferior: Consola (mensajes del intérprete)
 *  - Reportes: diálogos modales para Tokens y Errores
 */
public class MainWindow extends JFrame {

    // ===================== CONTROLADOR =====================
    private MainController controller;

    // ===================== COMPONENTES UI =====================
    private JTabbedPane editorTabs;
    private JTextArea outputArea;
    private JTextArea consoleArea;
    private TokenTableModel tokenTableModel;
    private ErrorTableModel errorTableModel;
    private JLabel statusLabel;

    // Mapa pestaña → archivo (usamos el componente JScrollPane como clave, no el índice)
    private java.util.Map<Component, File> tabFiles = new java.util.HashMap<>();

    // ===================== COLORES =====================
    private static final Color COLOR_BG         = new Color(30, 30, 30);
    private static final Color COLOR_EDITOR_BG  = new Color(40, 44, 52);
    private static final Color COLOR_EDITOR_FG  = new Color(220, 220, 220);
    private static final Color COLOR_CONSOLE_BG = new Color(20, 20, 20);
    private static final Color COLOR_CONSOLE_FG = new Color(0, 255, 100);
    private static final Color COLOR_TOOLBAR_BG = new Color(45, 45, 45);
    private static final Color COLOR_ACCENT     = new Color(97, 175, 239);
    private static final Color COLOR_ERROR      = new Color(224, 108, 117);
    private static final Color COLOR_SUCCESS    = new Color(152, 195, 121);
    private static final Color COLOR_BTN_RUN    = new Color(40, 160, 80);
    private static final Font  FONT_CODE        = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font  FONT_UI          = new Font("Segoe UI", Font.PLAIN, 12);

    public MainWindow() {
        controller = new MainController();
        tokenTableModel  = new TokenTableModel();
        errorTableModel  = new ErrorTableModel();
        initUI();
        setVisible(true);
    }

    // =====================================================================
    // INICIALIZACIÓN UI
    // =====================================================================

    private void initUI() {
        setTitle("ELI-NOSQL IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(0, 0));

        // Menú
        setJMenuBar(buildMenuBar());

        // Toolbar con botón Ejecutar
        add(buildToolBar(), BorderLayout.NORTH);

        // Panel central: editor + output lado a lado
        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildEditorPanel(),
                buildOutputPanel());
        centerSplit.setDividerLocation(680);
        centerSplit.setResizeWeight(0.6);
        centerSplit.setBorder(null);
        centerSplit.setBackground(COLOR_BG);

        // Panel inferior: consola
        JPanel bottomPanel = buildConsolePanel();
        bottomPanel.setPreferredSize(new Dimension(0, 180));

        // Split vertical: centro arriba, consola abajo
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                centerSplit, bottomPanel);
        mainSplit.setDividerLocation(540);
        mainSplit.setResizeWeight(0.75);
        mainSplit.setBorder(null);

        add(mainSplit, BorderLayout.CENTER);

        // Barra de estado
        statusLabel = new JLabel("  Listo.");
        statusLabel.setFont(FONT_UI);
        statusLabel.setForeground(COLOR_ACCENT);
        statusLabel.setBackground(new Color(25, 25, 25));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        add(statusLabel, BorderLayout.SOUTH);

        // Pestaña inicial en blanco
        agregarPestanaVacia();
    }

    // =====================================================================
    // TOOLBAR
    // =====================================================================

    private JToolBar buildToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(COLOR_TOOLBAR_BG);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)));

        // Botón ▶ Ejecutar
        JButton btnEjecutar = new JButton("  ▶  Ejecutar  ");
        btnEjecutar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnEjecutar.setBackground(COLOR_BTN_RUN);
        btnEjecutar.setForeground(Color.WHITE);
        btnEjecutar.setFocusPainted(false);
        btnEjecutar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 130, 60), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        btnEjecutar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEjecutar.setToolTipText("Ejecutar código (F5)");
        btnEjecutar.addActionListener(e -> ejecutar());

        // Hover effect
        btnEjecutar.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btnEjecutar.setBackground(new Color(50, 180, 90));
            }
            @Override public void mouseExited(MouseEvent e) {
                btnEjecutar.setBackground(COLOR_BTN_RUN);
            }
        });

        // Botón Nuevo
        JButton btnNuevo = toolbarButton("＋ Nuevo", COLOR_TOOLBAR_BG);
        btnNuevo.addActionListener(e -> agregarPestanaVacia());

        // Botón Abrir
        JButton btnAbrir = toolbarButton("📂 Abrir", COLOR_TOOLBAR_BG);
        btnAbrir.addActionListener(e -> abrirArchivo());

        // Botón Guardar
        JButton btnGuardar = toolbarButton("💾 Guardar", COLOR_TOOLBAR_BG);
        btnGuardar.addActionListener(e -> guardarArchivo());

        // Separador visual
        JSeparator sep1 = new JSeparator(JSeparator.VERTICAL);
        sep1.setMaximumSize(new Dimension(2, 30));
        sep1.setForeground(new Color(70, 70, 70));

        JSeparator sep2 = new JSeparator(JSeparator.VERTICAL);
        sep2.setMaximumSize(new Dimension(2, 30));
        sep2.setForeground(new Color(70, 70, 70));

        // Botones de reporte
        JButton btnTokens = toolbarButton("📋 Tokens", COLOR_TOOLBAR_BG);
        btnTokens.addActionListener(e -> mostrarReporteTokens());

        JButton btnErrores = toolbarButton("⚠ Errores", COLOR_TOOLBAR_BG);
        btnErrores.addActionListener(e -> mostrarReporteErrores());

        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(btnNuevo);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(btnAbrir);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(btnGuardar);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(sep1);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(btnEjecutar);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(sep2);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(btnTokens);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(btnErrores);
        toolbar.add(Box.createHorizontalGlue());

        return toolbar;
    }

    private JButton toolbarButton(String texto, Color bg) {
        JButton btn = new JButton(texto);
        btn.setFont(FONT_UI);
        btn.setBackground(bg);
        btn.setForeground(new Color(200, 200, 200));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(65, 65, 65));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // =====================================================================
    // MENÚ
    // =====================================================================

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(40, 40, 40));
        menuBar.setBorder(BorderFactory.createEmptyBorder());

        // ---- ARCHIVO ----
        JMenu menuArchivo = styledMenu("Archivo");
        JMenuItem miNuevo   = styledMenuItem("Nuevo",   KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        JMenuItem miAbrir   = styledMenuItem("Abrir",   KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
        JMenuItem miGuardar = styledMenuItem("Guardar", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        JMenuItem miSalir   = styledMenuItem("Salir",   KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);

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
        JMenuItem miTokens  = styledMenuItem("Tabla de Tokens",  0, 0);
        JMenuItem miErrores = styledMenuItem("Tabla de Errores", 0, 0);
        miTokens.addActionListener(e  -> mostrarReporteTokens());
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
        return menu;
    }

    private JMenuItem styledMenuItem(String texto, int keyCode, int modifiers) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(FONT_UI);
        item.setBackground(new Color(55, 55, 55));
        item.setForeground(Color.WHITE);
        if (keyCode != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        }
        return item;
    }

    // =====================================================================
    // PANEL EDITOR
    // =====================================================================

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

        editorTabs = new JTabbedPane();
        editorTabs.setBackground(COLOR_BG);
        editorTabs.setForeground(Color.WHITE);
        editorTabs.setFont(FONT_UI);

        // Clic derecho sobre pestaña → cerrar
        editorTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = editorTabs.indexAtLocation(e.getX(), e.getY());
                    if (idx >= 0 && editorTabs.getTabCount() > 1) {
                        Component comp = editorTabs.getComponentAt(idx);
                        tabFiles.remove(comp);
                        editorTabs.remove(idx);
                    }
                }
            }
        });

        panel.add(editorTabs, BorderLayout.CENTER);

        // Barra inferior: info línea/columna
        JLabel lineInfo = new JLabel("  Línea: 1  Columna: 1");
        lineInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lineInfo.setForeground(new Color(140, 140, 140));
        lineInfo.setBackground(new Color(35, 35, 35));
        lineInfo.setOpaque(true);
        lineInfo.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        panel.add(lineInfo, BorderLayout.SOUTH);

        return panel;
    }

    private JTextArea crearEditor() {
        JTextArea editor = new JTextArea();
        editor.setFont(FONT_CODE);
        editor.setBackground(COLOR_EDITOR_BG);
        editor.setForeground(COLOR_EDITOR_FG);
        editor.setCaretColor(Color.WHITE);
        editor.setTabSize(4);
        editor.setLineWrap(false);
        editor.setMargin(new Insets(6, 6, 6, 6));
        return editor;
    }

    private void agregarPestanaVacia() {
        int num = editorTabs.getTabCount() + 1;
        JTextArea editor = crearEditor();
        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBackground(COLOR_EDITOR_BG);
        editorTabs.addTab("Sin título " + num, scroll);
        editorTabs.setSelectedIndex(editorTabs.getTabCount() - 1);
    }

    /** Devuelve el JTextArea de la pestaña actualmente seleccionada. */
    private JTextArea getEditorActivo() {
        int idx = editorTabs.getSelectedIndex();
        if (idx < 0) return null;
        JScrollPane scroll = (JScrollPane) editorTabs.getComponentAt(idx);
        return (JTextArea) scroll.getViewport().getView();
    }

    /** Devuelve el JScrollPane (componente) de la pestaña activa. */
    private Component getComponenteActivo() {
        int idx = editorTabs.getSelectedIndex();
        if (idx < 0) return null;
        return editorTabs.getComponentAt(idx);
    }

    // =====================================================================
    // PANEL OUTPUT  (solo resultados de READ)
    // =====================================================================

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_ACCENT, 1),
            "Output  —  Resultados de Consultas (READ)",
            TitledBorder.LEFT, TitledBorder.TOP,
            FONT_UI, COLOR_ACCENT
        );
        panel.setBorder(border);

        outputArea = new JTextArea();
        outputArea.setFont(FONT_CODE);
        outputArea.setBackground(COLOR_EDITOR_BG);
        outputArea.setForeground(new Color(180, 230, 180));   // verde claro para diferenciar
        outputArea.setEditable(false);
        outputArea.setMargin(new Insets(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBackground(COLOR_EDITOR_BG);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnLimpiar = new JButton("Limpiar output");
        btnLimpiar.setFont(FONT_UI);
        btnLimpiar.setBackground(new Color(55, 55, 55));
        btnLimpiar.setForeground(Color.WHITE);
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btnLimpiar.addActionListener(e -> outputArea.setText(""));
        panel.add(btnLimpiar, BorderLayout.SOUTH);

        return panel;
    }

    // =====================================================================
    // PANEL CONSOLA  (mensajes del intérprete)
    // =====================================================================

    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_CONSOLE_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70), 1),
            "Consola  —  Mensajes del Intérprete",
            TitledBorder.LEFT, TitledBorder.TOP,
            FONT_UI, new Color(140, 140, 140)
        );
        panel.setBorder(border);

        consoleArea = new JTextArea();
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleArea.setBackground(COLOR_CONSOLE_BG);
        consoleArea.setForeground(COLOR_CONSOLE_FG);
        consoleArea.setEditable(false);
        consoleArea.setMargin(new Insets(4, 8, 4, 8));

        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBackground(COLOR_CONSOLE_BG);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnLimpiarConsola = new JButton("Limpiar consola");
        btnLimpiarConsola.setFont(FONT_UI);
        btnLimpiarConsola.setBackground(new Color(35, 35, 35));
        btnLimpiarConsola.setForeground(new Color(180, 180, 180));
        btnLimpiarConsola.setFocusPainted(false);
        btnLimpiarConsola.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        btnLimpiarConsola.addActionListener(e -> consoleArea.setText(""));
        panel.add(btnLimpiarConsola, BorderLayout.EAST);

        return panel;
    }

    // =====================================================================
    // ACCIONES PRINCIPALES
    // =====================================================================

    /**
     * Ejecuta el código del editor activo.
     * - La consola muestra los mensajes del intérprete (ctx.print / ctx.printError).
     * - El output muestra SOLO las tablas resultado de las instrucciones READ.
     */
    private void ejecutar() {
        JTextArea editor = getEditorActivo();
        if (editor == null) {
            setStatus("No hay editor activo.", COLOR_ERROR);
            return;
        }

        String codigo = editor.getText().trim();
        if (codigo.isEmpty()) {
            setStatus("El editor está vacío.", COLOR_ERROR);
            return;
        }

        setStatus("Ejecutando...", COLOR_ACCENT);
        outputArea.setText("");
        consoleArea.setText("");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                controller.ejecutar(codigo);
                return null;
            }

            @Override
            protected void done() {
                actualizarUI();
            }
        };
        worker.execute();
    }

    /**
     * Actualiza consola, output y modelos de reporte.
     *
     * SEPARACIÓN:
     *   - consoleArea  ← controller.getConsoleOutput()   (todos los ctx.print)
     *   - outputArea   ← controller.getQueryOutput()     (solo tablas READ)
     */
    private void actualizarUI() {
        // --- Consola: mensajes generales del intérprete ---
        String consoleText = controller.getConsoleOutput();
        consoleArea.setText(consoleText);
        consoleArea.setCaretPosition(0);

        // --- Output: solo el resultado de las consultas READ ---
        String queryOutput = controller.getQueryOutput();
        if (queryOutput != null && !queryOutput.isEmpty()) {
            outputArea.setText(queryOutput);
        } else {
            outputArea.setText("(Sin resultados de consulta en esta ejecución)");
        }
        outputArea.setCaretPosition(0);

        // Actualizar modelos de reporte
        tokenTableModel.setTokens(controller.getTokens());
        errorTableModel.setErrors(controller.getErrors());

        // Status bar
        int numErrores = controller.getErrors().size();
        if (numErrores > 0) {
            setStatus("Ejecución completada con " + numErrores + " error(es). Ver Reportes > Tabla de Errores.", COLOR_ERROR);
        } else {
            setStatus("Ejecución completada sin errores.", COLOR_SUCCESS);
        }
    }

    /** Abre un archivo .code desde el sistema de archivos. */
    private void abrirArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ELI (*.code)", "code"));
        chooser.setDialogTitle("Abrir archivo ELI (.code)");

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
                // Guardar la asociación componente → archivo
                tabFiles.put(scroll, file);
                setStatus("Archivo abierto: " + file.getName(), COLOR_SUCCESS);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al abrir el archivo:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                setStatus("Error al abrir archivo.", COLOR_ERROR);
            }
        }
    }

    /** Guarda el archivo de la pestaña activa. */
    private void guardarArchivo() {
        JTextArea editor = getEditorActivo();
        if (editor == null) return;

        Component comp = getComponenteActivo();
        File file = tabFiles.get(comp);

        if (file == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Archivos ELI (*.code)", "code"));
            chooser.setDialogTitle("Guardar archivo ELI (.code)");
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".code")) {
                    file = new File(file.getAbsolutePath() + ".code");
                }
                tabFiles.put(comp, file);
                int idx = editorTabs.getSelectedIndex();
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
            setStatus("Error al guardar.", COLOR_ERROR);
        }
    }

    // =====================================================================
    // REPORTES (diálogos modales)
    // =====================================================================

    private void mostrarReporteTokens() {
        JDialog dialog = new JDialog(this, "Reporte de Tokens", true);
        dialog.setSize(720, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(COLOR_BG);

        JTable table = new JTable(tokenTableModel);
        estilizarTabla(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(COLOR_BG);

        JLabel titulo = new JLabel("  Tokens reconocidos: " + tokenTableModel.getRowCount());
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(COLOR_ACCENT);
        titulo.setBackground(new Color(40, 40, 40));
        titulo.setOpaque(true);
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton btnCerrar = dialogCloseButton(dialog);

        dialog.add(titulo, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnCerrar, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void mostrarReporteErrores() {
        JDialog dialog = new JDialog(this, "Reporte de Errores", true);
        dialog.setSize(780, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(COLOR_BG);

        JTable table = new JTable(errorTableModel);
        estilizarTabla(table);

        JScrollPane scroll = new JScrollPane(table);

        int numErr = errorTableModel.getRowCount();
        JLabel titulo = new JLabel("  Errores encontrados: " + numErr);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(numErr > 0 ? COLOR_ERROR : COLOR_SUCCESS);
        titulo.setBackground(new Color(40, 40, 40));
        titulo.setOpaque(true);
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton btnCerrar = dialogCloseButton(dialog);

        dialog.add(titulo, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnCerrar, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JButton dialogCloseButton(JDialog dialog) {
        JButton btn = new JButton("Cerrar");
        btn.setFont(FONT_UI);
        btn.setBackground(new Color(60, 60, 60));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        btn.addActionListener(e -> dialog.dispose());
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.setBackground(new Color(40, 40, 40));
        p.add(btn);
        // reutilizamos el panel como componente SOUTH del diálogo
        dialog.add(p, BorderLayout.SOUTH);
        return btn;
    }

    private void estilizarTabla(JTable table) {
        table.setBackground(COLOR_EDITOR_BG);
        table.setForeground(COLOR_EDITOR_FG);
        table.setFont(FONT_CODE);
        table.setRowHeight(24);
        table.setGridColor(new Color(65, 65, 65));
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setBackground(new Color(55, 55, 55));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

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
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(MainWindow::new);
    }
}