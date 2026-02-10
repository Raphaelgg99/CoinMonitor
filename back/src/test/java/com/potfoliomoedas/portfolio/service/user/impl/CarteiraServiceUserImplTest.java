package com.potfoliomoedas.portfolio.service.user.impl;

import com.potfoliomoedas.portfolio.component.ConvertToDTO;
import com.potfoliomoedas.portfolio.component.UsuarioLogado;
import com.potfoliomoedas.portfolio.dto.Carteira;
import com.potfoliomoedas.portfolio.dto.MoedaDTO;
import com.potfoliomoedas.portfolio.dto.MoedaRequest;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.model.Moeda;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.MoedaRepository;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.CoinGeckoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CarteiraServiceUserImplTest {

    @Mock
    private MoedaRepository moedaRepository;

    @Mock
    private CoinGeckoService coinGeckoService;

    @Mock
    private UsuarioLogado usuarioLogado;

    // Precisamos mockar mesmo que não usemos diretamente nesse método,
    // pois o Spring tentaria injetar na classe real
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConvertToDTO convertToDTO;

    @InjectMocks
    private CarteiraServiceUserImpl service;

    @Test
    @DisplayName("Deve adicionar uma NOVA moeda quando o usuário ainda não a possui")
    void adicionarMoeda_NovaMoeda() {
        // 1. ARRANGE
        // Usuário quer adicionar 2.5 Bitcoins
        MoedaRequest request = new MoedaRequest("bitcoin", 2.5, null); // Logo null pra testar a busca automática

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        // Simulamos o objeto que o banco vai devolver após salvar
        Moeda moedaSalva = new Moeda();
        moedaSalva.setCoinId("bitcoin");
        moedaSalva.setQuantidade(2.5);
        moedaSalva.setLogo("http://logo-bitcoin.png");

        // Mocks
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        // Simula que NÃO encontrou moeda no banco (Optional Empty) -> Vai cair no 'else' (Criar Nova)
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin")).thenReturn(Optional.empty());

        // Simula a busca do logo na API externa
        when(coinGeckoService.buscarUrlLogo("bitcoin")).thenReturn("http://logo-bitcoin.png");

        // Simula o salvamento
        when(moedaRepository.save(any(Moeda.class))).thenReturn(moedaSalva);

        // 2. ACT
        MoedaDTO resultado = service.adicionarMoeda(request);

        // 3. ASSERT
        assertNotNull(resultado);
        assertEquals("bitcoin", resultado.coinId());
        assertEquals(2.5, resultado.quantidade());

        // Verificações cruciais para nova moeda:
        // 1. Buscou o logo na API externa?
        verify(coinGeckoService).buscarUrlLogo("bitcoin");
        // 2. Forçou a atualização de preço única?
        verify(coinGeckoService).atualizarPrecoUnico("bitcoin");
        // 3. Salvou no banco?
        verify(moedaRepository).save(any(Moeda.class));
    }

    @Test
    @DisplayName("Deve somar quantidade quando a moeda JÁ EXISTE")
    void adicionarMoeda_MoedaExistente() {
        // 1. ARRANGE
        // Usuário já tem Bitcoin e quer adicionar +10
        MoedaRequest request = new MoedaRequest("bitcoin", 10.0, "logo-enviado-pelo-front.png");

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        // Moeda que já existe no banco com 5 unidades
        Moeda moedaExistente = new Moeda();
        moedaExistente.setCoinId("bitcoin");
        moedaExistente.setQuantidade(5.0);
        moedaExistente.setUsuario(usuario);

        // Mocks
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        // Simula que encontrou! (Optional com objeto) -> Vai cair no 'if' (Atualizar Existente)
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin"))
                .thenReturn(Optional.of(moedaExistente));

        // Mock do save (retorna a moeda com a quantidade somada para o teste passar no return final)
        // Note que aqui o Java passa a referência do objeto, então ele já vai estar modificado
        when(moedaRepository.save(moedaExistente)).thenReturn(moedaExistente);

        // 2. ACT
        MoedaDTO resultado = service.adicionarMoeda(request);

        // 3. ASSERT
        // 5 (antigo) + 10 (novo) = 15
        assertEquals(15.0, moedaExistente.getQuantidade());
        assertEquals(15.0, resultado.quantidade());

        // Verificações para moeda existente:
        // NÃO deve buscar preço de novo (pois já existe no monitoramento)
        verify(coinGeckoService, never()).atualizarPrecoUnico(anyString());
        // Deve salvar a moeda atualizada
        verify(moedaRepository).save(moedaExistente);
    }

    @Test
    @DisplayName("Deve calcular o valor total da carteira corretamente (BRL, USD, EUR)")
    void calcularValorTotal_ComMoedas() {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raphael");
        usuario.setEmail("raphael@email.com");

        // Criando moedas do usuário (Banco de Dados simulado)
        // 1 Bitcoin (Preço simulado: 100.0) -> Total deve ser 100.0
        Moeda bitcoin = new Moeda();
        bitcoin.setCoinId("bitcoin");
        bitcoin.setQuantidade(1.0);
        bitcoin.setUsuario(usuario);

        // 10 Ethereums (Preço simulado: 10.0) -> Total deve ser 100.0
        Moeda ethereum = new Moeda();
        ethereum.setCoinId("ethereum");
        ethereum.setQuantidade(10.0);
        ethereum.setUsuario(usuario);

        // Simulando a resposta da API (Map<String, Map<String, Number>>)
        // É um Map dentro de Map. Ex: "bitcoin" -> {"brl": 100.0, "usd": 50.0...}
        Map<String, Map<String, Number>> tabelaPrecos = new HashMap<>();

        // Preços do Bitcoin
        Map<String, Number> precosBtc = new HashMap<>();
        precosBtc.put("brl", 100.0);
        precosBtc.put("usd", 50.0);
        precosBtc.put("eur", 40.0);
        tabelaPrecos.put("bitcoin", precosBtc);

        // Preços do Ethereum
        Map<String, Number> precosEth = new HashMap<>();
        precosEth.put("brl", 10.0);
        precosEth.put("usd", 5.0);
        precosEth.put("eur", 4.0);
        tabelaPrecos.put("ethereum", precosEth);

        // Mocks
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioId(1L)).thenReturn(List.of(bitcoin, ethereum));
        // O Matcher anyList() serve para dizer "qualquer lista de strings"
        when(coinGeckoService.buscarPrecosEmLote(anyList())).thenReturn(tabelaPrecos);

        // 2. ACT
        Carteira resultado = service.calcularValorTotal();

        // 3. ASSERT
        assertNotNull(resultado);
        assertEquals("Raphael", resultado.usuarioNome());

        // Validação Matemática (Soma dos totais)
        // BRL: (1 * 100) + (10 * 10) = 200.0
        assertEquals(200.0, resultado.seuSaldoTotalBRL());

        // USD: (1 * 50) + (10 * 5) = 100.0
        assertEquals(100.0, resultado.seuSaldoTotalUSD());

        // Verifica se a lista de detalhes tem 2 itens
        assertEquals(2, resultado.moedas().size());
    }

    @Test
    @DisplayName("Deve retornar zeros quando a carteira estiver vazia")
    void calcularValorTotal_CarteiraVazia() {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        // Retorna lista vazia do banco
        when(moedaRepository.findByUsuarioId(1L)).thenReturn(List.of());

        // 2. ACT
        Carteira resultado = service.calcularValorTotal();

        // 3. ASSERT
        assertNotNull(resultado);
        assertEquals(0.0, resultado.seuSaldoTotalBRL());
        assertEquals(0.0, resultado.seuSaldoTotalUSD());
        assertEquals(0.0, resultado.seuSaldoTotalEUR());
        assertTrue(resultado.moedas().isEmpty());

        // Verifica se a gente economizou chamada de API (Lógica do if(listaIds.isEmpty()))
        verify(coinGeckoService, never()).buscarPrecosEmLote(anyList());
    }

    @Test
    @DisplayName("Deve deletar moeda com sucesso se ela existir na carteira")
    void deletarMoeda_Sucesso() {
        // ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Moeda moeda = new Moeda();
        moeda.setCoinId("bitcoin");
        moeda.setUsuario(usuario);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        // Simula que o banco encontrou a moeda
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin")).thenReturn(Optional.of(moeda));

        // ACT
        service.deletarMoeda("bitcoin");

        // ASSERT
        verify(moedaRepository).delete(moeda); // Garante que o comando de deletar foi dado
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar deletar moeda que não existe")
    void deletarMoeda_NaoEncontrada() {
        // ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        // Simula que o banco NÃO encontrou nada (Optional Empty)
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "xrp")).thenReturn(Optional.empty());

        // ACT & ASSERT
        RuntimeException erro = assertThrows(RuntimeException.class, () -> service.deletarMoeda("xrp"));
        assertEquals("Moeda não encontrada", erro.getMessage());

        verify(moedaRepository, never()).delete(any()); // Garante que não tentou deletar vento
    }

    @Test
    @DisplayName("Deve ignorar moeda sem preço (Teste do getValorSeguro)")
    void calcularValorTotal_ComMoedaSemCotacao() {
        // ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        // Usuário tem Bitcoin (que tem preço)
        Moeda bitcoin = new Moeda();
        bitcoin.setCoinId("bitcoin");
        bitcoin.setQuantidade(1.0);

        // E tem uma "Moeda Fantasma" (que a API não vai retornar preço)
        Moeda moedaFantasma = new Moeda();
        moedaFantasma.setCoinId("moeda-fantasma");
        moedaFantasma.setQuantidade(100.0);

        // Simulamos que a API só devolveu o preço do Bitcoin. A "moeda-fantasma" sumiu do mapa.
        Map<String, Map<String, Number>> tabelaPrecos = new HashMap<>();
        tabelaPrecos.put("bitcoin", Map.of("brl", 500.0, "usd", 100.0, "eur", 90.0));
        // Note: Não adicionamos a chave "moeda-fantasma" no mapa!

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioId(1L)).thenReturn(List.of(bitcoin, moedaFantasma));
        when(coinGeckoService.buscarPrecosEmLote(anyList())).thenReturn(tabelaPrecos);

        // ACT
        Carteira resultado = service.calcularValorTotal();

        // ASSERT
        // O total deve ser APENAS o do Bitcoin (500.0).
        // Se o getValorSeguro falhasse, aqui daria NullPointerException porque a moeda-fantasma é null.
        assertEquals(500.0, resultado.seuSaldoTotalBRL());
    }

    @Test
    @DisplayName("Deve editar a quantidade de uma moeda existente com sucesso")
    void editarQuantidade_Sucesso() {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raphael");

        // O usuário quer mudar a quantidade de Bitcoin para 5.0
        MoedaRequest request = new MoedaRequest("bitcoin", 5.0, null);

        // A moeda que já está no banco (tinha 10.0 antes)
        Moeda moedaExistente = new Moeda();
        moedaExistente.setCoinId("bitcoin");
        moedaExistente.setQuantidade(10.0);
        moedaExistente.setUsuario(usuario);

        // DTO de resposta (só para o Mock do converter não reclamar)
        UsuarioResponseDTO dtoEsperado = new UsuarioResponseDTO(
                1L, "Raphael", "email", new ArrayList<>(), false, null
        );

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        // Simula que achou a moeda no banco
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin"))
                .thenReturn(Optional.of(moedaExistente));

        when(convertToDTO.convertUserToUserDTO(usuario)).thenReturn(dtoEsperado);

        // 2. ACT
        UsuarioResponseDTO resultado = service.editarQuantidade(request);

        // 3. ASSERT
        assertNotNull(resultado);

        // O mais importante: A quantidade da moeda mudou de 10.0 para 5.0?
        assertEquals(5.0, moedaExistente.getQuantidade());

        // Verificamos se o save foi chamado com a moeda alterada
        verify(moedaRepository).save(moedaExistente);
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar editar quantidade de moeda que não existe")
    void editarQuantidade_MoedaNaoEncontrada() {
        // 1. ARRANGE
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        MoedaRequest request = new MoedaRequest("cardano", 1000.0, null);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);

        // Simula que NÃO achou a moeda (Optional Empty)
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "cardano"))
                .thenReturn(Optional.empty());

        // 2. ACT & ASSERT
        RuntimeException erro = assertThrows(RuntimeException.class, () -> service.editarQuantidade(request));
        assertEquals("Moeda não encontrada", erro.getMessage());

        // Garante que não salvou nada
        verify(moedaRepository, never()).save(any());
    }
}