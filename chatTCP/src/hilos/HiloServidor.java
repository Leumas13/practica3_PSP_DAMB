package hilos;

import data.Conversacion;
import data.Usuario;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class HiloServidor extends Thread {

    private Socket cliente;
    private ArrayList<String> texto;
    private HashMap<String, HiloServidor> lista;
    private String usuariosConectados = "";
    private Conversacion c;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private int penalizacion = 0;

    public HiloServidor(Socket cliente, HashMap<String, HiloServidor> lista, Conversacion c) {
        this.cliente = cliente;
        this.lista = lista;
        this.c = c;
    }

    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(cliente.getOutputStream());
            ois = new ObjectInputStream(cliente.getInputStream());

            boolean flag = true;
            Usuario aux = (Usuario) ois.readObject();
            while (flag) {
                int codigo = aux.getCodigoConexion();
                String mensaje = aux.getMensaje();
                String nick = aux.getNickname();
                String hora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm"));

                String inicioMensaje = hora + " " + nick + "-> ";
                String mensajeFormateado = hora + " " + nick + "-> " + mensaje + "\n";

                switch (codigo) {
                    //apertura de conexion
                    case 0 -> {
                        //recuperar la lista de usuarios en cadena de texto para enviar
                        listaUsuarios();
                        Usuario primeraConexion = new Usuario("","",usuariosConectados,401);
                        oos.writeObject(primeraConexion);
                        oos.flush();
                        oos.reset();
                    }
                    //solicitud de nick
                    case 101 -> {
                        //recuperar la lista de usuarios en cadena de texto para enviar
                        listaUsuarios();

                        //nick aceptado y agregado a la lista de nicks - nick aceptado
                        if (!lista.containsKey(nick)) {
                             /*Primera conexion, eso implica:
                            - Incluir el hilo a la lista de difusion
                            - Informar al resto de usuarios
                            - Actualizar la lista de usuarios a todos los usuarios
                            - Dar bienvenida al usuario nuevo
                            - Cargar los ultimos 5 mensajes de la conversacion
                            - Realizar el registro en objeto compartido Conversacion
                             */

                            lista.put(nick,this);
                            listaUsuarios();

                            //Registrar en el historial
                            synchronized (c) {
                                //recorrer los hilos para actualizar la info
                                lista.forEach((k, v) -> {
                                    String bienvenida;
                                    try {
                                        //si el usuario es el nuevo recibe un mensaje diferente al resto
                                        if (k.equals(nick)) {
                                            bienvenida = c.primeraConexion() + inicioMensaje + "Bienvenid@ al chat\n";
                                        } else {
                                            bienvenida = inicioMensaje + "se ha unido al chat\n";
                                        }
                                        Usuario info = new Usuario(k,bienvenida,usuariosConectados,200);
                                        v.oos.writeObject(info);
                                        v.oos.flush();
                                        v.oos.reset();
                                        c.registrar(inicioMensaje + "primera conexion");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            }

                            //el nick ya esta ocupado
                        } else {
                            try {
                                Usuario info = new Usuario("","El nick esta ocupado, por favor ingresa uno nuevo",usuariosConectados,401);
                                oos.writeObject(info);
                                oos.flush();
                                oos.reset();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }
                    //solicitud de enviar mensaje
                    case 102 -> {

                            /*Usuario quiere mandar un mensaje, eso implica:
                            - Informar al resto de usuarios
                            - Realizar el registro en objeto compartido Conversacion
                            - Valorar que no se incumplen las normas
                             */
                        //cargamos la lista de usuarios
                        listaUsuarios();
                        synchronized (c) {
                            if (penalizarVocabulario(mensaje)) {
                                penalizacion++;
                                //si pasa de las 3 faltas se expulsa de la sala
                                if (penalizacion > 3) {
                                    //comunicar a los usuarios que ha sido expulsado
                                    lista.forEach((k, v) -> {
                                        String enviarMensaje;
                                        int codigoInfo;
                                        try {
                                            //si el usuario es el expulsado recibe un mensaje diferente al resto
                                            if (k.equals(nick)) {
                                                enviarMensaje = inicioMensaje + "te pasaste, quedas expulsado del chat\n";
                                                codigoInfo = 601;
                                                c.registrar(inicioMensaje + "EXPULSADO\n");
                                            } else {
                                                enviarMensaje = inicioMensaje + "EXPULSADO por mala conducta\n";
                                                codigoInfo = 200;
                                            }
                                            Usuario info = new Usuario(k,enviarMensaje,usuariosConectados,codigoInfo);
                                            v.oos.writeObject(info);
                                            v.oos.flush();
                                            v.oos.reset();
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });

                                } else {
                                    //comunicar a los usuarios que ha sido penalizado
                                    lista.forEach((k, v) -> {
                                        String enviarMensaje;
                                        try {
                                            //si el usuario es el penalizado se le informa de las faltas que tiene
                                            if (k.equals(nick)) {
                                                enviarMensaje = inicioMensaje + "CUIDADO tienes " + penalizacion + " faltas, con +3 te EXPULSAMOS\n";
                                                c.registrar(inicioMensaje +penalizacion +" FALTA\n");
                                            } else {
                                                enviarMensaje = inicioMensaje + "FALTA DE CONDUCTA\n";
                                            }
                                            Usuario info = new Usuario(k,enviarMensaje,usuariosConectados,200);
                                            v.oos.writeObject(info);
                                            v.oos.flush();
                                            v.oos.reset();
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }

                                //Mensaje correcto
                            } else {
                                lista.forEach((k, v) -> {
                                    try {
                                        c.registrar(mensajeFormateado);
                                        Usuario info = new Usuario(k,mensajeFormateado,usuariosConectados,200);
                                        v.oos.writeObject(info);
                                        v.oos.flush();
                                        v.oos.reset();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            }
                        }
                    }
                    //baja de usuario voluntaria
                    case 103 -> {
                        //recuperar la lista de usuarios en cadena de texto para enviar
                        lista.remove(nick);
                        listaUsuarios();
                        System.out.println("desconectado "+lista);

                        lista.forEach((k, v) -> {
                            try {
                                c.registrar(mensajeFormateado);
                                Usuario info = new Usuario(k,inicioMensaje + " se ha desconectado\n",usuariosConectados,200);
                                v.oos.writeObject(info);
                                v.oos.flush();
                                v.oos.reset();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        Usuario info = new Usuario(nick,inicioMensaje + " Hasta Pronto!",usuariosConectados,601);
                        oos.writeObject(info);
                        oos.flush();
                        oos.reset();
                    }
                    //Salida abrupta
                    case 104 ->{
                        lista.remove(nick);
                        listaUsuarios();
                        System.out.println("desconectado "+lista);

                        lista.forEach((k, v) -> {
                            try {
                                c.registrar(mensajeFormateado);
                                Usuario info = new Usuario(k,inicioMensaje + " se ha desconectado\n",usuariosConectados,200);
                                v.oos.writeObject(info);
                                v.oos.flush();
                                v.oos.reset();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                //actualizar lista de hilos
                comprobarActividad();
                aux = (Usuario) ois.readObject();
            }
            cliente.close();

        } catch (IOException e) {
            System.out.println("cierre sesion de cliente");;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void listaUsuarios() {
        usuariosConectados = "";
        if (!lista.isEmpty()) {
            lista.forEach((k,v) -> {
                usuariosConectados = usuariosConectados + k + "\n";
            });
        }
    }

    public boolean penalizarVocabulario(String mensaje) {
        // Normalizar el texto para eliminar acentos
        String quitarAcentos = Normalizer.normalize(mensaje, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String minusculas = quitarAcentos.toLowerCase();

        // Eliminar caracteres especiales, dejando solo letras, números y espacios
        String mensajeLimpio = minusculas.replaceAll("[^a-zA-Z0-9 ]", "");

        // Dividir el texto en un array por espacios
        String[] lista = mensajeLimpio.trim().split("\\s+");

        HashSet<String> insultos = new HashSet<>();
        insultos.add("imbecil");
        insultos.add("subnormal");
        insultos.add("puta");
        insultos.add("capullo");
        insultos.add("gilipollas");
        insultos.add("cabron");

        for (String s : lista) {
            if (insultos.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public void comprobarActividad(){
        ArrayList<String> eliminar = new ArrayList<>();
        lista.forEach((k,v)-> {
            if(!v.isAlive()){
                eliminar.add(k);
            }
        });

        eliminar.forEach(k ->{
            lista.remove(k);
        });
    }
}
