package com.potfoliomoedas.portfolio.service.user.impl;

import com.potfoliomoedas.portfolio.component.ConvertToDTO;
import com.potfoliomoedas.portfolio.component.UsuarioLogado;
import com.potfoliomoedas.portfolio.dto.UsuarioRequestDTO;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.dto.VerificarEmailDTO;
import com.potfoliomoedas.portfolio.exception.EmailExistenteException;
import com.potfoliomoedas.portfolio.exception.EmailNullException;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita o Mockito
class UsuarioServiceUserImplTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private ConvertToDTO convertToDTO;

    @Mock
    private EmailService emailService;

    @Mock
    private UsuarioLogado usuarioLogado;

    @InjectMocks
    private UsuarioServiceUserImpl service;

    @Test
    @DisplayName("Deve criar usuário com sucesso quando tudo estiver válido")
    void criarUsuario_CenarioSucesso() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Raphael", "raphael@email.com", "Senha123!");

        Usuario usuarioSalvo = new Usuario();
        usuarioSalvo.setId(1L);
        usuarioSalvo.setEmail("raphael@email.com");

        UsuarioResponseDTO responseEsperado = new UsuarioResponseDTO(1L,
                "Raphael",
                "raphael@email.com",
                new ArrayList<>(),
                false,
                null);

        when(repository.existsByEmail(dto.email())).thenReturn(false);
        when(encoder.encode(dto.senha())).thenReturn("senhaCriptografada");
        when(repository.save(any(Usuario.class))).thenReturn(usuarioSalvo);
        when(convertToDTO.convertUserToUserDTO(any(Usuario.class))).thenReturn(responseEsperado);

        UsuarioResponseDTO resultado = service.criarUsuario(dto);

        assertNotNull(resultado);
        assertEquals("raphael@email.com", resultado.email());
        verify(repository, times(1)).save(any(Usuario.class));
        verify(emailService, times(1)).enviarEmailTexto(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o email já existe")
    void criarUsuario_EmailExistente() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Raphael", "existe@email.com", "Senha123!");
        when(repository.existsByEmail(dto.email())).thenReturn(true);
        assertThrows(EmailExistenteException.class, () -> service.criarUsuario(dto));
        verify(repository, never()).save(any(Usuario.class));
        verify(emailService, never()).enviarEmailTexto(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando email é nulo")
    void criarUsuario_EmailNulo() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Raphael", null, "Senha123!");

        assertThrows(EmailNullException.class, () -> service.criarUsuario(dto));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve verificar email com sucesso quando código e validade estão ok")
    void verificarEmail_Sucesso() {
        String email = "raphael@email.com";
        String codigo = "123456";
        VerificarEmailDTO dto = new VerificarEmailDTO(email, codigo);

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setCodigoVerificacao("123456");
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(10));
        usuario.setVerificado(false);

        when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));
        service.verificarEmail(dto);

        assertTrue(usuario.isVerificado());
        assertNull(usuario.getCodigoVerificacao());
        assertNull(usuario.getValidadeCodigo());

        verify(repository).save(usuario);
    }

    @Test
    @DisplayName("Deve lançar erro quando o código está incorreto")
    void verificarEmail_CodigoIncorreto() {
        VerificarEmailDTO dto = new VerificarEmailDTO("raphael@email.com", "999999");
        Usuario usuario = new Usuario();
        usuario.setCodigoVerificacao("123456");
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(10));

        when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.verificarEmail(dto));
        assertEquals("Código inválido ou expirado.", exception.getMessage());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar erro quando o código expirou (tempo acabou)")
    void verificarEmail_CodigoExpirado() {
        VerificarEmailDTO dto = new VerificarEmailDTO("raphael@email.com", "123456");

        Usuario usuario = new Usuario();
        usuario.setCodigoVerificacao("123456");
        usuario.setValidadeCodigo(LocalDateTime.now().minusMinutes(1));

        when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuario));
        assertThrows(RuntimeException.class, () -> service.verificarEmail(dto));
        assertFalse(usuario.isVerificado());
    }

    @Test
    @DisplayName("Deve excluir o usuário logado com sucesso")
    void excluirUsuario_Sucesso() {
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setEmail("teste@email.com");
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioMock);

        service.excluirUsuario();
        verify(repository, times(1)).delete(usuarioMock);
    }

    @Test
    @DisplayName("Deve atualizar nome e senha com sucesso")
    void atualizarUsuario_NomeESenha() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("Novo Nome", null, "NovaSenha123");
        Usuario usuarioNoBanco = new Usuario();
        usuarioNoBanco.setId(1L);
        usuarioNoBanco.setNome("Nome Antigo");
        usuarioNoBanco.setSenha("SenhaAntigaHash");
        usuarioNoBanco.setEmail("email@teste.com");
        UsuarioResponseDTO responseEsperado = new UsuarioResponseDTO(
                1L, "Novo Nome", "email@teste.com", new ArrayList<>(), false, null
        );
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioNoBanco);
        when(encoder.encode("NovaSenha123")).thenReturn("HashNovaSenha");
        when(convertToDTO.convertUserToUserDTO(usuarioNoBanco)).thenReturn(responseEsperado);

        service.atualizarUsuario(request);
        assertEquals("Novo Nome", usuarioNoBanco.getNome());
        assertEquals("HashNovaSenha", usuarioNoBanco.getSenha());
        verify(repository, times(1)).save(usuarioNoBanco);
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar atualizar para um email que já existe")
    void atualizarUsuario_EmailJaExiste() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(null, "email.ocupado@teste.com", null);

        Usuario usuarioLogadoMock = new Usuario();
        usuarioLogadoMock.setEmail("meu.email@teste.com");

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioLogadoMock);
        when(repository.existsByEmail("email.ocupado@teste.com")).thenReturn(true);
        assertThrows(EmailExistenteException.class, () -> service.atualizarUsuario(request));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar foto de perfil no usuário logado")
    void salvarFoto_Sucesso() throws IOException {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file",
                "minha-foto.jpg",
                "image/jpeg",
                "bytes_da_imagem".getBytes()
        );

        Usuario usuarioLogadoMock = new Usuario();
        usuarioLogadoMock.setId(1L);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioLogadoMock);

        service.salvarFoto(arquivo);
        assertNotNull(usuarioLogadoMock.getFotoPerfil());
        assertArrayEquals("bytes_da_imagem".getBytes(), usuarioLogadoMock.getFotoPerfil());
        verify(repository, times(1)).save(usuarioLogadoMock);
    }

    @Test
    @DisplayName("Deve reenviar código se usuário existe e não está verificado")
    void reenviarCodigo_Sucesso() {
        String email = "raphael@email.com";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome("Raphael");
        usuario.setVerificado(false);

        when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));
        service.reenviarCodigo(email);
        assertNotNull(usuario.getCodigoVerificacao());
        assertNotNull(usuario.getValidadeCodigo());

        verify(repository, times(1)).save(usuario);
        verify(emailService, times(1)).enviarEmailTexto(eq(email), anyString(), anyString());
    }

    @Test
    @DisplayName("Não deve fazer nada se usuário já for verificado")
    void reenviarCodigo_JaVerificado() {
        String email = "raphael@email.com";
        Usuario usuario = new Usuario();
        usuario.setVerificado(true);

        when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

        service.reenviarCodigo(email);

        verify(repository, never()).save(any());
        verify(emailService, never()).enviarEmailTexto(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar reenviar para email inexistente")
    void reenviarCodigo_EmailNaoEncontrado() {

        String email = "fantasma@email.com";
        when(repository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.reenviarCodigo(email));
    }

    @Test
    @DisplayName("Deve retornar os dados do usuário logado corretamente")
    void verUsuario_Sucesso() {
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("Raphael");
        usuarioMock.setEmail("raphael@email.com");
        UsuarioResponseDTO dtoEsperado = new UsuarioResponseDTO(
                1L, "Raphael", "raphael@email.com", new ArrayList<>(), false, null
        );
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioMock);
        when(convertToDTO.convertUserToUserDTO(usuarioMock)).thenReturn(dtoEsperado);
        UsuarioResponseDTO resultado = service.verUsuario();
        assertNotNull(resultado);
        assertEquals("Raphael", resultado.nome());
        assertEquals("raphael@email.com", resultado.email());
        verify(usuarioLogado, times(1)).getUsuarioLogado();
    }
}