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
        Login login = new Login("raphael@email.com", "123456");

        Usuario usuario = new Usuario();
        usuario.setEmail("raphael@email.com");
        usuario.setSenha("SenhaCriptografadaNoBanco");
        usuario.setVerificado(true);

        when(repository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        when(encoder.matches(login.senha(), usuario.getSenha())).thenReturn(true);
        when(securityConfig.getEXPIRATION()).thenReturn(3600000L);
        when(jwtCreator.gerarToken(any(JWTObject.class))).thenReturn("token.jwt.falso");

        Sessao sessao = loginService.logar(login);

        assertNotNull(sessao);
        assertEquals("raphael@email.com", sessao.login());
        assertEquals("token.jwt.falso", sessao.token());

        verify(jwtCreator, times(1)).gerarToken(any(JWTObject.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando usuário não for encontrado (Email errado)")
    void logar_UsuarioNaoEncontrado() {

        Login login = new Login("naoexiste@email.com", "123456");

        when(repository.findByEmail(login.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> loginService.logar(login));
    }

    @Test
    @DisplayName("Deve lançar erro quando a senha estiver incorreta")
    void logar_SenhaIncorreta() {
        Login login = new Login("raphael@email.com", "SenhaErrada");

        Usuario usuario = new Usuario();
        usuario.setSenha("HashVerdadeira");
        usuario.setVerificado(true);

        when(repository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        when(encoder.matches(login.senha(), usuario.getSenha())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> loginService.logar(login));
    }

    @Test
    @DisplayName("Deve bloquear login e reenviar código se usuário não for verificado")
    void logar_UsuarioNaoVerificado() {
        Login login = new Login("raphael@email.com", "123456");

        Usuario usuario = new Usuario();
        usuario.setEmail("raphael@email.com");
        usuario.setSenha("HashSenha");
        usuario.setVerificado(false); // ⚠️ NÃO VERIFICADO

        when(repository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        when(encoder.matches(login.senha(), usuario.getSenha())).thenReturn(true);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> loginService.logar(login));

        assertTrue(erro.getMessage().contains("Seu email ainda não foi verificado"));

        verify(usuarioService, times(1)).reenviarCodigo("raphael@email.com");

        verify(jwtCreator, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Deve logar com Google criando NOVO usuário quando token for válido")
    void logarComGoogle_NovoUsuario() throws GeneralSecurityException, IOException {
        String tokenGoogle = "token_valido_google";

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("novo@gmail.com");
        payload.set("name", "Novo Usuario");
        payload.set("picture", "http://foto.com/perfil.jpg");

        GoogleIdToken idTokenMock = new GoogleIdToken(
                new GoogleIdToken.Header(),
                payload,
                new byte[0],
                new byte[0]
        );

        LoginService serviceSpy = spy(loginService);
        doReturn(idTokenMock).when(serviceSpy).validarTokenNoGoogle(tokenGoogle);

        when(repository.findByEmail("novo@gmail.com")).thenReturn(Optional.empty()); // Não existe ainda
        when(securityConfig.getEXPIRATION()).thenReturn(3600000L);
        when(jwtCreator.gerarToken(any())).thenReturn("token.jwt.google");

        Sessao sessao = serviceSpy.logarComGoogle(tokenGoogle);

        assertNotNull(sessao);
        assertEquals("novo@gmail.com", sessao.login());
        assertEquals("token.jwt.google", sessao.token());

        verify(repository, times(2)).save(argThat(user ->
                user.getEmail().equals("novo@gmail.com") &&
                        user.getSenha().equals("GOOGLE_AUTH_EXTERNO") &&
                        user.isVerificado() == true
        ));
    }

    @Test
    @DisplayName("Deve lançar erro quando o token do Google for inválido")
    void logarComGoogle_TokenInvalido() throws GeneralSecurityException, IOException {

        String tokenRuim = "token_falso";

        LoginService serviceSpy = spy(loginService);

        doReturn(null).when(serviceSpy).validarTokenNoGoogle(tokenRuim);

        Exception erro = assertThrows(IllegalArgumentException.class, () ->
                serviceSpy.logarComGoogle(tokenRuim)
        );

        assertEquals("Token do Google inválido", erro.getMessage());

        verify(repository, never()).save(any());
        verify(jwtCreator, never()).gerarToken(any());
    }
}