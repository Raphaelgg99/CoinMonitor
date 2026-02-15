package com.potfoliomoedas.portfolio.service.user.impl;

import com.potfoliomoedas.portfolio.component.ConvertToDTO;
import com.potfoliomoedas.portfolio.component.UsuarioLogado;
import com.potfoliomoedas.portfolio.dto.*;
import com.potfoliomoedas.portfolio.model.Moeda;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.MoedaRepository;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.CoinGeckoService;
import com.potfoliomoedas.portfolio.service.user.CarteiraServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CarteiraServiceUserImpl implements CarteiraServiceUser {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MoedaRepository moedaRepository;

    @Autowired
    private CoinGeckoService coinGeckoService;

    @Autowired
    UsuarioLogado usuarioLogado;

    @Autowired
    private ConvertToDTO convertToDTO;

    @Override
    public MoedaDTO adicionarMoeda(MoedaRequest requestDTO) {
        Usuario usuario = usuarioLogado.getUsuarioLogado();

        String coinIdTratado = requestDTO.coinId().trim().toLowerCase();
        String logoFinal = requestDTO.logo();

        if (logoFinal == null || logoFinal.isEmpty()) {
            System.out.println("🔍 Front não mandou logo (usuário digitou manual?). Buscando no Backend para: " + coinIdTratado);
            logoFinal = coinGeckoService.buscarUrlLogo(coinIdTratado);
        }

        Optional<Moeda> moedaOptional = moedaRepository
                .findByUsuarioIdAndCoinId(usuario.getId(), coinIdTratado);

        Moeda moedaParaSalvar;

        if (moedaOptional.isPresent()) {
            moedaParaSalvar = moedaOptional.get();
            moedaParaSalvar.setQuantidade(moedaParaSalvar.getQuantidade() + requestDTO.quantidade());

            if (logoFinal != null && !logoFinal.isEmpty()) {
                moedaParaSalvar.setLogo(logoFinal);
            }
        } else {
            moedaParaSalvar = new Moeda();
            moedaParaSalvar.setCoinId(coinIdTratado);
            moedaParaSalvar.setQuantidade(requestDTO.quantidade());
            moedaParaSalvar.setUsuario(usuario);

            moedaParaSalvar.setLogo(logoFinal);
            coinGeckoService.atualizarPrecoUnico(moedaParaSalvar.getCoinId());
        }
        Moeda moedaSalva = moedaRepository.save(moedaParaSalvar);
        return new MoedaDTO(moedaSalva.getCoinId(), moedaSalva.getQuantidade());
    }

    @Override
    public Carteira calcularValorTotal() {

        Usuario usuario = usuarioLogado.getUsuarioLogado();
        List<Moeda> carteira = moedaRepository.findByUsuarioId(usuario.getId());

        List<String> listaIds = carteira.stream()
                .map(Moeda::getCoinId)
                .toList();

        Map<String, Map<String, Number>> tabelaPrecos;

        if (listaIds.isEmpty()) {
            tabelaPrecos = new HashMap<>();
        } else {
            tabelaPrecos = coinGeckoService.buscarPrecosEmLote(listaIds);
        }

        double valorTotalBRL = 0.0;
        double valorTotalUSD = 0.0;
        double valorTotalEUR = 0.0;
        List<MoedaResponse> moedasResponse = new ArrayList<>();

        for (Moeda moeda : carteira) {

            Map<String, Number> precosDaMoeda = tabelaPrecos.get(moeda.getCoinId());

            double precoBRL = getValorSeguro(precosDaMoeda, "brl");
            double precoUSD = getValorSeguro(precosDaMoeda, "usd");
            double precoEUR = getValorSeguro(precosDaMoeda, "eur");
            double totalMoedaBRL = precoBRL * moeda.getQuantidade();
            double totalMoedaUSD = precoUSD * moeda.getQuantidade();
            double totalMoedaEUR = precoEUR * moeda.getQuantidade();

            // Acumula nos totais da carteira
            valorTotalBRL += totalMoedaBRL;
            valorTotalUSD += totalMoedaUSD;
            valorTotalEUR += totalMoedaEUR;

            // Adiciona na lista de resposta
            moedasResponse.add(new MoedaResponse(
                    moeda.getCoinId(),
                    moeda.getQuantidade(),
                    moeda.getLogo(),
                    round(precoBRL),
                    round(totalMoedaBRL),
                    round(precoUSD),
                    round(totalMoedaUSD),
                    round(precoEUR),
                    round(totalMoedaEUR)
            ));
        }

        // 5. Retorna o DTO Final
        return new Carteira(
                usuario.getEmail(),
                usuario.getNome(),
                round(valorTotalBRL),
                round(valorTotalUSD),
                round(valorTotalEUR),
                moedasResponse
        );
    }

    private double getValorSeguro(Map<String, Number> map, String currency) {
        if (map == null || !map.containsKey(currency) || map.get(currency) == null) {
            return 0.0;
        }
        return map.get(currency).doubleValue();
    }

    public void deletarMoeda(String coinId) {
        Usuario usuario = usuarioLogado.getUsuarioLogado();

        Moeda moeda = moedaRepository.findByUsuarioIdAndCoinId(usuario.getId(), coinId)
                .orElseThrow(() -> new RuntimeException("Moeda não encontrada"));

        moedaRepository.delete(moeda);
    }

    public UsuarioResponseDTO editarQuantidade(MoedaRequest request){
        Usuario usuario = usuarioLogado.getUsuarioLogado();
        // 2. Verifica se o usuário já tem essa moeda
        Moeda moeda = moedaRepository.findByUsuarioIdAndCoinId(usuario.getId(), request.coinId())
                .orElseThrow(() -> new RuntimeException("Moeda não encontrada"));
        moeda.setQuantidade(request.quantidade());
        moedaRepository.save(moeda);
        return convertToDTO.convertUserToUserDTO(usuario);

    }


    private Double round(Double valor) {
        if (valor == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
