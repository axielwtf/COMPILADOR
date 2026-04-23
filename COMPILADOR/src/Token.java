public class Token {

    private int id;
    private String tipo;
    private String lexema;
    private int linea;

    public Token(int id, String tipo, String lexema, int linea) {
        this.id = id;
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
    }

    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public int getLinea() {
        return linea;
    }

    @Override
    public String toString() {
        return id + " | " + tipo + " | " + lexema + " | línea: " + linea;
    }
}
