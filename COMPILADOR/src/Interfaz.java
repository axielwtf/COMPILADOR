import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class Interfaz {

    private JTextArea      inputArea;
    private JTextArea      errorsArea;
    private JTextArea      ciArea;
    private JTable         tabla_tokens;
    private DefaultTableModel tableModel;

    private Scanner scanner;
    private String  asmGenerado = "";
    private JTextArea      coArea;

    public Interfaz() {
        scanner = new Scanner();

        JFrame frame = new JFrame("Silksong Compiler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920, 1060);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setBackground(Color.BLACK);

        
        JMenuBar menuBar = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem abrirArchivo = new JMenuItem("Abrir");
        menuArchivo.add(abrirArchivo);
        menuBar.add(menuArchivo);
        abrirArchivo.addActionListener(e -> AbrirArchivo());
        
        
        frame.setJMenuBar(menuBar);

        
        
        JPanel topPanel = new JPanel(new GridLayout(1, 3, 10, 0));

        topPanel.setPreferredSize(new Dimension(1400, 320));
        
        // Código fuente
        JPanel inputPanel = new JPanel(new BorderLayout());
        
        inputPanel.setBorder(new TitledBorder("Código Fuente"));
        inputArea = new JTextArea(12, 30);
        inputArea.setFont(new Font("Courier New", Font.PLAIN, 18));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);


        // Tokens
        JPanel tokensPanel = new JPanel(new BorderLayout());
        tokensPanel.setBorder(new TitledBorder("Tokens"));
        tableModel = new DefaultTableModel(new Object[]{"Tipo", "Lexema", "ID", "Linea"}, 0);
        tabla_tokens = new JTable(tableModel);
        tabla_tokens.setRowHeight(18);
        tabla_tokens.getTableHeader().setReorderingAllowed(false);
        tokensPanel.add(new JScrollPane(tabla_tokens), BorderLayout.CENTER);

        // Errores
        JPanel errorsPanel = new JPanel(new BorderLayout());
        errorsPanel.setBorder(new TitledBorder("Errores"));
        errorsArea = new JTextArea(12, 25);
        errorsArea.setFont(new Font("Courier New", Font.PLAIN, 16));
        errorsArea.setEditable(false);
        errorsArea.setBackground(new Color(255, 240, 240));
        errorsPanel.add(new JScrollPane(errorsArea), BorderLayout.CENTER);
        

        topPanel.add(inputPanel);
        topPanel.add(tokensPanel);
        topPanel.add(errorsPanel);

        JPanel ciPanel = new JPanel(new BorderLayout());
        ciPanel.setBorder(new TitledBorder("Código Intermedio"));

        ciArea = new JTextArea();
        ciArea.setFont(new Font("Courier New", Font.PLAIN, 18));
        ciArea.setEditable(false);
        ciPanel.add(new JScrollPane(ciArea), BorderLayout.CENTER);


        JPanel coPanel = new JPanel(new BorderLayout());
        coPanel.setBorder(new TitledBorder("Código Objeto"));

        coArea = new JTextArea();
        coArea.setFont(new Font("Courier New", Font.PLAIN, 18));
        coArea.setEditable(false);
        coPanel.add(new JScrollPane(coArea), BorderLayout.CENTER);

        JSplitPane splitCodigos = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ciPanel, coPanel);
        splitCodigos.setResizeWeight(0.5);
        splitCodigos.setDividerSize(6);
        splitCodigos.setContinuousLayout(true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        JButton btnScanner   = boton("Scanner",   e -> analyzeCode());
        JButton btnParser    = boton("Parser",    e -> parseCode());
        JButton btnSemantic  = boton("Semantico", e -> semanticCode());
        JButton btnCI        = boton("CI", e -> generarCI());
        JButton btnCO        = boton("CO",        null);   // futura implementación
        JButton btnLimpiar   = boton("Limpiar",   e -> clearAll());
        JButton btnCopiar   = boton("Copiar", e -> copiarTASM());

        buttonPanel.add(btnScanner);
        buttonPanel.add(btnParser);
        buttonPanel.add(btnSemantic);
        buttonPanel.add(btnCI);
        buttonPanel.add(btnCO);
        buttonPanel.add(btnLimpiar);
        buttonPanel.add(btnCopiar);

        frame.add(topPanel,    BorderLayout.NORTH);
        frame.add(splitCodigos, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void analyzeCode() {
        tableModel.setRowCount(0);
        ciArea.setText("");
        scanner.analyze(inputArea.getText());

        for (int i = 0; i < scanner.tokenCount; i++) {
            Token t = scanner.tabla_tokens[i];
            tableModel.addRow(new Object[]{t.getTipo(), t.getLexema(), t.getId(), t.getLinea()});
        }
        errorsArea.setForeground(Color.GREEN.darker());
        errorsArea.setText(scanner.errores.isEmpty() ? "Sin errores léxicos." : scanner.errores);
    }

    private void parseCode() {
        if (!verificarScanner()) return;
        Parser p = new Parser(scanner.tabla_tokens, scanner.tokenCount);
        p.parse();
        errorsArea.setForeground(Color.GREEN.darker());
        errorsArea.setText(p.errores.isEmpty() ? "Análisis sintáctico exitoso." : p.errores);
    }

    private void semanticCode() {
        if (!verificarScanner()) return;
        if (!verificarParser())  return;
        Semantico s = new Semantico(scanner.tabla_tokens, scanner.tokenCount);
        s.analizar();
        errorsArea.setText(s.errores.isEmpty() ? "Análisis semántico exitoso." : s.errores);
    }

    private void generarCI() {
        if (!verificarScanner()) return;
        if (!verificarParser())  return;
        if (!verificarSemantico()) return;

        CI ci = new CI(scanner.tabla_tokens, scanner.tokenCount);
        ci.generar();

        ciArea.setText(ci.getCI());
        ciArea.setCaretPosition(0);
        asmGenerado = ci.getASM();

        errorsArea.setText(ci.errores.isEmpty()
            ? "Código generado correctamente."
            : ci.errores);
        errorsArea.setForeground(ci.errores.isEmpty() ? Color.GREEN.darker() : Color.RED);
    }

    private void copiarTASM() {
        if (asmGenerado == null || asmGenerado.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Primero genera el código con el botón CI → TASM.",
                "Sin código", JOptionPane.WARNING_MESSAGE);
            return;
        }
        java.awt.datatransfer.StringSelection sel =
            new java.awt.datatransfer.StringSelection(asmGenerado);
        java.awt.Toolkit.getDefaultToolkit()
            .getSystemClipboard().setContents(sel, sel);
        JOptionPane.showMessageDialog(null,
            "Código TASM copiado al portapapeles.",
            "Copiado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void guardarASM() {
        String contenido = ciArea.getText();
        if (contenido == null || contenido.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Primero genera el código con el botón CI → TASM.",
                "Sin código", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar código TASM");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos TASM (*.asm)", "asm"));
        fc.setSelectedFile(new File("programa.asm"));

        if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            if (!archivo.getName().endsWith(".asm"))
                archivo = new File(archivo.getAbsolutePath() + ".asm");
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.print(contenido);
                JOptionPane.showMessageDialog(null,
                    "Archivo guardado:\n" + archivo.getAbsolutePath(),
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                    "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearAll() {
        inputArea.setText("");
        errorsArea.setText("");
        ciArea.setText("");
        tableModel.setRowCount(0);
        coArea.setText("");
    }

    private boolean verificarScanner() {
        if (scanner.tokenCount == 0) {
            errorsArea.setForeground(Color.RED);
            errorsArea.setText("Primero ejecuta el Scanner.");
            return false;
        }
        if (!scanner.errores.isEmpty()) {
            errorsArea.setForeground(Color.RED);
            errorsArea.setText("Hay errores léxicos. Corrígelos primero.\n\n" + scanner.errores);
            return false;
        }
        return true;
    }

    private boolean verificarParser() {
        Parser p = new Parser(scanner.tabla_tokens, scanner.tokenCount);
        p.parse();
        if (!p.errores.isEmpty()) {
            errorsArea.setForeground(Color.RED);
            errorsArea.setText("Hay errores sintácticos. Corrígelos primero.\n\n" + p.errores);
            return false;
        }
        return true;
    }

    private boolean verificarSemantico() {
        Semantico s = new Semantico(scanner.tabla_tokens, scanner.tokenCount);
        s.analizar();
        if (!s.errores.isEmpty()) {
            errorsArea.setForeground(Color.RED);
            errorsArea.setText("Hay errores semánticos. Corrígelos primero.\n\n" + s.errores);
            return false;
        }
        return true;
    }

    private JButton boton(String texto, java.awt.event.ActionListener al) {
        JButton b = new JButton(texto);
        b.setPreferredSize(new Dimension(150, 32));
        if (al != null) b.addActionListener(al);
        else b.setEnabled(false);
        return b;
    }

    public void AbrirArchivo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Abrir archivo de código fuente");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos de texto (*.java, *.src)", "java", "src"));

        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                inputArea.read(br, null);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                    "Error al abrir el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Interfaz::new);
    }
}