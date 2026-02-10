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

@SpringBootTest // 1. "Liga" a aplicação Spring
@AutoConfigureMockMvc(addFilters = false) // 2. Nos dá o MockMvc para fazer chamadas HTTP
@ActiveProfiles("test") // 3. Usa o application-test.properties (para o H2)
@Transactional // 4. Roda o teste em uma transação e faz rollback no final
class UsuarioControllerUserTest {

    @Autowired
    private MockMvc mockMvc; // O "Postman" do nosso teste

    @Autowired
    private ObjectMapper objectMapper; // Para converter objetos em JSON

    @Autowired
    private UsuarioRepository usuarioRepository; // O repositório REAL

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
        // Limpa o banco antes de cada teste para garantir isolamento
        usuarioRepository.deleteAll();

        // 1. Cria um usuário base para testes que exigem login
        /*Usuario usuario = new Usuario();
        usuario.setNome("Usuario Logado Teste");
        usuario.setEmail("logado@email.com");
        usuario.setSenha(encoder.encode("senha123")); // Salva a senha criptografada
        usuario.setRoles(new ArrayList<>(List.of("USER")));

        Usuario usuarioSalvoNoBanco = usuarioRepository.save(usuario);*/
    }

    @Test
    @DisplayName("Integração: Deve criar usuário com sucesso (201 Created)")
    void criarUsuario_Sucesso() throws Exception {
        // 1. ARRANGE
        UsuarioRequestDTO request = new UsuarioRequestDTO("Novato", "novato@email.com", "123456");

        // 2. ACT & ASSERT
        mockMvc.perform(post("/usuario/criarusuario") // Chama a URL
                        .contentType(MediaType.APPLICATION_JSON) // Avisa que é JSON
                        .content(objectMapper.writeValueAsString(request))) // Transforma objeto em JSON string

                // Validações da Resposta HTTP
                .andExpect(status().isCreated()) // Espera status 201
                .andExpect(header().exists("Location")) // Espera o header Location
                .andExpect(jsonPath("$.email").value("novato@email.com")) // O JSON de volta tem o email certo?
                .andExpect(jsonPath("$.nome").value("Novato")); // O JSON de volta tem o nome certo?

        // 3. VERIFICAÇÃO FINAL (No Banco de Dados Real H2)
        // Isso prova que a Controller chamou o Service e o Service chamou o Repository!
        assertTrue(usuarioRepository.findByEmail("novato@email.com").isPresent());
    }

    @Test
    @DisplayName("Integração: Deve excluir o próprio usuário logado (Status 204)")
    void excluirUsuario_Sucesso() throws Exception {
        // 1. ARRANGE (Preparar o terreno)
        // Criamos um usuário de verdade e salvamos no banco H2
        Usuario usuarioParaDeletar = new Usuario();
        usuarioParaDeletar.setNome("Vou Sumir");
        usuarioParaDeletar.setEmail("tchau@email.com");
        usuarioParaDeletar.setSenha("123456");

        // Salvamos e guardamos a referência dele (com ID gerado)
        Usuario usuarioSalvo = usuarioRepository.save(usuarioParaDeletar);

        // O PULO DO GATO 🐱:
        // Ensinamos o UsuarioLogado a retornar esse cara que acabamos de salvar.
        // Assim, quando o Service rodar, ele vai achar que esse é o usuário da sessão.
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioSalvo);

        // 2. ACT (Ação)
        mockMvc.perform(delete("/usuario")) // Faz a requisição DELETE para /usuario

                // 3. ASSERT (Validações HTTP)
                .andExpect(status().isNoContent()); // Espera o status 204 (Sucesso sem conteúdo)

        // 4. VERIFICAÇÃO FINAL (Banco de Dados)
        // Buscamos pelo email para garantir que ele SUMIU do banco H2
        assertTrue(usuarioRepository.findByEmail("tchau@email.com").isEmpty());
    }

    @Test
    @DisplayName("Integração: Deve validar o código e verificar o usuário (Status 200)")
    void verificarEmail_Sucesso() throws Exception {
        // 1. ARRANGE (Preparar o cenário)
        // Criamos um usuário que ainda NÃO está verificado
        Usuario usuario = new Usuario();
        usuario.setNome("Indeciso");
        usuario.setEmail("duvida@email.com");
        usuario.setSenha("123456");
        usuario.setCodigoVerificacao("CODIGO-TOP-123"); // Definimos um código esperado
        usuario.setVerificado(false);
        usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(10));

        usuarioRepository.save(usuario);

        // Simulamos o JSON que o Front envia com o código correto
        VerificarEmailDTO dto = new VerificarEmailDTO("duvida@email.com", "CODIGO-TOP-123");

        // 2. ACT (Ação)
        mockMvc.perform(post("/usuario/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                // 3. ASSERT (Resposta HTTP)
                .andExpect(status().isOk()) // Espera 200 OK
                .andExpect(jsonPath("$.message").value("Conta verificada!")); // Confere a mensagem do Map

        // 4. VERIFICAÇÃO FINAL (Banco de Dados)
        // Buscamos o usuário de novo para ver se a mágica aconteceu
        Usuario usuarioAtualizado = usuarioRepository.findByEmail("duvida@email.com").orElseThrow();

        // A flag verificado mudou para TRUE?
        assertTrue(usuarioAtualizado.isVerificado(), "O usuário deveria estar marcado como verificado no banco");

        // O código de verificação foi limpo? (Opcional, depende da sua regra, mas é comum limpar)
        assertNull(usuarioAtualizado.getCodigoVerificacao(), "O código de verificação deveria ser limpo após o uso");
    }

    @Test
    @DisplayName("Integração: Deve atualizar dados do usuário logado (Status 200)")
    void atualizarUsuario_Sucesso() throws Exception {
        // 1. ARRANGE
        Usuario usuarioAntigo = new Usuario();
        usuarioAntigo.setNome("Nome Antigo");
        usuarioAntigo.setEmail("troca@email.com");
        usuarioAntigo.setSenha("123456");
        usuarioAntigo.setVerificado(true);

        Usuario usuarioSalvo = usuarioRepository.save(usuarioAntigo);

        // O Service vai perguntar "Quem está logado?". Nós respondemos: "É esse cara aqui!"
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuarioSalvo);

        // O JSON com os dados novos (O usuário quer mudar o nome para "Nome Novo")
        UsuarioRequestDTO requestAtualizacao = new UsuarioRequestDTO(
                "Nome Novo",
                "troca@email.com",
                "123456"
        );

        // 2. ACT
        mockMvc.perform(put("/usuario") // PutMapping na raiz do controller
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAtualizacao)))

                // 3. ASSERT (HTTP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo")); // O retorno já deve vir atualizado

        // 4. VERIFICAÇÃO FINAL (Banco de Dados)
        Usuario usuarioNoBanco = usuarioRepository.findByEmail("troca@email.com").orElseThrow();

        // O nome mudou mesmo?
        assertEquals("Nome Novo", usuarioNoBanco.getNome());
    }

    @Test
    @DisplayName("Integração: Deve solicitar o reenvio de código (Status 200)")
    void reenviarCodigo_Sucesso() throws Exception {
        // 1. ARRANGE
        // Criamos um usuário que precisa do reenvio
        Usuario usuario = new Usuario();
        usuario.setNome("Esquecido");
        usuario.setEmail("esqueci@email.com");
        usuario.setSenha("123456");
        usuario.setVerificado(false);
        usuarioRepository.save(usuario);

        // Criamos o JSON simples { "email": "esqueci@email.com" }
        Map<String, String> payload = Map.of("email", "esqueci@email.com");

        // 2. ACT
        mockMvc.perform(post("/usuario/reenviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))

                // 3. ASSERT (Resposta HTTP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email reenviado!"));

        // 4. VERIFICAÇÃO DE MOCK (Side Effect)
        // Isso confirma que o Controller chamou o Service, e o Service chamou o EmailService!
        verify(emailService).enviarEmailTexto(eq("esqueci@email.com"), any(), any());
    }

    @Test
    @DisplayName("Integração: Deve fazer upload de foto de perfil (200 OK)")
    void uploadFoto_Sucesso() throws Exception {
        // 1. ARRANGE
        // Criamos e salvamos um usuário no banco H2
        Usuario usuario = new Usuario();
        usuario.setEmail("foto@email.com");
        usuario.setNome("Modelo");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);

        // Ensinamos o Mock a dizer que esse usuário está logado
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        // Criamos um arquivo de imagem falso
        MockMultipartFile arquivo = new MockMultipartFile(
                "file",                      // nome do parâmetro no Controller (@RequestParam("file"))
                "avatar.jpg",                // nome do arquivo
                MediaType.IMAGE_JPEG_VALUE,  // tipo de conteúdo
                "bytes_da_imagem_aqui".getBytes() // conteúdo fake
        );

        // 2. ACT
        mockMvc.perform(multipart("/usuario/foto") // 'multipart' simula um POST com arquivo
                        .file(arquivo))

                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Foto atualizada com sucesso!"));
    }

    @Test
    @DisplayName("Integração: Deve retornar dados do usuário logado (200 OK)")
    void verUsuario_Sucesso() throws Exception {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setNome("Curioso");
        usuario.setEmail("curioso@email.com");
        usuario.setSenha("123456");
        // Se sua entidade tiver campo de foto, pode setar algo aqui para testar
        // usuario.setFotoPerfil("http://foto.fake");
        usuarioRepository.save(usuario);

        // Simulamos que o "Curioso" está logado
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        // 2. ACT
        // Chamamos o GET /usuario (que mapeia para o verUsuario())
        mockMvc.perform(get("/usuario"))

                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Curioso"))
                .andExpect(jsonPath("$.email").value("curioso@email.com"));
    }

}
