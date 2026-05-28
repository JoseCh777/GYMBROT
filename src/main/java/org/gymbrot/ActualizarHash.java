package org.gymbrot;

import org.mindrot.jbcrypt.BCrypt;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Usuario;

public class ActualizarHash {
    public static void main(String[] args) {
        String password = "123";
        String nuevoHash = BCrypt.hashpw(password, BCrypt.gensalt());
        
        System.out.println("Nuevo hash generado: " + nuevoHash);
        
        UsuarioDAO dao = new UsuarioDAO();
        Usuario user = dao.buscarPorNombreOCorreo("cliente@test.com");
        
        if (user != null) {
            System.out.println("Usuario encontrado: " + user.getCorreo());
            
            user.setContrasenaHash(nuevoHash);
            boolean actualizado = dao.actualizar(user);
            System.out.println("¿Actualizado? " + actualizado);
            
            // Verificar que funciona
            Usuario verificado = dao.buscarPorNombreOCorreo("cliente@test.com");
            boolean match = BCrypt.checkpw(password, verificado.getContrasenaHash());
            System.out.println("¿Contraseña válida? " + match);
            
            if (match) {
                System.out.println("✅ TODO OK. Ahora puedes iniciar sesión con: cliente@test.com / 123");
            } else {
                System.out.println("❌ Algo falló en la verificación");
            }
        } else {
            System.out.println("❌ Usuario no encontrado en la base de datos");
        }
    }
}