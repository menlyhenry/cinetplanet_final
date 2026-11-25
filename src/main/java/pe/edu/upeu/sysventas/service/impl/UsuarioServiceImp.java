package pe.edu.upeu.sysventas.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.UsuarioRepository;
import pe.edu.upeu.sysventas.service.IUsuarioService;
import pe.edu.upeu.sysventas.util.PasswordUtil;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class UsuarioServiceImp extends CrudGenericoServiceImp<Usuario, Long> implements IUsuarioService {
    private final UsuarioRepository usuarioRepository;
    private boolean sembrado;

    @Override
    protected ICrudGenericoRepository<Usuario, Long> getRepo() {
        return usuarioRepository;
    }

    @Override
    public List<Usuario> findAll() {
        sembrar();
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario save(Usuario usuario) {
        sembrar();
        boolean repetido = false;
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getUsuario() != null && u.getUsuario().equalsIgnoreCase(usuario.getUsuario())) {
                repetido = true;
            }
        }
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre: " + usuario.getUsuario());
        }
        usuario.setClave(PasswordUtil.encriptar(usuario.getClave()));
        return super.save(usuario);
    }

    @Override
    public Usuario update(Long id, Usuario usuario) {
        Usuario actual = findById(id);
        boolean repetido = false;
        for (Usuario u : usuarioRepository.findAll()) {
            if (!id.equals(u.getIdUsuario()) && u.getUsuario() != null && u.getUsuario().equalsIgnoreCase(usuario.getUsuario())) {
                repetido = true;
            }
        }
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre: " + usuario.getUsuario());
        }
        if (usuario.getClave() == null || usuario.getClave().isBlank()) {
            usuario.setClave(actual.getClave());
        } else {
            usuario.setClave(PasswordUtil.encriptar(usuario.getClave()));
        }
        return super.update(id, usuario);
    }

    @Override
    public Optional<Usuario> loginUsuario(String usuario, String clave) {
        sembrar();
        return usuarioRepository.findByUsuarioAndClave(usuario, clave);
    }

    private void sembrar() {
        if (!sembrado) {
            sembrado = true;
            if (usuarioRepository.findAll().isEmpty()) {
                usuarioRepository.seedData();
            }
        }
    }
}
