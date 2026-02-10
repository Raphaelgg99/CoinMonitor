package com.potfoliomoedas.portfolio.service.user;

import com.potfoliomoedas.portfolio.dto.Carteira;
import com.potfoliomoedas.portfolio.dto.MoedaDTO;
import com.potfoliomoedas.portfolio.dto.MoedaRequest;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;

public interface CarteiraServiceUser {
    void deletarMoeda(String coinId);
    MoedaDTO adicionarMoeda(MoedaRequest request);
    UsuarioResponseDTO editarQuantidade(MoedaRequest request);
    Carteira calcularValorTotal();
}
