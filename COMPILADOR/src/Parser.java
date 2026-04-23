public class Parser {

    private Token[] tokens;
    private int tokenCount;
    private int index = 0;
    private Token actual;
    public String errores = "";

    public Parser(Token[] tokens, int tokenCount) {
        this.tokens = tokens;
        this.tokenCount = tokenCount;
        if (tokenCount > 0) actual = tokens[0];
    }

    private void avanzar() {
        index++;
        actual = index < tokenCount ? tokens[index] : null;
    }

    private void match(int idEsperado) {
        if (actual != null && actual.getId() == idEsperado) {
            avanzar();
        } else {
            errores += "Error sintáctico en línea "
                    + (actual != null ? actual.getLinea() : "desconocida")
                    + ": se esperaba token con ID " + idEsperado + "\n";
            avanzar();
        }
    }

    public void parse() {
        programa();
        if (actual != null)
            errores += "Error: tokens después del cierre de clase\n";
    }

    private void programa() {
        match(1);   // public
        match(2);   // class
        match(201); // ID (nombre clase)
        match(50);  // {
        listaSentencias();
        match(51);  // }
    }

    private void listaSentencias() {
        while (actual != null && actual.getId() != 51)
            sentencia();
    }

    private void sentencia() {
        if (actual == null) return;
        switch (actual.getId()) {
            case 8: case 9: case 10: declaracion();    break;  // int, double, boolean
            case 201:                asignacion();     break;  // ID
            case 11:                 sentenciaIf();    break;  // if
            case 12:                 sentenciaWhile(); break;  // while
            case 13:                 println();        break;  // println
            default:
                errores += "Sentencia inválida en línea " + actual.getLinea() + "\n";
                avanzar();
        }
    }

    private void declaracion() {
        match(actual.getId()); // tipo (int/double/boolean)
        match(201);            // ID
        match(54);             // ;
    }

    private void asignacion() {
        match(201);  // ID
        match(104);  // =
        expresion();
        match(54);   // ;
    }

    private void sentenciaIf() {
        match(11);  // if
        match(52);  // (
        expresion();
        match(53);  // )
        match(50);  // {
        listaSentencias();
        match(51);  // }
    }

    private void sentenciaWhile() {
        match(12);  // while
        match(52);  // (
        expresion();
        match(53);  // )
        match(50);  // {
        listaSentencias();
        match(51);  // }
    }

    private void println() {
        match(13);  // println
        match(52);  // (
        expresion();
        match(53);  // )
        match(54);  // ;
    }

    /**
     * expresion → factor ( opArit factor )* ( opComp factor )?
     *
     * opArit : + - * /        (IDs 100-103)
     * opComp : == != < > <= >= (IDs 105-110)
     *
     * CORRECCIÓN: la versión anterior solo aceptaba opArit (100-103)
     * dentro del while, por eso fallaba con > < == etc. en condiciones
     * de if/while.  Ahora también se consume el operador de comparación
     * al final de la expresión.
     */
    private void expresion() {
        if (actual == null) return;

        // Primer operando (factor)
        if (esOperando(actual.getId())) {
            avanzar();
        } else {
            errores += "Expresión inválida en línea " + actual.getLinea() + "\n";
            avanzar();
            return;
        }

        // Operadores aritméticos: ( opArit factor )*
        while (actual != null && esOpArit(actual.getId())) {
            avanzar(); // consume operador aritmético
            if (actual != null && esOperando(actual.getId())) {
                avanzar();
            } else {
                errores += "Expresión inválida en línea "
                        + (actual != null ? actual.getLinea() : "?") + "\n";
                return;
            }
        }

        // Operador de comparación opcional: ( opComp factor )?
        // Esto es lo que faltaba: > < == != <= >=
        if (actual != null && esOpComp(actual.getId())) {
            avanzar(); // consume operador de comparación
            if (actual != null && esOperando(actual.getId())) {
                avanzar();
            } else {
                errores += "Expresión inválida en línea "
                        + (actual != null ? actual.getLinea() : "?") + "\n";
            }
        }
    }

    // ── Helpers de clasificación ──────────────────────────────────────────

    /** ID (201), NUM (200), CAD (202) */
    private boolean esOperando(int id) {
        return id == 201 || id == 200 || id == 202;
    }

    /** + - * /  (100-103) */
    private boolean esOpArit(int id) {
        return id >= 100 && id <= 103;
    }

    /** == != < > <= >=  (105-110) */
    private boolean esOpComp(int id) {
        return id >= 105 && id <= 110;
    }
}