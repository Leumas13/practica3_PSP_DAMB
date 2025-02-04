package servidor;

import data.Conversacion;
import data.Usuario;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Servidor {
    static int puerto = 6000;
    static ArrayList<DatagramPacket> listaPaquetes = new ArrayList<>();
    static ArrayList<String> listaNicks = new ArrayList<>();
    static String usuariosConectados = "";
    static Conversacion c = new Conversacion();

    public static void main(String[] args) {


        try {
            DatagramSocket servidor = new DatagramSocket(puerto);
            while (true) {
                byte[] datos = new byte[1024];
                DatagramPacket recibido = new DatagramPacket(datos, datos.length);
                servidor.receive(recibido);

                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bs);

                ByteArrayInputStream bais = new ByteArrayInputStream(datos);
                ObjectInputStream ois = new ObjectInputStream(bais);

                Usuario u = (Usuario) ois.readObject();
                int codigo = u.getCodigoConexion();
                String mensaje = u.getMensaje();
                String nick = u.getNickname();
                String hora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm"));

                String inicioMensaje = hora + " " + nick + "-> ";
                String mensajeFormateado = hora + " " + nick + "-> " + mensaje + "\n";

                switch (codigo) {
                    //apertura de conexion
                    case 0 -> {
                        //recuperar la lista de usuarios en cadena de texto para enviar
                        listaUsuarios();

                        Usuario primeraConexion = new Usuario("", "", "", 401);
                        oos.writeObject(primeraConexion);
                        oos.close();
                        datos = bs.toByteArray();
                        DatagramPacket envio = new DatagramPacket(datos, datos.length, recibido.getAddress(), recibido.getPort());
                        servidor.send(envio);
                    }
                    //solicitud de nick
                    case 101 -> {
                        //recuperar la lista de usuarios en cadena de texto para enviar
                        listaUsuarios();

                        //nick aceptado y agregado a la lista de nicks - nick aceptado
                        if (!listaNicks.contains(u.getNickname())) {
                             /*Primera conexion, eso implica:
                            - Incluir el hilo a la lista de difusion
                            - Informar al resto de usuarios
                            - Actualizar la lista de usuarios a todos los usuarios
                            - Dar bienvenida al usuario nuevo
                            - Cargar los ultimos 5 mensajes de la conversacion
                            - Realizar el registro en objeto compartido Conversacion
                             */

                            listaNicks.add(u.getNickname());
                            listaPaquetes.add(recibido);
                            listaUsuarios();

                                //recorrer los hilos para actualizar la info
                               for(int i = 0; i< listaPaquetes.size();i++){
                                   String bienvenida;
                                   try {
                                       if(listaNicks.get(i).equals(u.getNickname())){
                                           bienvenida = c.primeraConexion() + inicioMensaje + "Bienvenid@ al chat\n";
                                       }else {
                                           bienvenida = inicioMensaje + "se ha unido al chat\n";
                                       }
                                       System.out.println(bienvenida);
                                       Usuario info = new Usuario(listaNicks.get(i), bienvenida, usuariosConectados, 200);
                                       oos.writeObject(info);
                                       oos.close();
                                       datos = bs.toByteArray();
                                       DatagramPacket envio = new DatagramPacket(datos, datos.length, listaPaquetes.get(i).getAddress(), listaPaquetes.get(i).getPort());
                                       servidor.send(envio);
                                       c.registrar(inicioMensaje + "primera conexion");
                                   } catch (IOException e) {
                                       throw new RuntimeException(e);
                                   }
                            }

                            //el nick ya esta ocupado
                        } else {
                            try {
                                Usuario info = new Usuario("", "El nick esta ocupado, por favor ingresa uno nuevo", usuariosConectados, 401);
                                oos.writeObject(info);
                                oos.close();
                                datos = bs.toByteArray();
                                DatagramPacket envio = new DatagramPacket(datos, datos.length, recibido.getAddress(), recibido.getPort());
                                servidor.send(envio);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }
                    //solicitud de enviar mensaje
                    case 102 -> {
                        for(int i = 0; i < listaPaquetes.size(); i++) {
                            try {
                                c.registrar(mensajeFormateado);
                                Usuario info = new Usuario(listaNicks.get(i), mensajeFormateado, usuariosConectados, 200);
                                oos.writeObject(info);
                                oos.close();
                                datos = bs.toByteArray();
                                DatagramPacket envio = new DatagramPacket(datos, datos.length, listaPaquetes.get(i).getAddress(), listaPaquetes.get(i).getPort());
                                servidor.send(envio);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    //baja de usuario voluntaria
                    case 103 -> {
                        //recuperar la lista de usuarios en cadena de texto para enviar
                        int posi = listaNicks.indexOf(nick);
                        int posi2 = listaPaquetes.indexOf(recibido);
                        Usuario info1 = new Usuario(listaNicks.get(posi), inicioMensaje + " Hasta Pronto!", usuariosConectados, 601);
                        oos.writeObject(info1);
                        oos.close();
                        datos = bs.toByteArray();
                        DatagramPacket envio1 = new DatagramPacket(datos, datos.length, recibido.getAddress(), recibido.getPort());
                        servidor.send(envio1);

                        listaNicks.remove(posi);
                        listaPaquetes.remove(posi2);

                        listaUsuarios();

                        for(int i = 0; i < listaPaquetes.size(); i++) {
                            try {
                                c.registrar(mensajeFormateado);
                                Usuario info = new Usuario(listaNicks.get(i), inicioMensaje + " se ha desconectado\n", usuariosConectados, 200);
                                oos.writeObject(info);
                                oos.close();
                                datos = bs.toByteArray();
                                DatagramPacket envio = new DatagramPacket(datos, datos.length, listaPaquetes.get(i).getAddress(), listaPaquetes.get(i).getPort());
                                servidor.send(envio);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    //Salida abrupta
                    case 104 -> {
                        int posi = listaNicks.indexOf(nick);
                        listaNicks.remove(posi);
                        listaPaquetes.remove(posi);
                        listaUsuarios();

                        for(int i = 0; i < listaPaquetes.size(); i++) {
                            try {
                                c.registrar(mensajeFormateado);
                                Usuario info = new Usuario(listaNicks.get(i), inicioMensaje + " se ha desconectado\n", usuariosConectados, 200);
                                oos.writeObject(info);
                                oos.close();
                                datos = bs.toByteArray();
                                DatagramPacket envio = new DatagramPacket(datos, datos.length, listaPaquetes.get(i).getAddress(), listaPaquetes.get(i).getPort());
                                servidor.send(envio);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }

        } catch (
                IOException e) {
            System.out.println("cierre sesion de cliente");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void listaUsuarios() {
        usuariosConectados = "";
        if (!listaNicks.isEmpty()) {
            listaNicks.forEach(i -> {
                usuariosConectados = usuariosConectados + i + "\n";
            });
        }
    }
}


