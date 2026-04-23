import java.util.HashMap;
import java.util.Map;

public class Semantico {

    private Token[] tokens;
    private int tokenCount;
    public String errores;
    private Map<String, String> symbolTable;

    public Semantico(Token[] tokens, int tokenCount) {
        this.tokens = tokens;
        this.tokenCount = tokenCount;
        this.symbolTable = new HashMap<>();
    }

    public void analizar() {

        errores = "";
        symbolTable.clear();

        for (int i = 0; i < tokenCount; i++) {

            Token t = tokens[i];

            // Declaración de variables (int, double, etc.)
            if (t.getId() == 2 || 
                t.getId() == 8 || 
                t.getId() == 9 || 
                t.getId() == 10) {

                if (i + 1 < tokenCount && tokens[i + 1].getId() == 201) {

                    String nombreVar = tokens[i + 1].getLexema();
                    String tipoVar = t.getLexema();

                    if (symbolTable.containsKey(nombreVar)) {
                        errores += "Error semántico: variable redeclarada '"
                                + nombreVar + "' en línea "
                                + tokens[i + 1].getLinea() + "\n";
                    } else {
                        symbolTable.put(nombreVar, tipoVar);
                    }
                }
            }

            // Uso de identificadores
            else if (t.getId() == 201) {

                String nombreVar = t.getLexema();

                if (!symbolTable.containsKey(nombreVar)) {
                    errores += "Error semántico: variable no declarada '"
                            + nombreVar + "' en línea "
                            + t.getLinea() + "\n";
                }

                // Verificación de asignación
                if (i + 1 < tokenCount && tokens[i + 1].getId() == 104) {

                    if (i + 2 < tokenCount) {

                        Token valor = tokens[i + 2];

                        String tipoVar = symbolTable.get(nombreVar);
                        String tipoValor = obtenerTipo(valor);

                        if (tipoVar != null && !tipoVar.equals(tipoValor)) {
                            errores += "Error semántico: tipo incompatible en asignación a '"
                                    + nombreVar + "' en línea "
                                    + t.getLinea() + "\n";
                        }
                    }
                }
            }
        }
    }

    private String obtenerTipo(Token t) {

        // Número literal
        if (t.getId() == 200) {

            if (t.getLexema().contains(".")) {
                return "double";
            } else {
                return "int";
            }
        }

        // Identificador
        if (t.getId() == 201) {
            return symbolTable.getOrDefault(t.getLexema(), "desconocido");
        }

        return "desconocido";
    }
}