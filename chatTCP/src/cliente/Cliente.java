package cliente;

import data.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.Buffer;
import java.util.Scanner;

public class Cliente {
    private JPanel panel1;
    private JTextArea txtChat;
    private JTextArea txtUsuarios;
    private JButton btnSalir;
    private JButton btnEnviar;
    private JTextField txtMensaje;


static Socket cliente;
    static String codigo;
    static ObjectOutputStream oos;
    static Usuario u;

    public Cliente() {
        btnEnviar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                if (!btnEnviar.getText().equals("Enviar")) {
                    //usuario sin nickname
                    //valorar que el textbox este relleno
                    if (txtMensaje.getText().isEmpty() || txtMensaje.getText().equals("Introduce tu nick")) {
                        txtChat.append("Escribe un nick, no puede estar vacio\n");
                        txtMensaje.setText("Introduce tu nick");
                    } else {
                        Usuario solicitarNick = new Usuario(txtMensaje.getText(), "", "", 101);
                        oos.writeObject(solicitarNick);
                        oos.flush();
                        oos.reset();
                        txtMensaje.setText("");
                    }
                } else {
                    //usuario con codigo OK
                    //valorar que el textbox este relleno
                    if (txtMensaje.getText().isEmpty()) {
                        txtMensaje.setText("No puedes dejarme vacio");
                    } else {
                        Usuario mensajeUsuario = new Usuario(u.getNickname(), txtMensaje.getText(), "", 102);
                        oos.writeObject(mensajeUsuario);
                        oos.flush();
                        oos.reset();
                        txtMensaje.setText("");
                    }
                }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });


        txtMensaje.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (txtMensaje.getText().equals("Introduce tu nick") || txtMensaje.getText().equals("No puedes dejarme vacio")) {
                    txtMensaje.setText("");
                }
            }
        });
        btnSalir.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Usuario mensajeUsuario = new Usuario(u.getNickname(), "", "", 103);
                    oos.writeObject(mensajeUsuario);
                    oos.flush();
                    oos.reset();

                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {
        JFrame frame = new JFrame("Chat TCP");
        Cliente chat = new Cliente();
        frame.setContentPane(chat.panel1);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);


        //listener para controlar la salida
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    //Si el boton no esta activo, el usuariio cerro sesion con el boton salir y solo queda cerrar la app
                    if(chat.btnEnviar.isEnabled()) {
                        Usuario mensajeUsuario = new Usuario(u.getNickname(), "", "", 104);
                        oos.writeObject(mensajeUsuario);
                        oos.flush();
                        oos.reset();

                        oos.close();
                        cliente.close();
                    }
                    System.exit(0);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.pack();
        frame.setVisible(true);


        cliente = new Socket("localhost", 6000);

        oos = new ObjectOutputStream(cliente.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());

        //solicitud de conexion, devuleve la lista de usuarios para elegir un nick, pero no devuelve conversacion
        oos.writeObject(new Usuario("","","",0));
        try {
        boolean flag = true;
        while(flag) {
                Usuario aux = (Usuario) ois.readObject();
                u = aux;
                int codigo = u.getCodigoConexion();
                System.out.println(u);
                switch (codigo) {
                    //Conectado a chat
                    case 200 -> {
                        chat.txtChat.append(u.getMensaje());
                        chat.txtUsuarios.setText(u.getUsuarios());
                        chat.btnEnviar.setText("Enviar");
                    }
                    //Conectado a servidor sin entrar en chat por falta de nick, el boton no cambia
                    case 401 -> {
                        chat.txtChat.append(u.getMensaje());
                        chat.txtUsuarios.setText(u.getUsuarios());
                        chat.txtMensaje.setText("Introduce tu nick");
                    }
                    //salir o expulsion
                    case 601 -> {
                        chat.txtChat.append(u.getMensaje());
                        chat.txtUsuarios.setText(u.getUsuarios());

                        oos.close();
                        ois.close();
                        cliente.close();
                        flag = false;
                    }
                }
            }
        }catch(SocketException ex){
            System.out.println("cierre abrupto");
        }
        chat.btnEnviar.setEnabled(false);
        chat.btnSalir.setEnabled(false);
        chat.txtMensaje.setEnabled(false);

    }
}

