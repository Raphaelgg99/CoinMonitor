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

    @Mock // Cria uma versão falsa do Repository
    private UsuarioRepository repository;

    @Mock // Cria uma versão falsa do Encoder
    private BCryptPasswordEncoder encoder;

    @Mock // Cria uma versão falsa do Converter
    private ConvertToDTO convertToDTO;

    @Mock // Cria uma versão falsa do EmailService
    private EmailService emailService;

    @Mock
    private UsuarioLogado usuarioLogado;

    // A classe que estamos testando de verdade
    // O @InjectMocks injeta os mocks acima dentro dela automaticamente
    @InjectMocks
    private UsuarioServiceUserImpl service;

    @Test
    @DisplayName("Deve criar usuário com sucesso quando tudo estiver válido")
    void criarUsuario_CenarioSucesso() {
        // 1. ARRANGE (Preparação)
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Raphael", "raphael@email.com", "Senha123!");

        Usuario usuarioSalvo = new Usuario();
        usuarioSalvo.setId(1L);
        usuarioSalvo.setEmail("raphael@email.com");

        UsuarioResponseDTO responseEsperado = new UsuarioResponseDTO(1L,
                "Raphael",
                "raphael@email.com",
                new ArrayList<>(), // carteira vazia
                false,             // isGoogleAccount
                null);

        // Ensinando os Mocks a se comportarem
        when(repository.existsByEmail(dto.email())).thenReturn(false); // Email não existe
        when(encoder.encode(dto.senha())).thenReturn("senhaCriptografada");
        when(repository.save(any(Usuario.class))).thenReturn(usuarioSalvo); // Simula o save
        when(convertToDTO.convertUserToUserDTO(any(Usuario.class))).thenReturn(responseEsperado);

        // 2. ACT (Ação)
        UsuarioResponseDTO resultado = service.criarUsuario(dto);

        // 3. ASSERT (Verificação)
        assertNotNull(resultado);
        assertEquals("raphael@email.com", resultado.email());

        // Verifica se o save foi chamado 1 vez (Garantiu que salvou no banco)
        verify(repository, times(1)).save(any(Usuario.class));

        // Verifica se o email foi enviado (dentro do método privado gerarEEnviarCodigo)
        verify(emailService, times(1)).enviarEmailTexto(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o email já existe")
    void criarUsuario_EmailExistente() {
        // ARRANGE
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Raphael", "existe@email.com", "Senha123!");

        // Simulamos que o banco disse "Sim, existe"
        when(repository.existsByEmail(dto.email())).thenReturn(true);

        // ACT & ASSERT
        // Verifica se a exceção correta é lançada
        assertThrows(EmailExistenteException.class, () -> service.criarUsuario(dto));

        // Garante que NUNCA tentou salvar se o email já existe
        verify(repository, never()).save(any(Usuario.class));
        // Garante que NUNCA tentou enviar email
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
        // ARRANGE
        String email = "raphael@email.com";
        String codigo = "123456";
        VerificarEmailDTO dto = new VerificarEmailDTO(email, codigo);

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setCodigoVerificacao("123456");
        // Define validade para o futuro (Daqui a 10 min)
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(10));
        usuario.setVerificado(false);

        when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

        // ACT
        service.verificarEmail(dto);

        // ASSERT
        assertTrue(usuario.isVerificado()); // Deve virar true
        assertNull(usuario.getCodigoVerificacao()); // Deve limpar o código
        assertNull(usuario.getValidadeCodigo()); // Deve limpar a data

        verify(repository).save(usuario); // Deve salvar no banco
    }

    @Test
    @DisplayName("Deve lançar erro quando o código está incorreto")
    void verificarEmail_CodigoIncorreto() {
        // ARRANGE
        VerificarEmailDTO dto = new VerificarEmailDTO("raphael@email.com", "999999"); // Código errado

        Usuario usuario = new Usuario();
        usuario.setCodigoVerificacao("123456"); // Código certo no banco
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(10));

        when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuario));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.verificarEmail(dto));
        assertEquals("Código inválido ou expirado.", exception.getMessage());

        verify(repository, never()).save(any()); // Não deve salvar nada
    }

    @Test
    @DisplayName("Deve lançar erro quando o código expirou (tempo acabou)")
    void verificarEmail_CodigoExpirado() {
        // ARRANGE
        VerificarEmailDTO dto = new VerificarEmailDTO("raphael@email.com", "123456");

        Usuario usuario = new Usuario();
        usuario.setCodigoVerificacao("123456");
        // Define validade para o PASSADO (1 minuto atrás)
        usuario.setValidadeCodigo(LocalDateTime.now().minusMinutes(1));

        when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuario));

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () -> service.verificarEmail(dto));
        assertFalse(usuario.isVerificado()); // Continua não verificado
    }

    @Test
    @DisplayName("Deve excluir o usuário logado com sucesso")
    void excluirUsuario_Sucesso() {
        // 1. ARRANGE (Preparação)
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setEmail("teste@email.com");

        // Ensinamos o Mock: "Quando perguntarem quem está logado, diga que é esse usuarioMock"
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioMock);

        // 2. ACT (Ação)
        service.excluirUsuario();

        // 3. ASSERT (Verificação)
        // Verificamos se o método .delete() do repository foi chamado exatamente 1 vez
        // e se o parâmetro passado foi o nosso usuarioMock.
        verify(repository, times(1)).delete(usuarioMock);
    }

    @Test
    @DisplayName("Deve atualizar nome e senha com sucesso")
    void atualizarUsuario_NomeESenha() {
        // 1. ARRANGE
        // Dados novos que queremos colocar
        UsuarioRequestDTO request = new UsuarioRequestDTO("Novo Nome", null, "NovaSenha123");

        // O usuário como está hoje no banco (Simulado)
        Usuario usuarioNoBanco = new Usuario();
        usuarioNoBanco.setId(1L);
        usuarioNoBanco.setNome("Nome Antigo");
        usuarioNoBanco.setSenha("SenhaAntigaHash");
        usuarioNoBanco.setEmail("email@teste.com");

        // O DTO que esperamos de volta (ajustado para o seu construtor de 6 argumentos)
        UsuarioResponseDTO responseEsperado = new UsuarioResponseDTO(
                1L, "Novo Nome", "email@teste.com", new ArrayList<>(), false, null
        );

        // Ensinando os Mocks
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioNoBanco);
        when(encoder.encode("NovaSenha123")).thenReturn("HashNovaSenha"); // Simula criptografia
        when(convertToDTO.convertUserToUserDTO(usuarioNoBanco)).thenReturn(responseEsperado);

        // 2. ACT
        service.atualizarUsuario(request);

        // 3. ASSERT
        // Verifica se o objeto usuarioNoBanco foi alterado na memória
        assertEquals("Novo Nome", usuarioNoBanco.getNome());
        assertEquals("HashNovaSenha", usuarioNoBanco.getSenha());

        // Verifica se o save foi chamado
        verify(repository, times(1)).save(usuarioNoBanco);
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar atualizar para um email que já existe")
    void atualizarUsuario_EmailJaExiste() {
        // 1. ARRANGE
        UsuarioRequestDTO request = new UsuarioRequestDTO(null, "email.ocupado@teste.com", null);

        Usuario usuarioLogadoMock = new Usuario();
        usuarioLogadoMock.setEmail("meu.email@teste.com");

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioLogadoMock);
        // O banco diz: "Sim, esse email já existe!"
        when(repository.existsByEmail("email.ocupado@teste.com")).thenReturn(true);

        // 2. ACT & ASSERT
        assertThrows(EmailExistenteException.class, () -> service.atualizarUsuario(request));

        // Garante que NÃO salvou a alteração
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar foto de perfil no usuário logado")
    void salvarFoto_Sucesso() throws IOException {
        // ARRANGE
        // Cria um arquivo falso em memória (nome, original, tipo, conteúdo em bytes)
        MockMultipartFile arquivo = new MockMultipartFile(
                "file",
                "minha-foto.jpg",
                "image/jpeg",
                "bytes_da_imagem".getBytes()
        );

        Usuario usuarioLogadoMock = new Usuario();
        usuarioLogadoMock.setId(1L);

        // Ensina o mock: "Quando pedirem o usuário logado, devolva esse aqui"
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioLogadoMock);

        // ACT
        service.salvarFoto(arquivo);

        // ASSERT
        // Verifica se os bytes foram salvos dentro do objeto usuário
        assertNotNull(usuarioLogadoMock.getFotoPerfil());
        assertArrayEquals("bytes_da_imagem".getBytes(), usuarioLogadoMock.getFotoPerfil());

        // Garante que o repository.save foi chamado
        verify(repository, times(1)).save(usuarioLogadoMock);
    }

    @Test
    @DisplayName("Deve reenviar código se usuário existe e não está verificado")
    void reenviarCodigo_Sucesso() {
        // ARRANGE
        String email = "raphael@email.com";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome("Raphael");
        usuario.setVerificado(false); // Importante: NÃO verificado

        when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

        // ACT
        service.reenviarCodigo(email);

        // ASSERT
        assertNotNull(usuario.getCodigoVerificacao()); // Garante que gerou código
        assertNotNull(usuario.getValidadeCodigo());    // Garante que gerou data

        verify(repository, times(1)).save(usuario); // Salvou no banco
        verify(emailService, times(1)).enviarEmailTexto(eq(email), anyString(), anyString()); // Enviou email
    }

    @Test
    @DisplayName("Não deve fazer nada se usuário já for verificado")
    void reenviarCodigo_JaVerificado() {
        // ARRANGE
        String email = "raphael@email.com";
        Usuario usuario = new Usuario();
        usuario.setVerificado(true); // Já verificado!

        when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

        // ACT
        service.reenviarCodigo(email);

        // ASSERT
        verify(repository, never()).save(any()); // Não deve salvar nada
        verify(emailService, never()).enviarEmailTexto(anyString(), anyString(), anyString()); // Não deve enviar email
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar reenviar para email inexistente")
    void reenviarCodigo_EmailNaoEncontrado() {
        // ARRANGE
        String email = "fantasma@email.com";
        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () -> service.reenviarCodigo(email));
    }

    @Test
    @DisplayName("Deve retornar os dados do usuário logado corretamente")
    void verUsuario_Sucesso() {
        // 1. ARRANGE (Preparação)
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("Raphael");
        usuarioMock.setEmail("raphael@email.com");

        // O DTO que esperamos que o conversor devolva
        // (Lembrando dos 6 argumentos que o seu DTO exige agora)
        UsuarioResponseDTO dtoEsperado = new UsuarioResponseDTO(
                1L, "Raphael", "raphael@email.com", new ArrayList<>(), false, null
        );

        // Ensinando os Mocks
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioMock);
        when(convertToDTO.convertUserToUserDTO(usuarioMock)).thenReturn(dtoEsperado);

        // 2. ACT (Ação)
        UsuarioResponseDTO resultado = service.verUsuario();

        // 3. ASSERT (Verificação)
        assertNotNull(resultado);
        assertEquals("Raphael", resultado.nome());
        assertEquals("raphael@email.com", resultado.email());

        // Verifica se ele realmente perguntou pro usuarioLogado quem era o usuário
        verify(usuarioLogado, times(1)).getUsuarioLogado();
    }
}