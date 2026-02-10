package com.potfoliomoedas.portfolio.controller.user;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potfoliomoedas.portfolio.component.UsuarioLogado;
import com.potfoliomoedas.portfolio.dto.*;
import com.potfoliomoedas.portfolio.model.Moeda;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.MoedaRepository;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.CoinGeckoService;
import com.potfoliomoedas.portfolio.service.user.CarteiraServiceUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class CarteiraControllerUserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MoedaRepository moedaRepository;

    @Autowired
    private EntityManager em;

    // --- MOCKS ---
    @MockitoBean
    private UsuarioLogado usuarioLogado;

    @Autowired
    private CoinGeckoService coinGeckoService;

    // ❌ REMOVI O MOCK DO SERVICE! Queremos testar a lógica real.
    // @MockitoBean private CarteiraServiceUser carteiraService;

    // ❌ REMOVI O MOCK DO COINGECKO! O Service real vai usar o RestTemplate mockado abaixo.
    // @MockitoBean private CoinGeckoService coinGeckoService;

    @MockitoBean
    private RestTemplate restTemplate; // Esse é o único que precisa ser mentira

    @BeforeEach
    void setup() {
        moedaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Integração: Deve adicionar moeda na carteira com sucesso (201 Created)")
    void adicionarMoeda_Sucesso() throws Exception {
        // 1. ARRANGE
        Usuario investidor = new Usuario();
        investidor.setNome("Holder");
        investidor.setEmail("holder@crypto.com");
        investidor.setSenha("123456");
        usuarioRepository.save(investidor);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(investidor);

        // Mockamos a API externa (CoinGecko) para o service real funcionar
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("bitcoin", Map.of("brl", 350000.0, "usd", 70000.0)));

        MoedaRequest request = new MoedaRequest("bitcoin", 0.5, "http://fake.url/logo.png");

        // 2. ACT
        mockMvc.perform(post("/usuario/carteira/adicionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 3. ASSERT
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.coinId").value("bitcoin"));
    }

    @Test
    @DisplayName("Integração: Deve retornar o valor total da carteira (Status 200)")
    void getValorTotal_Sucesso() throws Exception {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setNome("Rico");
        usuario.setEmail("rico@email.com");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        // Adicionamos uma moeda REAL no banco
        Moeda btc = new Moeda();
        btc.setCoinId("bitcoin");
        btc.setQuantidade(1.0); // 1 Bitcoin
        btc.setUsuario(usuario);
        btc.setLogo("url");
        moedaRepository.save(btc);

        // Garante que o banco H2 já enxerga a moeda para o serviço buscar
        em.flush();

        // Mockamos a resposta da internet (API)
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("bitcoin", Map.of("brl", 5000.0, "usd", 1000.0, "eur", 900.0)));

        // 👇 1.5 O PULO DO GATO: FORÇAR A ATUALIZAÇÃO DO CACHE
        // Como o serviço é real, precisamos mandar ele buscar os preços agora.
        // Ele vai ler o banco, achar o "bitcoin", chamar o RestTemplate (mockado) e preencher o cache.
        coinGeckoService.atualizarPrecosAutomaticamente();

        // 2. ACT
        mockMvc.perform(get("/usuario/carteira")
                        .contentType(MediaType.APPLICATION_JSON))
                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioNome").value("Rico"))
                // Agora o serviço vai achar o valor 5000.0 no cache e calcular certo!
                .andExpect(jsonPath("$.seuSaldoTotalBRL").value(5000.0));
    }

    @Test
    @DisplayName("Integração: Deve deletar moeda da carteira (Status 204)")
    void deletarMoeda_Sucesso() throws Exception {
        // 1. ARRANGE
        Usuario investidor = new Usuario();
        investidor.setNome("Trader Delete");
        investidor.setEmail("del@email.com");
        investidor.setSenha("123456");
        usuarioRepository.save(investidor);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(investidor);

        // Salvar a moeda antes de deletar
        Moeda moeda = new Moeda();
        moeda.setCoinId("bitcoin");
        moeda.setQuantidade(1.0);
        moeda.setUsuario(investidor);
        moeda.setLogo("fake");
        moedaRepository.save(moeda);

        // 2. ACT
        mockMvc.perform(delete("/usuario/carteira/bitcoin")
                        .contentType(MediaType.APPLICATION_JSON))
                // 3. ASSERT
                .andExpect(status().isNoContent());

        // 4. VERIFICAÇÃO NO BANCO (Service real rodou?)
        assertEquals(0, moedaRepository.count());
    }

    @Test
    @DisplayName("Integração: Deve editar a quantidade de uma moeda existente (200 OK)")
    void editarQuantidade_Sucesso() throws Exception {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setNome("Trader");
        usuario.setEmail("trader@email.com");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        Moeda moedaExistente = new Moeda();
        moedaExistente.setCoinId("bitcoin");
        moedaExistente.setQuantidade(1.0); // Valor antigo
        moedaExistente.setUsuario(usuario);
        moedaExistente.setLogo("http://fake.logo");
        moedaRepository.save(moedaExistente);

        // Mock do CoinGecko (via RestTemplate) para não quebrar o retorno
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("bitcoin", Map.of("brl", 100000.0, "usd", 20000.0)));

        MoedaRequest request = new MoedaRequest("bitcoin", 5.0, "http://logo.fake"); // Novo valor

        // 2. ACT
        mockMvc.perform(put("/usuario/carteira")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. LIMPEZA DE CACHE
        em.flush();
        em.clear();

        // 4. ASSERT
        Moeda moedaAtualizada = moedaRepository.findByUsuarioIdAndCoinId(usuario.getId(), "bitcoin")
                .orElseThrow(() -> new RuntimeException("Moeda não encontrada!"));

        assertEquals(5.0, moedaAtualizada.getQuantidade());
    }

    @Test
    @DisplayName("Integração: Deve buscar moedas na CoinGecko (Autocomplete) - Status 200")
    void buscarMoeda_Sucesso() throws Exception {
        // 1. ARRANGE
        String termoBusca = "bit";

        // Criamos uma resposta Fake igual a API da CoinGecko retornaria
        CoinGeckoSearchResponse.CoinThumb moedaFake = new CoinGeckoSearchResponse.CoinThumb(
                "bitcoin", "Bitcoin", "BTC", "http://logo.url"
        );
        CoinGeckoSearchResponse respostaFake = new CoinGeckoSearchResponse(List.of(moedaFake));

        // Ensinamos o Mock do RestTemplate a devolver essa resposta quando a URL contiver "search?query=bit"
        // Note que usamos `eq(CoinGeckoSearchResponse.class)` porque o seu service chama `getForObject(..., Class)`
        when(restTemplate.getForObject(
                contains("search?query=" + termoBusca), // Matcher inteligente do Mockito
                eq(CoinGeckoSearchResponse.class)
        )).thenReturn(respostaFake);

        // 2. ACT
        mockMvc.perform(get("/usuario/carteira/buscar-moeda")
                        .param("query", termoBusca) // Adiciona ?query=bit na URL
                        .contentType(MediaType.APPLICATION_JSON))

                // 3. ASSERT
                .andExpect(status().isOk())
                // Verifica se retornou uma lista e se o primeiro item é o Bitcoin
                .andExpect(jsonPath("$[0].id").value("bitcoin"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].symbol").value("BTC"));
    }

    @Test
    @DisplayName("Integração: Deve retornar o histórico de preços (Gráfico) - Status 200")
    void getHistorico_Sucesso() throws Exception {
        // 1. ARRANGE
        String coinId = "bitcoin";
        String dias = "7";
        String currency = "brl";

        // Preparamos a resposta FAKE da API da CoinGecko
        // A API retorna algo assim: { "prices": [ [timestamp, preco], [timestamp, preco] ... ] }
        List<List<Number>> listaPrecos = List.of(
                List.of(1700000000000L, 200000.50), // Dia 1
                List.of(1700086400000L, 205000.00)  // Dia 2
        );
        Map<String, Object> respostaApiFake = Map.of("prices", listaPrecos);

        // Ensinamos o Mock do RestTemplate a devolver esse JSON quando a URL tiver "market_chart"
        when(restTemplate.getForObject(
                contains("/market_chart"), // Parte única da URL desse endpoint
                eq(Map.class)
        )).thenReturn(respostaApiFake);

        // 2. ACT
        mockMvc.perform(get("/usuario/carteira/historico/" + coinId)
                        .param("dias", dias)
                        .param("currency", currency)
                        .contentType(MediaType.APPLICATION_JSON))

                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()) // Deve ser uma lista
                .andExpect(jsonPath("$[0][0]").value(1700000000000L)) // Primeiro timestamp
                .andExpect(jsonPath("$[0][1]").value(200000.50));     // Primeiro preço
    }
}