package com.potfoliomoedas.portfolio.component;

import com.potfoliomoedas.portfolio.dto.MoedaDTO;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.model.Usuario;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConvertToDTO {

    public UsuarioResponseDTO convertUserToUserDTO(Usuario usuario) {
        List<MoedaDTO> carteiraDTO = usuario.getCarteira().stream()
                .map(moeda -> new MoedaDTO(
                        moeda.getCoinId(),
                        moeda.getQuantidade()
                ))
                .collect(Collectors.toList());

        // 2. Verifica se é Google (Mantido igual)
        boolean isGoogle = "GOOGLE_AUTH_EXTERNO".equals(usuario.getSenha());

        // 3. Prepara a Foto (A CORREÇÃO ESTÁ AQUI) 👇
        String fotoBase64 = null;
        if (usuario.getFotoPerfil() != null) {
            fotoBase64 = Base64.getEncoder().encodeToString(usuario.getFotoPerfil());
        }

        // 4. Retorna o DTO com tudo pronto
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                carteiraDTO,
                isGoogle,
                fotoBase64 // <--- Passa a variável pronta aqui
        );
    }
}
