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

    @MockitoBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        moedaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Integração: Deve adicionar moeda na carteira com sucesso (201 Created)")
    void adicionarMoeda_Sucesso() throws Exception {
        Usuario investidor = new Usuario();
        investidor.setNome("Holder");
        investidor.setEmail("holder@crypto.com");
        investidor.setSenha("123456");
        usuarioRepository.save(investidor);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(investidor);
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("bitcoin", Map.of("brl", 350000.0, "usd", 70000.0)));

        MoedaRequest request = new MoedaRequest("bitcoin", 0.5, "http://fake.url/logo.png");
        mockMvc.perform(post("/usuario/carteira/adicionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.coinId").value("bitcoin"));
    }

    @Test
    @DisplayName("Integração: Deve retornar o valor total da carteira (Status 200)")
    void getValorTotal_Sucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Rico");
        usuario.setEmail("rico@email.com");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        Moeda btc = new Moeda();
        btc.setCoinId("bitcoin");
        btc.setQuantidade(1.0); // 1 Bitcoin
        btc.setUsuario(usuario);
        btc.setLogo("url");
        moedaRepository.save(btc);
        em.flush();

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("bitcoin", Map.of("brl", 5000.0, "usd", 1000.0, "eur", 900.0)));
        coinGeckoService.atualizarPrecosAutomaticamente();
        mockMvc.perform(get("/usuario/carteira")
                        .contentType(MediaType.APPLICATION_JSON))
                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioNome").value("Rico"))
                .andExpect(jsonPath("$.seuSaldoTotalBRL").value(5000.0));
    }

    @Test
    @DisplayName("Integração: Deve deletar moeda da carteira (Status 204)")
    void deletarMoeda_Sucesso() throws Exception {
        Usuario investidor = new Usuario();
        investidor.setNome("Trader Delete");
        investidor.setEmail("del@email.com");
        investidor.setSenha("123456");
        usuarioRepository.save(investidor);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(investidor);

        Moeda moeda = new Moeda();
        moeda.setCoinId("bitcoin");
        moeda.setQuantidade(1.0);
        moeda.setUsuario(investidor);
        moeda.setLogo("fake");
        moedaRepository.save(moeda);
        mockMvc.perform(delete("/usuario/carteira/bitcoin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        assertEquals(0, moedaRepository.count());
    }

    @Test
    @DisplayName("Integração: Deve editar a quantidade de uma moeda existente (200 OK)")
    void editarQuantidade_Sucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Trader");
        usuario.setEmail("trader@email.com");
        usuario.setSenha("123456");
        usuarioRepository.save(usuario);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        Moeda moedaExistente = new Moeda();
        moedaExistente.setCoinId("bitcoin");
        moedaExistente.setQuantidade(1.0);
        moedaExistente.setUsuario(usuario);
        moedaExistente.setLogo("http://fake.logo");
        moedaRepository.save(moedaExistente);
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("bitcoin", Map.of("brl", 100000.0, "usd", 20000.0)));

        MoedaRequest request = new MoedaRequest("bitcoin", 5.0, "http://logo.fake"); // Novo valor

        // 2. ACT
        mockMvc.perform(put("/usuario/carteira")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        Moeda moedaAtualizada = moedaRepository.findByUsuarioIdAndCoinId(usuario.getId(), "bitcoin")
                .orElseThrow(() -> new RuntimeException("Moeda não encontrada!"));

        assertEquals(5.0, moedaAtualizada.getQuantidade());
    }

    @Test
    @DisplayName("Integração: Deve buscar moedas na CoinGecko (Autocomplete) - Status 200")
    void buscarMoeda_Sucesso() throws Exception {
        String termoBusca = "bit";

        CoinGeckoSearchResponse.CoinThumb moedaFake = new CoinGeckoSearchResponse.CoinThumb(
                "bitcoin", "Bitcoin", "BTC", "http://logo.url"
        );
        CoinGeckoSearchResponse respostaFake = new CoinGeckoSearchResponse(List.of(moedaFake));
        when(restTemplate.getForObject(
                contains("search?query=" + termoBusca),
                eq(CoinGeckoSearchResponse.class)
        )).thenReturn(respostaFake);
        mockMvc.perform(get("/usuario/carteira/buscar-moeda")
                        .param("query", termoBusca)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("bitcoin"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].symbol").value("BTC"));
    }

    @Test
    @DisplayName("Integração: Deve retornar o histórico de preços (Gráfico) - Status 200")
    void getHistorico_Sucesso() throws Exception {
        String coinId = "bitcoin";
        String dias = "7";
        String currency = "brl";
        List<List<Number>> listaPrecos = List.of(
                List.of(1700000000000L, 200000.50),
                List.of(1700086400000L, 205000.00)
        );
        Map<String, Object> respostaApiFake = Map.of("prices", listaPrecos);

        when(restTemplate.getForObject(
                contains("/market_chart"),
                eq(Map.class)
        )).thenReturn(respostaApiFake);

        mockMvc.perform(get("/usuario/carteira/historico/" + coinId)
                        .param("dias", dias)
                        .param("currency", currency)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0][0]").value(1700000000000L))
                .andExpect(jsonPath("$[0][1]").value(200000.50));
    }
}