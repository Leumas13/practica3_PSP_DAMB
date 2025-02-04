package servidor;

import data.Conversacion;
import data.Usuario;
import hilos.HiloServidor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Servidor {

    public static void main(String[] args) {
        HashMap<String, HiloServidor> lista = new HashMap<>();
        Conversacion c = new Conversacion();


        try {
            int puerto=6000;
            ServerSocket servidor=new ServerSocket(6000);

            while(true) {
                Socket cliente = servidor.accept();
                HiloServidor m = new HiloServidor(cliente, lista,c);
                m.start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
