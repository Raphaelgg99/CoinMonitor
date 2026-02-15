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

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConvertToDTO convertToDTO;

    @InjectMocks
    private CarteiraServiceUserImpl service;

    @Test
    @DisplayName("Deve adicionar uma NOVA moeda quando o usuário ainda não a possui")
    void adicionarMoeda_NovaMoeda() {
        MoedaRequest request = new MoedaRequest("bitcoin", 2.5, null); // Logo null pra testar a busca automática

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Moeda moedaSalva = new Moeda();
        moedaSalva.setCoinId("bitcoin");
        moedaSalva.setQuantidade(2.5);
        moedaSalva.setLogo("http://logo-bitcoin.png");
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin")).thenReturn(Optional.empty());
        when(coinGeckoService.buscarUrlLogo("bitcoin")).thenReturn("http://logo-bitcoin.png");
        when(moedaRepository.save(any(Moeda.class))).thenReturn(moedaSalva);
        MoedaDTO resultado = service.adicionarMoeda(request);
        assertNotNull(resultado);
        assertEquals("bitcoin", resultado.coinId());
        assertEquals(2.5, resultado.quantidade());
        verify(coinGeckoService).buscarUrlLogo("bitcoin");
        verify(coinGeckoService).atualizarPrecoUnico("bitcoin");
        verify(moedaRepository).save(any(Moeda.class));
    }

    @Test
    @DisplayName("Deve somar quantidade quando a moeda JÁ EXISTE")
    void adicionarMoeda_MoedaExistente() {
        MoedaRequest request = new MoedaRequest("bitcoin", 10.0, "logo-enviado-pelo-front.png");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Moeda moedaExistente = new Moeda();
        moedaExistente.setCoinId("bitcoin");
        moedaExistente.setQuantidade(5.0);
        moedaExistente.setUsuario(usuario);
        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin"))
                .thenReturn(Optional.of(moedaExistente));
        when(moedaRepository.save(moedaExistente)).thenReturn(moedaExistente);
        MoedaDTO resultado = service.adicionarMoeda(request);
        assertEquals(15.0, moedaExistente.getQuantidade());
        assertEquals(15.0, resultado.quantidade());
        verify(coinGeckoService, never()).atualizarPrecoUnico(anyString());
        verify(moedaRepository).save(moedaExistente);
    }

    @Test
    @DisplayName("Deve calcular o valor total da carteira corretamente (BRL, USD, EUR)")
    void calcularValorTotal_ComMoedas() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raphael");
        usuario.setEmail("raphael@email.com");
        Moeda bitcoin = new Moeda();
        bitcoin.setCoinId("bitcoin");
        bitcoin.setQuantidade(1.0);
        bitcoin.setUsuario(usuario);
        Moeda ethereum = new Moeda();
        ethereum.setCoinId("ethereum");
        ethereum.setQuantidade(10.0);
        ethereum.setUsuario(usuario);
        Map<String, Map<String, Number>> tabelaPrecos = new HashMap<>();
        Map<String, Number> precosBtc = new HashMap<>();
        precosBtc.put("brl", 100.0);
        precosBtc.put("usd", 50.0);
        precosBtc.put("eur", 40.0);
        tabelaPrecos.put("bitcoin", precosBtc);

        Map<String, Number> precosEth = new HashMap<>();
        precosEth.put("brl", 10.0);
        precosEth.put("usd", 5.0);
        precosEth.put("eur", 4.0);
        tabelaPrecos.put("ethereum", precosEth);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioId(1L)).thenReturn(List.of(bitcoin, ethereum));
        when(coinGeckoService.buscarPrecosEmLote(anyList())).thenReturn(tabelaPrecos);

        Carteira resultado = service.calcularValorTotal();

        assertNotNull(resultado);
        assertEquals("Raphael", resultado.usuarioNome());

        assertEquals(200.0, resultado.seuSaldoTotalBRL());
        assertEquals(100.0, resultado.seuSaldoTotalUSD());
        assertEquals(2, resultado.moedas().size());
    }

    @Test
    @DisplayName("Deve retornar zeros quando a carteira estiver vazia")
    void calcularValorTotal_CarteiraVazia() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioId(1L)).thenReturn(List.of());

        Carteira resultado = service.calcularValorTotal();

        assertNotNull(resultado);
        assertEquals(0.0, resultado.seuSaldoTotalBRL());
        assertEquals(0.0, resultado.seuSaldoTotalUSD());
        assertEquals(0.0, resultado.seuSaldoTotalEUR());
        assertTrue(resultado.moedas().isEmpty());

        verify(coinGeckoService, never()).buscarPrecosEmLote(anyList());
    }

    @Test
    @DisplayName("Deve deletar moeda com sucesso se ela existir na carteira")
    void deletarMoeda_Sucesso() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Moeda moeda = new Moeda();
        moeda.setCoinId("bitcoin");
        moeda.setUsuario(usuario);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin")).thenReturn(Optional.of(moeda));

        service.deletarMoeda("bitcoin");

        verify(moedaRepository).delete(moeda); // Garante que o comando de deletar foi dado
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar deletar moeda que não existe")
    void deletarMoeda_NaoEncontrada() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "xrp")).thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(RuntimeException.class, () -> service.deletarMoeda("xrp"));
        assertEquals("Moeda não encontrada", erro.getMessage());

        verify(moedaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve ignorar moeda sem preço (Teste do getValorSeguro)")
    void calcularValorTotal_ComMoedaSemCotacao() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Moeda bitcoin = new Moeda();
        bitcoin.setCoinId("bitcoin");
        bitcoin.setQuantidade(1.0);
        Moeda moedaFantasma = new Moeda();
        moedaFantasma.setCoinId("moeda-fantasma");
        moedaFantasma.setQuantidade(100.0);

        Map<String, Map<String, Number>> tabelaPrecos = new HashMap<>();
        tabelaPrecos.put("bitcoin", Map.of("brl", 500.0, "usd", 100.0, "eur", 90.0));

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioId(1L)).thenReturn(List.of(bitcoin, moedaFantasma));
        when(coinGeckoService.buscarPrecosEmLote(anyList())).thenReturn(tabelaPrecos);

        Carteira resultado = service.calcularValorTotal();
        assertEquals(500.0, resultado.seuSaldoTotalBRL());
    }

    @Test
    @DisplayName("Deve editar a quantidade de uma moeda existente com sucesso")
    void editarQuantidade_Sucesso() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raphael");

        MoedaRequest request = new MoedaRequest("bitcoin", 5.0, null);
        Moeda moedaExistente = new Moeda();
        moedaExistente.setCoinId("bitcoin");
        moedaExistente.setQuantidade(10.0);
        moedaExistente.setUsuario(usuario);
        UsuarioResponseDTO dtoEsperado = new UsuarioResponseDTO(
                1L, "Raphael", "email", new ArrayList<>(), false, null
        );

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "bitcoin"))
                .thenReturn(Optional.of(moedaExistente));

        when(convertToDTO.convertUserToUserDTO(usuario)).thenReturn(dtoEsperado);
        UsuarioResponseDTO resultado = service.editarQuantidade(request);
        assertNotNull(resultado);
        assertEquals(5.0, moedaExistente.getQuantidade());
        verify(moedaRepository).save(moedaExistente);
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar editar quantidade de moeda que não existe")
    void editarQuantidade_MoedaNaoEncontrada() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        MoedaRequest request = new MoedaRequest("cardano", 1000.0, null);

        when(usuarioLogado.getUsuarioLogado()).thenReturn(usuario);
        when(moedaRepository.findByUsuarioIdAndCoinId(1L, "cardano"))
                .thenReturn(Optional.empty());
        RuntimeException erro = assertThrows(RuntimeException.class, () -> service.editarQuantidade(request));
        assertEquals("Moeda não encontrada", erro.getMessage());
        verify(moedaRepository, never()).save(any());
    }
}