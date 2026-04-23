import java.util.*;

public class CI {
    static class Instruccion {
        String numero;
        String operacion;
        String operandos;
        String comentario;

        Instruccion(String numero, String operacion, String operandos, String comentario) {
            this.numero     = numero;
            this.operacion  = operacion;
            this.operandos  = operandos;
            this.comentario = comentario;
        }
    }

    private Token[] tokens;
    private int     tokenCount;
    private int     idx;
    private Token   actual;

    private List<Instruccion> ci = new ArrayList<>();

    private LinkedHashMap<String, String> variables  = new LinkedHashMap<>();
    private HashMap<String, Integer>      valorConocido   = new HashMap<>();
    private HashSet<String>               inicializadaEnData = new HashSet<>();
    private LinkedHashMap<String, String> strings     = new LinkedHashMap<>();
    private LinkedHashMap<String, String> msgs        = new LinkedHashMap<>();

    private List<String> asmCode = new ArrayList<>();

    private int labelCount = 0;
    private int strCount   = 0;
    private int msgCount   = 0;
    private int instrCount = 0;   // contador de filas CI

    public String errores = "";

    public CI(Token[] tokens, int tokenCount) {
        this.tokens     = tokens;
        this.tokenCount = tokenCount;
    }

    public void generar() {
        errores      = "";
        labelCount   = 0;
        strCount     = 0;
        msgCount     = 0;
        instrCount   = 0;
        idx          = 0;
        actual       = tokenCount > 0 ? tokens[0] : null;
        ci.clear();
        variables.clear();
        valorConocido.clear();
        inicializadaEnData.clear();
        strings.clear();
        msgs.clear();
        asmCode.clear();
        programa();
    }

 
    public String getCI() {
        int col1 = 8;   // Numero
        int col2 = 12;  // Operacion
        int col3 = 20;  // Operandos

        StringBuilder sb = new StringBuilder();

        sb.append(pad("Numero", col1))
          .append(pad("Operacion", col2))
          .append("Operandos\n");
        for (int i = 0; i < col1 + col2 + col3; i++) sb.append("-");
        sb.append("\n");

        for (Instruccion ins : ci) {
            sb.append(pad(ins.numero,    col1))
              .append(pad(ins.operacion, col2))
              .append(ins.operandos)
              .append("\n");
        }

        return sb.toString();
    }

    public String getASM() {
        StringBuilder sb = new StringBuilder();

        sb.append(".model small\n");
        sb.append(".stack 100h\n\n");
        sb.append(".data\n");

        for (Map.Entry<String, String> e : variables.entrySet())
            sb.append("    ").append(e.getKey())
              .append(" ").append(e.getValue()).append("\n");

        for (Map.Entry<String, String> e : msgs.entrySet())
            sb.append("    ").append(e.getKey())
              .append(" db ").append(e.getValue()).append("\n");

        for (Map.Entry<String, String> e : strings.entrySet())
            sb.append("    ").append(e.getKey())
              .append(" db ").append(e.getValue()).append(", '$'\n");

        sb.append("\n.code\n");
        sb.append("main proc\n");
        sb.append("    mov ax, @data\n");
        sb.append("    mov ds, ax\n\n");

        for (String line : asmCode)
            sb.append(line).append("\n");

        sb.append("\n    mov ah, 4Ch\n");
        sb.append("    int 21h\n");
        sb.append("main endp\n");
        sb.append("\nend main\n");

        return sb.toString();
    }


    private void emitCI(String operacion, String operandos, String comentario) {
        ci.add(new Instruccion(String.valueOf(++instrCount), operacion, operandos, comentario));
    }

    private void emitCIDir(String numero, String operacion, String operandos, String comentario) {
        ci.add(new Instruccion(numero, operacion, operandos, comentario));
    }

    private void emit(String line) {
        asmCode.add(line);
        String trim = line.trim();
        if (trim.isEmpty()) return;
        if (trim.endsWith(":")) {
            emitCIDir(trim, "", "", "");
            return;
        }
        String[] parts = trim.split("\\s+", 2);
        String op  = parts[0].toUpperCase();
        String ops = parts.length > 1 ? parts[1].trim() : "";
        emitCI(op, ops, "");
    }

    private void emitLabel(String label) {
        emit(label + ":");
    }

    private void emitAmbos(String operacion, String operandos) {
        emit("    " + operacion.toLowerCase() + " " + operandos);
    }

    private void avanzar() {
        idx++;
        actual = idx < tokenCount ? tokens[idx] : null;
    }

    private boolean match(int id) {
        if (actual != null && actual.getId() == id) { avanzar(); return true; }
        return false;
    }

    private String nuevaEtiqueta() { return "E" + (++labelCount); }

    private boolean esLiteral(String s) {
        try { Integer.parseInt(s); return true; }
        catch (Exception e) { return false; }
    }

    private void loadAX(String v) {
        if (!v.equals("__AX__")) emitAmbos("MOV", "ax, " + v);
    }

    private void storeAX(String dest) { emitAmbos("MOV", dest + ", ax"); }

    private Integer resolverValor(String v) {
        if (esLiteral(v)) return Integer.parseInt(v);
        return valorConocido.get(v);
    }

    private String pad(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s + "  ";
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    private void programa() {
        match(1);    // public
        match(2);    // class
        String nombreClase = actual != null ? actual.getLexema() : "Programa";
        match(201);
        match(50);   // {

        emitCIDir("", "TITLE",  nombreClase, "");
        emitCIDir("", ".MODEL", "Small",     "");
        emitCIDir("", ".STACK", "",          "");
        emitCIDir("", ".DATA",  "",          "");

        int idxAntes = idx;
        Token actualAntes = actual;
        soloDeclaraciones();

        emitCIDir("", ".CODE",  "",          "");
        emitCIDir("", "MOV",    "AX, @data", "");
        emitCIDir("", "MOV",    "DS, AX",    "");

        idx    = idxAntes;
        actual = actualAntes;
        listaSentencias();

        match(51);   // }
    }

    private void soloDeclaraciones() {
        int saved = idx;
        Token savedActual = actual;
        while (actual != null && actual.getId() != 51) {
            if (actual.getId() == 6 || actual.getId() == 8 ||
                actual.getId() == 9 || actual.getId() == 10) {
                declaracion();
            } else {
                avanzar();
            }
        }
    }

    private void listaSentencias() {
        while (actual != null && actual.getId() != 51)
            sentencia();
    }

    private void sentencia() {
        if (actual == null) return;
        switch (actual.getId()) {
            case 6: case 8: case 9: case 10: saltarDeclaracion(); break;
            case 201:                         asignacion();        break;
            case 11:                          sentenciaIf();       break;
            case 12:                          sentenciaWhile();    break;
            case 13:                          println();           break;
            default: avanzar(); break;
        }
    }

    private void saltarDeclaracion() {
        avanzar(); // tipo
        if (actual != null && actual.getId() == 201) avanzar(); // nombre
        match(54); // ;
    }

    private void declaracion() {
        String tipo = actual.getLexema();
        avanzar();
        if (actual != null && actual.getId() == 201) {
            String nombre = actual.getLexema();
            avanzar();

            String dir;
            if (tipo.equals("String")) {
                dir = "db 128 dup ('$')";
                emitCIDir(nombre, "DB", "128 DUP (?)", "cadena");
            } else if (tipo.equals("double")) {
                dir = "dd 0";
                emitCIDir(nombre, "DD", "?", "");
            } else {
                dir = "dw 0";
                emitCIDir(nombre, "DW", "?", "");
            }
            variables.putIfAbsent(nombre, dir);
        }
        match(54); // ;
    }

    private void asignacion() {
        String nombre = actual.getLexema();
        avanzar();
        match(104); // =

        boolean sigueEsLiteral = (actual != null && actual.getId() == 200);
        String res = expresion();
        match(54); // ;

        if (sigueEsLiteral && esLiteral(res)) {
            int val = Integer.parseInt(res);
            if (variables.containsKey(nombre)) {
                String tipo = variables.get(nombre).startsWith("dd") ? "dd" : "dw";
                variables.put(nombre, tipo + " " + val);
                for (Instruccion ins : ci) {
                    if (ins.numero.equals(nombre) && ins.operandos.equals("?")) {
                        ins.operandos = String.valueOf(val);
                    }
                }
            }
            valorConocido.put(nombre, val);
            inicializadaEnData.add(nombre);

        } else {
            loadAX(res);
            storeAX(nombre);
            emit("");
            valorConocido.remove(nombre);
            inicializadaEnData.remove(nombre);
        }
    }

    private void sentenciaIf() {
        match(11);
        match(52);
        String lFalse = nuevaEtiqueta();
        String lFin   = nuevaEtiqueta();
        String cond   = expresion();
        match(53);

        emitBranch(cond, lFalse);

        match(50);
        listaSentencias();
        match(51);

        emitAmbos("JMP", lFin);
        emitLabel(lFalse);
        emitLabel(lFin);
        emit("");
    }

    private void sentenciaWhile() {
        match(12);
        String lIni   = nuevaEtiqueta();
        String lSalir = nuevaEtiqueta();

        emitLabel(lIni);

        match(52);
        String cond = expresion();
        match(53);

        emitBranch(cond, lSalir);

        match(50);
        valorConocido.clear();
        inicializadaEnData.clear();
        listaSentencias();
        match(51);

        emitAmbos("JMP", lIni);
        emitLabel(lSalir);
        emit("");
    }

    private void println() {
        match(13);
        match(52);

        if (actual != null && actual.getId() == 202) {
            String raw   = actual.getLexema();
            String label = "cad" + (++strCount);
            String inner = raw.substring(1, raw.length() - 1).replace("'", "''");
            strings.put(label, "'" + inner + "'");
            avanzar();

            emit("    mov ah, 09h");
            emit("    mov dx, offset " + label);
            emit("    int 21h");
        } else {
            String res = expresion();
            emitPrintNumber(res);
        }

        match(53);
        match(54);
        emit("");
    }

    private String expresion() {
        if (actual == null) return "0";

        String izq = factor();

        while (actual != null && actual.getId() >= 100 && actual.getId() <= 103) {
            int    opId = actual.getId();
            String opLex = actual.getLexema();
            avanzar();
            String der = factor();

            loadAX(izq);
            switch (opId) {
                case 100: emitAmbos("ADD", "ax, " + der); break;
                case 101: emitAmbos("SUB", "ax, " + der); break;
                case 102:
                    emitAmbos("MOV", "bx, " + der);
                    emitAmbos("IMUL", "bx");
                    break;
                case 103:
                    emit("    cwd");
                    emitCI("CWD", "", "");
                    emitAmbos("MOV", "bx, " + der);
                    emitAmbos("IDIV", "bx");
                    break;
            }
            izq = "__AX__";
        }

        if (actual != null && actual.getId() >= 105 && actual.getId() <= 110) {
            String op = actual.getLexema();
            avanzar();
            String der = factor();
            return "CMP:" + op + ":" + izq + ":" + der;
        }

        return izq;
    }

    private String factor() {
        if (actual == null) return "0";
        String v = actual.getLexema();
        avanzar();
        return v;
    }

    private String opMnemonico(int opId) {
        switch (opId) {
            case 100: return "ADD";
            case 101: return "SUB";
            case 102: return "MUL";
            case 103: return "DIV";
            default:  return "OP";
        }
    }

    private void emitBranchCI(String cond, String lFalse) {
        if (cond.startsWith("CMP:")) {
            String[] p  = cond.split(":");
            String op   = p[1], izq = p[2], der = p[3];
            emitCI("CMP", (izq.equals("__AX__") ? "AX" : izq) + ", " + der, "");
            emitCI(saltoInverso(op), lFalse, "");
        } else {
            emitCI("CMP", (cond.equals("__AX__") ? "AX" : cond) + ", 0", "");
            emitCI("JE",  lFalse, "");
        }
    }

    private void emitBranch(String cond, String lFalse) {
        if (cond.startsWith("CMP:")) {
            String[] p  = cond.split(":");
            String op   = p[1], izq = p[2], der = p[3];
            loadAX(izq);
            emitAmbos("CMP", "ax, " + der);
            emitAmbos(saltoInverso(op), lFalse);
        } else {
            loadAX(cond);
            emitAmbos("CMP", "ax, 0");
            emitAmbos("JE", lFalse);
        }
        emit("");
    }

    private String saltoInverso(String op) {
        switch (op) {
            case "==": return "JNE";
            case "!=": return "JE";
            case "<":  return "JGE";
            case ">":  return "JLE";
            case "<=": return "JG";
            case ">=": return "JL";
            default:   return "JNE";
        }
    }

    private void emitPrintNumber(String val) {
        Integer valorInt = esLiteral(val) ? resolverValor(val) : null;

        if (valorInt != null) {
            String label = "num" + (++msgCount);
            msgs.put(label, "'" + valorInt + "$'");
            emit("    mov ah, 09h");
            emit("    mov dx, offset " + label);
            emit("    int 21h");
        } else {
            String lpLabel = "E" + (labelCount + 1);
            String prLabel = "E" + (labelCount + 2);
            labelCount += 2;

            loadAX(val);
            emit("    mov bx, 10");
            emit("    mov cx, 0");
            emit(lpLabel + ":");
            emit("    xor dx, dx");
            emit("    div bx");
            emit("    add dx, 30h");
            emit("    push dx");
            emit("    inc cx");
            emit("    cmp ax, 0");
            emit("    jne " + lpLabel);
            emit(prLabel + ":");
            emit("    pop dx");
            emit("    mov ah, 02h");
            emit("    int 21h");
            emit("    loop " + prLabel);
            emit("    mov ah, 02h");
            emit("    mov dl, 0Dh");
            emit("    int 21h");
            emit("    mov dl, 0Ah");
            emit("    int 21h");
        }
    }
}