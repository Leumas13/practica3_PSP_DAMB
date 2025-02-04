package data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Conversacion {

    private ArrayList<String> historialApp = new ArrayList<>();

    public Conversacion() {
        historialApp.add("");
        historialApp.add("");
        historialApp.add("");
        historialApp.add("");
        historialApp.add("");
    }

    public void registrar(String mensaje) {
        this.historialApp.add(mensaje);
    }

    public ArrayList<String> getHistorialApp() {
        return historialApp;
    }

    public String primeraConexion() {
        String historico = "";
        if(!historialApp.get(historialApp.size() - 5).isEmpty()) {
            historico = historico + (historialApp.get(historialApp.size() - 5));
        }
        if(!historialApp.get(historialApp.size() - 4).isEmpty()) {
            historico = historico + (historialApp.get(historialApp.size() - 4));
        }
        if(!historialApp.get(historialApp.size() - 3).isEmpty()) {
            historico = historico + (historialApp.get(historialApp.size() - 3));
        }
        if(!historialApp.get(historialApp.size() - 2).isEmpty()) {
            historico = historico + (historialApp.get(historialApp.size() - 2));
        }
        if(!historialApp.get(historialApp.size() - 1).isEmpty()) {
            historico= historico + (historialApp.get(historialApp.size() - 1));
        }
        return historico;
    }

}
