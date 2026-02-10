package com.potfoliomoedas.portfolio.service.user;

import com.potfoliomoedas.portfolio.dto.UsuarioRequestDTO;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.dto.VerificarEmailDTO;
import com.potfoliomoedas.portfolio.model.Usuario;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UsuarioServiceUser {

    UsuarioResponseDTO criarUsuario(UsuarioRequestDTO requestDTO);
    void excluirUsuario();
    UsuarioResponseDTO atualizarUsuario(UsuarioRequestDTO requestDTO);
    UsuarioResponseDTO verUsuario();
    void verificarEmail(VerificarEmailDTO dados);
    void reenviarCodigo(String email);
    void salvarFoto(MultipartFile file) throws IOException;
}
