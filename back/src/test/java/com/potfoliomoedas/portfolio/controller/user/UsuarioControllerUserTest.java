package com.potfoliomoedas.portfolio.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potfoliomoedas.portfolio.component.UsuarioLogado;
import com.potfoliomoedas.portfolio.config.JWTCreator;
import com.potfoliomoedas.portfolio.config.SecurityConfig;
import com.potfoliomoedas.portfolio.dto.UsuarioRequestDTO;
import com.potfoliomoedas.portfolio.dto.VerificarEmailDTO;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class UsuarioControllerUserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private JWTCreator jwtCreator;

    @Autowired
    private SecurityConfig securityConfig;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private UsuarioLogado usuarioLogado;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

    }

    @Test
    @DisplayName("Integração: Deve criar usuário com sucesso (201 Created)")
    void criarUsuario_Sucesso() throws Exception {
        UsuarioRequestDTO request = new UsuarioRequestDTO("Novato", "novato@email.com", "123456");

        mockMvc.perform(post("/usuario/criarusuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("novato@email.com"))
                .andExpect(jsonPath("$.nome").value("Novato"));

        assertTrue(usuarioRepository.findByEmail("novato@email.com").isPresent());
    }

    @Test
    @DisplayName("Integração: Deve excluir o próprio usuário logado (Status 204)")
    void excluirUsuario_Sucesso() throws Exception {
        Usuario usuarioParaDeletar = new Usuario();
        usuarioParaDeletar.setNome("Vou Sumir");
        usuarioParaDeletar.setEmail("tchau@email.com");
        usuarioParaDeletar.setSenha("123456");
        Usuario usuarioSalvo = usuarioRepository.save(usuarioParaDeletar);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioSalvo);
        mockMvc.perform(delete("/usuario"))
                .andExpect(status().isNoContent());
        assertTrue(usuarioRepository.findByEmail("tchau@email.com").isEmpty());
    }

    @Test
    @DisplayName("Integração: Deve validar o código e verificar o usuário (Status 200)")
    void verificarEmail_Sucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Indeciso");
        usuario.setEmail("duvida@email.com");
        usuario.setSenha("123456");
        usuario.setCodigoVerificacao("CODIGO-TOP-123");
        usuario.setVerificado(false);
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(10));

        usuarioRepository.save(usuario);

        VerificarEmailDTO dto = new VerificarEmailDTO("duvida@email.com", "CODIGO-TOP-123");

        mockMvc.perform(post("/usuario/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conta verificada!"));
        Usuario usuarioAtualizado = usuarioRepository.findByEmail("duvida@email.com").orElseThrow();

        assertTrue(usuarioAtualizado.isVerificado(), "O usuário deveria estar marcado como verificado no banco");

        assertNull(usuarioAtualizado.getCodigoVerificacao(), "O código de verificação deveria ser limpo após o uso");
    }

    @Test
    @DisplayName("Integração: Deve atualizar dados do usuário logado (Status 200)")
    void atualizarUsuario_Sucesso() throws Exception {
        Usuario usuarioAntigo = new Usuario();
        usuarioAntigo.setNome("Nome Antigo");
        usuarioAntigo.setEmail("troca@email.com");
        usuarioAntigo.setSenha("123456");
        usuarioAntigo.setVerificado(true);

        Usuario usuarioSalvo = usuarioRepository.save(usuarioAntigo);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioSalvo);

        UsuarioRequestDTO requestAtualizacao = new UsuarioRequestDTO(
                "Nome Novo",
                "troca@email.com",
                "123456"
        );
        mockMvc.perform(put("/usuario") // PutMapping na raiz do controller
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAtualizacao)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
        Usuario usuarioNoBanco = usuarioRepository.findByEmail("troca@email.com").orElseThrow();
        assertEquals("Nome Novo", usuarioNoBanco.getNome());
    }

    @Test
    @DisplayName("Integração: Deve solicitar o reenvio de código (Status 200)")
    void reenviarCodigo_Sucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Esquecido");
        usuario.setEmail("esqueci@email.com");
        usuario.setSenha("123456");
        usuario.setVerificado(false);
        usuarioRepository.save(usuario);
        Map<String, String> payload = Map.of("email", "esqueci@email.com");
        mockMvc.perform(post("/usuario/reenviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email reenviado!"));
        verify(emailService).enviarEmailTexto(eq("esqueci@email.com"), any(), any());
    }

    @Test
    @DisplayName("Integração: Deve fazer upload de foto de perfil (200 OK)")
    void uploadFoto_Sucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("foto@email.com");
        usuario.setNome("Modelo");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        MockMultipartFile arquivo = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "bytes_da_imagem_aqui".getBytes()
        );

        mockMvc.perform(multipart("/usuario/foto")
                        .file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Foto atualizada com sucesso!"));
    }

    @Test
    @DisplayName("Integração: Deve retornar dados do usuário logado (200 OK)")
    void verUsuario_Sucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Curioso");
        usuario.setEmail("curioso@email.com");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        mockMvc.perform(get("/usuario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Curioso"))
                .andExpect(jsonPath("$.email").value("curioso@email.com"));
    }

}
