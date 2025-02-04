package cliente;

import data.Usuario;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;

public class Cliente {
    private JPanel panel1;
    private JTextArea txtChat;
    private JTextArea txtUsuarios;
    private JButton btnSalir;
    private JButton btnEnviar;
    private JTextField txtMensaje;


    private static DatagramSocket cliente;
    private static Usuario u = new Usuario("","","",0);
    private static String nick = "";
    private static int puerto  = 6000;
    private static byte[] datos = new byte[1024];
    public Cliente() {
        btnEnviar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    ByteArrayOutputStream bs=new ByteArrayOutputStream();
                    ObjectOutputStream oos=new ObjectOutputStream(bs);
                    if (!btnEnviar.getText().equals("Enviar")) {
                        if (txtMensaje.getText().isEmpty() || txtMensaje.getText().equals("Introduce tu nick")) {
                            txtChat.append("Escribe un nick, no puede estar vacío\n");
                            txtMensaje.setText("Introduce tu nick");
                        } else {
                            nick = txtMensaje.getText();
                            oos.writeObject(new Usuario(nick,"","",101));
                            oos.close();
                            byte[] datos=bs.toByteArray();
                            DatagramPacket envio=new DatagramPacket(datos,datos.length, InetAddress.getByName("localhost"),puerto);
                            cliente.send(envio);
                            txtMensaje.setText("");
                        }
                    } else {
                        if (txtMensaje.getText().isEmpty()) {
                            txtMensaje.setText("No puedes dejarme vacío");
                        } else {
                            oos.writeObject(new Usuario(nick,txtMensaje.getText(),"",102));
                            oos.close();
                            byte[] datos=bs.toByteArray();
                            DatagramPacket envio=new DatagramPacket(datos,datos.length, InetAddress.getByName("localhost"),puerto);
                            cliente.send(envio);
                            txtMensaje.setText("");
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        txtMensaje.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (txtMensaje.getText().equals("Introduce tu nick") || txtMensaje.getText().equals("No puedes dejarme vacío")) {
                    txtMensaje.setText("");
                }
            }
        });

        btnSalir.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    ByteArrayOutputStream bs=new ByteArrayOutputStream();
                    ObjectOutputStream oos=new ObjectOutputStream(bs);

                    oos.writeObject(new Usuario(nick,"","",103));
                    oos.close();
                    byte[] datos=bs.toByteArray();
                    DatagramPacket envio=new DatagramPacket(datos,datos.length, InetAddress.getByName("localhost"),puerto);
                    cliente.send(envio);
                    txtMensaje.setText("");

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        JFrame frame = new JFrame("Chat UDP");
        Cliente chat = new Cliente();
        frame.setContentPane(chat.panel1);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Manejar el cierre de la ventana
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    ByteArrayOutputStream bs=new ByteArrayOutputStream();
                    ObjectOutputStream oos=new ObjectOutputStream(bs);
                    if (chat.btnEnviar.isEnabled()) {
                        oos.writeObject(new Usuario(nick,"","",103));
                        oos.close();
                        byte[] datos=bs.toByteArray();
                        DatagramPacket envio=new DatagramPacket(datos,datos.length, InetAddress.getByName("localhost"),puerto);
                        cliente.send(envio);
                    }
                    cliente.close();

                    System.exit(0);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.pack();
        frame.setVisible(true);

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bs);

        cliente = new DatagramSocket();
        // Solicitud de conexión
        oos.writeObject(new Usuario("","","",0));
        oos.close();
        datos=bs.toByteArray();
        DatagramPacket envio=new DatagramPacket(datos,datos.length, InetAddress.getByName("localhost"),puerto);
        cliente.send(envio);

        try {

            boolean flag = true;

            while (flag) {

                bs = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bs);
                byte[] datosEntrada = new byte[1024];
                DatagramPacket recibido=new DatagramPacket(datosEntrada,datosEntrada.length);
                cliente.receive(recibido);
                ByteArrayInputStream bais = new ByteArrayInputStream(datosEntrada);
                ObjectInputStream ois = new ObjectInputStream(bais);

                u = (Usuario)ois.readObject();
                int codigo = u.getCodigoConexion();
                System.out.println("recibo " + u);
                switch (codigo) {
                    case 200 -> { // Mensaje válido recibido
                        chat.txtChat.append(u.getMensaje()); // Mostramos el mensaje en el chat
                        chat.txtUsuarios.setText(u.getUsuarios()); // Lista de usuarios conectados
                        chat.btnEnviar.setText("Enviar");
                    }
                    case 401 -> { // Petición de nick
                        chat.txtChat.append(u.getMensaje());
                        chat.txtUsuarios.setText(u.getUsuarios());
                        chat.txtMensaje.setText("Introduce tu nick");
                        nick = "";
                    }
                    case 601 -> { // Desconexión
                        chat.txtChat.append(u.getMensaje());
                        chat.txtUsuarios.setText(u.getUsuarios());
                        cliente.close();
                        chat.btnEnviar.setEnabled(false);
                        chat.btnSalir.setEnabled(false);
                        chat.txtMensaje.setEnabled(false);
                    }
                }
            }
        } catch (SocketException ex) {
            System.out.println(ex.getMessage());
        }


    }


}
