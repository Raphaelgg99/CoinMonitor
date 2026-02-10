package com.potfoliomoedas.portfolio.service.user.impl;

import com.potfoliomoedas.portfolio.component.ConvertToDTO;
import com.potfoliomoedas.portfolio.component.UsuarioLogado;
import com.potfoliomoedas.portfolio.dto.UsuarioRequestDTO;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.dto.VerificarEmailDTO;
import com.potfoliomoedas.portfolio.exception.*;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.EmailService;
import com.potfoliomoedas.portfolio.service.user.UsuarioServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Service
public class UsuarioServiceUserImpl implements UsuarioServiceUser {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private UsuarioLogado usuarioLogado;

    @Autowired
    private ConvertToDTO convertToDTO;

    @Autowired
    private EmailService emailService;

    @Override
    public UsuarioResponseDTO criarUsuario(UsuarioRequestDTO requestDTO){
        if (requestDTO.email() == null || requestDTO.email().isBlank()){
            throw new EmailNullException("Favor colocar um email");
        }
        if(requestDTO.nome() == null || requestDTO.nome().isBlank()){
            throw new NomeNullException("Favor colocar um nome");
        }
        if(requestDTO.senha() == null || requestDTO.senha().isBlank()){
            throw new SenhaNullException("Favor colocar uma senha");
        }

        if(usuarioRepository.existsByEmail(requestDTO.email())){
            throw new EmailExistenteException("Esse email já existe");
        }
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(requestDTO.nome());
        novoUsuario.setEmail(requestDTO.email());
        novoUsuario.setRoles(new ArrayList<>(List.of("USER")));
        novoUsuario.setSenha(encoder.encode(requestDTO.senha()));
        novoUsuario.setVerificado(false); // Inativo
        gerarEEnviarCodigo(novoUsuario);
        // 3. Gera o Código
        return convertToDTO.convertUserToUserDTO(novoUsuario);
    }

    public void salvarFoto(MultipartFile file) throws IOException {
        Usuario usuario = usuarioLogado.getUsuarioLogado();
        // Converte o arquivo recebido para bytes e salva no usuario
        usuario.setFotoPerfil(file.getBytes());
        usuarioRepository.save(usuario);
    }

    private void gerarEEnviarCodigo(Usuario usuario){
        String codigo = String.valueOf(new Random().nextInt(900000) + 100000);

        // Atualiza o usuário
        usuario.setCodigoVerificacao(codigo);
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(15));

        // Salva no banco (Importante salvar antes de enviar)
        usuarioRepository.save(usuario);

        // Envia o Email (Assíncrono)
        emailService.enviarEmailTexto(
                usuario.getEmail(),
                "Código de Verificação - CoinMonitor",
                "Olá " + usuario.getNome() + ",\n\nSeu código de verificação é: " + codigo
        );
    }

    @Override
    public void reenviarCodigo(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email não encontrado"));;
        if (usuario.isVerificado()) {
            return;
        }
        gerarEEnviarCodigo(usuario);
    }


    @Override
    public void verificarEmail(VerificarEmailDTO dados) {
        Usuario usuario = usuarioRepository.findByEmail(dados.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (usuario.isVerificado()) {
            return;
        }

        // 👇 AJUSTE AQUI: Adicione o .trim() para evitar espaços invisíveis
        if (usuario.getCodigoVerificacao().equals(dados.codigo().trim()) &&
                LocalDateTime.now().isBefore(usuario.getValidadeCodigo())) {

            usuario.setVerificado(true);
            usuario.setCodigoVerificacao(null);
            usuario.setValidadeCodigo(null);

            usuarioRepository.save(usuario);
        } else {
            // Dica de Debug: Se cair aqui, o print vai te mostrar no console quem é quem
            System.out.println("ERRO VERIFICACAO: Banco[" + usuario.getCodigoVerificacao() + "] vs Digitado[" + dados.codigo() + "]");
            System.out.println("Validade: " + usuario.getValidadeCodigo() + " vs Agora: " + LocalDateTime.now());

            throw new RuntimeException("Código inválido ou expirado.");
        }
    }

    @Override
    public void excluirUsuario(){
        Usuario usuario = usuarioLogado.getUsuarioLogado();
        usuarioRepository.delete(usuario);
    }

    @Override
    public UsuarioResponseDTO atualizarUsuario(UsuarioRequestDTO requestDTO){
        Usuario usuario = usuarioLogado.getUsuarioLogado();
        if(requestDTO.nome() != null && !requestDTO.nome().isBlank()) {
            usuario.setNome(requestDTO.nome());
        }
        if(requestDTO.email() != null && !requestDTO.email().isBlank()
                && !requestDTO.email().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(requestDTO.email())) {
                throw new EmailExistenteException("Esse email já existe");
            }
            usuario.setEmail(requestDTO.email());
        }
        if(requestDTO.senha() != null && !requestDTO.senha().isBlank()) {
            usuario.setSenha(encoder.encode(requestDTO.senha()));
        }
        usuarioRepository.save(usuario);
        return convertToDTO.convertUserToUserDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO verUsuario() {
        Usuario usuario = usuarioLogado.getUsuarioLogado();
        return convertToDTO.convertUserToUserDTO(usuario);
    }


}
