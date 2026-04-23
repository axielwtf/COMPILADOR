public class Scanner {

    public static final Token[] terminales = {
        new Token(1,  "PR", "public",             0),
        new Token(2,  "PR", "class",              0),
        new Token(3,  "PR", "static",             0),
        new Token(4,  "PR", "void",               0),
        new Token(5,  "PR", "main",               0),
        new Token(6,  "PR", "String",             0),
        new Token(7,  "PR", "args",               0),
        new Token(8,  "PR", "int",                0),
        new Token(9,  "PR", "double",             0),
        new Token(10, "PR", "boolean",            0),
        new Token(11, "PR", "if",                 0),
        new Token(12, "PR", "while",              0),
        new Token(13, "PR", "System.out.println", 0)
    };

    public static final Token[] simbolos = {
        new Token(50, "SIM", "{", 0),
        new Token(51, "SIM", "}", 0),
        new Token(52, "SIM", "(", 0),
        new Token(53, "SIM", ")", 0),
        new Token(54, "SIM", ";", 0)
    };

    public static final Token[] operadores = {
        new Token(100, "OP", "+",  0),
        new Token(101, "OP", "-",  0),
        new Token(102, "OP", "*",  0),
        new Token(103, "OP", "/",  0),
        new Token(104, "OP", "=",  0),
        new Token(105, "OP", "==", 0),
        new Token(106, "OP", "!=", 0),
        new Token(107, "OP", "<",  0),
        new Token(108, "OP", ">",  0),
        new Token(109, "OP", "<=", 0),
        new Token(110, "OP", ">=", 0)
    };

    public Token[] tabla_tokens = new Token[500];
    public int     tokenCount   = 0;
    public String  errores      = "";

    public void analyze(String input) {
        tokenCount = 0;
        errores    = "";

        String[] lines = input.split("\n");

        for (int l = 0; l < lines.length; l++) {
            String line = lines[l];
            int    linea = l + 1;
            int    i = 0;
            int    len = line.length();

            while (i < len) {
                char c = line.charAt(i);

                // -- espacios y tabuladores --
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // -- cadena literal "..." --
                if (c == '"') {
                    int j = i + 1;
                    while (j < len && line.charAt(j) != '"') j++;
                    String cad = line.substring(i, j + 1);
                    addToken(new Token(202, "CAD", cad, linea));
                    i = j + 1;
                    continue;
                }

                // -- operadores de DOS caracteres: == != <= >= --
                if (i + 1 < len) {
                    String dos = "" + c + line.charAt(i + 1);
                    Token t = buscarToken(dos, operadores);
                    if (t != null) {
                        addToken(new Token(t.getId(), t.getTipo(), dos, linea));
                        i += 2;
                        continue;
                    }
                }

                // -- operadores y simbolos de UN caracter --
                String uno = String.valueOf(c);
                Token t;

                if ((t = buscarToken(uno, operadores)) != null) {
                    addToken(new Token(t.getId(), t.getTipo(), uno, linea));
                    i++;
                    continue;
                }
                if ((t = buscarToken(uno, simbolos)) != null) {
                    addToken(new Token(t.getId(), t.getTipo(), uno, linea));
                    i++;
                    continue;
                }

                // -- numero: digitos (y punto decimal) --
                if (Character.isDigit(c)) {
                    int j = i;
                    while (j < len && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '.')) j++;
                    String num = line.substring(i, j);
                    addToken(new Token(200, "NUM", num, linea));
                    i = j;
                    continue;
                }

                // -- identificador o palabra reservada --
                // System.out.println se escanea como identificador extendido
                if (Character.isLetter(c)) {
                    int j = i;
                    // permitir puntos para reconocer System.out.println como una sola palabra
                    while (j < len && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '.')) j++;
                    String palabra = line.substring(i, j);

                    // buscar primero en palabras reservadas (incluye System.out.println)
                    if ((t = buscarToken(palabra, terminales)) != null) {
                        addToken(new Token(t.getId(), t.getTipo(), palabra, linea));
                    } else {
                        // identificador de usuario
                        addToken(new Token(201, "ID", palabra, linea));
                    }
                    i = j;
                    continue;
                }

                
                errores += "Error lexico (linea " + linea + "): " + c + "\n";
                
                i++;
            }
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private void addToken(Token t) {
        tabla_tokens[tokenCount] = t;
        tokenCount++;
    }

    private Token buscarToken(String w, Token[] tabla) {
        for (int i = 0; i < tabla.length; i++) {
            if (tabla[i].getLexema().equals(w)) return tabla[i];
        }
        return null;
    }
}