package data;

import java.io.Serializable;

public class Usuario implements Serializable {

    private String nickname;
    private String mensaje;
    private String usuarios;
    int codigoConexion;

    public Usuario(String nickname, String conversacion, String usuarios, int codigoConexion) {
        this.nickname = nickname;
        this.mensaje = conversacion;
        this.usuarios = usuarios;
        this.codigoConexion = codigoConexion;
    }

    public String getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(String usuarios) {
        this.usuarios = usuarios;
    }


    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getCodigoConexion() {
        return codigoConexion;
    }

    public void setCodigoConexion(int codigoConexion) {
        this.codigoConexion = codigoConexion;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "nickname='" + nickname + '\'' +
                ", mensaje='" + mensaje + '\'' +
                ", usuarios='" + usuarios + '\'' +
                ", codigoConexion=" + codigoConexion +
                '}';
    }
}
