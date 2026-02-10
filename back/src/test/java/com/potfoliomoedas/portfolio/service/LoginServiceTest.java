package com.potfoliomoedas.portfolio.service;

import static org.junit.jupiter.api.Assertions.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.potfoliomoedas.portfolio.config.JWTCreator;
import com.potfoliomoedas.portfolio.config.JWTObject;
import com.potfoliomoedas.portfolio.config.SecurityConfig;
import com.potfoliomoedas.portfolio.dto.Login;
import com.potfoliomoedas.portfolio.dto.Sessao;
import com.potfoliomoedas.portfolio.exception.InvalidCredentialsException;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.user.UsuarioServiceUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private JWTCreator jwtCreator;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private UsuarioServiceUser usuarioService;

    @InjectMocks
    private LoginService loginService;

    @Test
    @DisplayName("Deve logar com sucesso e retornar Token quando credenciais válidas")
    void logar_Sucesso() {
        // 1. ARRANGE
        Login login = new Login("raphael@email.com", "123456");

        Usuario usuario = new Usuario();
        usuario.setEmail("raphael@email.com");
        usuario.setSenha("SenhaCriptografadaNoBanco");
        usuario.setVerificado(true); // Importante: Usuário já verificou email

        // Mocks
        when(repository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        // Simula que a senha digitada bate com a criptografada
        when(encoder.matches(login.senha(), usuario.getSenha())).thenReturn(true);
        // Simula configuração de expiração do token (ex: 3600000ms)
        when(securityConfig.getEXPIRATION()).thenReturn(3600000L);
        // Simula a geração da String do token
        when(jwtCreator.gerarToken(any(JWTObject.class))).thenReturn("token.jwt.falso");

        // 2. ACT
        Sessao sessao = loginService.logar(login);

        // 3. ASSERT
        assertNotNull(sessao);
        assertEquals("raphael@email.com", sessao.login());
        assertEquals("token.jwt.falso", sessao.token());

        // Verifica se gerou o token chamando o criador
        verify(jwtCreator, times(1)).gerarToken(any(JWTObject.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando usuário não for encontrado (Email errado)")
    void logar_UsuarioNaoEncontrado() {
        // 1. ARRANGE
        Login login = new Login("naoexiste@email.com", "123456");

        when(repository.findByEmail(login.email())).thenReturn(Optional.empty());

        // 2. ACT & ASSERT
        assertThrows(InvalidCredentialsException.class, () -> loginService.logar(login));
    }

    @Test
    @DisplayName("Deve lançar erro quando a senha estiver incorreta")
    void logar_SenhaIncorreta() {
        // 1. ARRANGE
        Login login = new Login("raphael@email.com", "SenhaErrada");

        Usuario usuario = new Usuario();
        usuario.setSenha("HashVerdadeira");
        usuario.setVerificado(true);

        when(repository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        // Simula que a senha NÃO bate
        when(encoder.matches(login.senha(), usuario.getSenha())).thenReturn(false);

        // 2. ACT & ASSERT
        assertThrows(InvalidCredentialsException.class, () -> loginService.logar(login));
    }

    @Test
    @DisplayName("Deve bloquear login e reenviar código se usuário não for verificado")
    void logar_UsuarioNaoVerificado() {
        // 1. ARRANGE
        Login login = new Login("raphael@email.com", "123456");

        Usuario usuario = new Usuario();
        usuario.setEmail("raphael@email.com");
        usuario.setSenha("HashSenha");
        usuario.setVerificado(false); // ⚠️ NÃO VERIFICADO

        when(repository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        when(encoder.matches(login.senha(), usuario.getSenha())).thenReturn(true);

        // 2. ACT & ASSERT
        RuntimeException erro = assertThrows(RuntimeException.class, () -> loginService.logar(login));

        // Verifica a mensagem de erro
        assertTrue(erro.getMessage().contains("Seu email ainda não foi verificado"));

        // O PULO DO GATO:
        // Verifica se o sistema tentou reenviar o código automaticamente
        verify(usuarioService, times(1)).reenviarCodigo("raphael@email.com");

        // Garante que NÃO gerou token
        verify(jwtCreator, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Deve logar com Google criando NOVO usuário quando token for válido")
    void logarComGoogle_NovoUsuario() throws GeneralSecurityException, IOException {
        // 1. ARRANGE
        String tokenGoogle = "token_valido_google";

        // Cria o Payload falso (Dados que o Google retornaria)
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("novo@gmail.com");
        payload.set("name", "Novo Usuario");
        payload.set("picture", "http://foto.com/perfil.jpg");

        // Cria o objeto Token completo
        GoogleIdToken idTokenMock = new GoogleIdToken(
                new GoogleIdToken.Header(),
                payload,
                new byte[0],
                new byte[0]
        );

        // SPY: Criamos um espião do service real
        LoginService serviceSpy = spy(loginService);

        // Ensinamos o Spy: "Quando chamarem validarTokenNoGoogle, NÃO vá no Google. Retorne nosso mock."
        doReturn(idTokenMock).when(serviceSpy).validarTokenNoGoogle(tokenGoogle);

        // Se baixarFotoGoogle for chamado, retornamos bytes vazios para não baixar da internet
        // (Isso requer que baixarFotoGoogle seja protected ou public, se for private pode dar erro aqui, aí removemos essa linha)
        // doReturn(new byte[0]).when(serviceSpy).baixarFotoGoogle(anyString());

        // Configura mocks normais
        when(repository.findByEmail("novo@gmail.com")).thenReturn(Optional.empty()); // Não existe ainda
        when(securityConfig.getEXPIRATION()).thenReturn(3600000L);
        when(jwtCreator.gerarToken(any())).thenReturn("token.jwt.google");

        // 2. ACT
        Sessao sessao = serviceSpy.logarComGoogle(tokenGoogle);

        // 3. ASSERT
        assertNotNull(sessao);
        assertEquals("novo@gmail.com", sessao.login());
        assertEquals("token.jwt.google", sessao.token());

        // Verifica se salvou o usuário novo com a flag do Google
        verify(repository, times(2)).save(argThat(user ->
                user.getEmail().equals("novo@gmail.com") &&
                        user.getSenha().equals("GOOGLE_AUTH_EXTERNO") &&
                        user.isVerificado() == true
        ));
    }

    @Test
    @DisplayName("Deve lançar erro quando o token do Google for inválido")
    void logarComGoogle_TokenInvalido() throws GeneralSecurityException, IOException {
        // 1. ARRANGE
        String tokenRuim = "token_falso";

        LoginService serviceSpy = spy(loginService);
        // Simula que o Google retornou NULL (token inválido)
        doReturn(null).when(serviceSpy).validarTokenNoGoogle(tokenRuim);

        // 2. ACT & ASSERT
        Exception erro = assertThrows(IllegalArgumentException.class, () ->
                serviceSpy.logarComGoogle(tokenRuim)
        );

        assertEquals("Token do Google inválido", erro.getMessage());

        // Garante que NUNCA salvou nada nem gerou token
        verify(repository, never()).save(any());
        verify(jwtCreator, never()).gerarToken(any());
    }
}