package com.potfoliomoedas.portfolio.controller.user;

import com.potfoliomoedas.portfolio.dto.*;
import com.potfoliomoedas.portfolio.model.Moeda;
import com.potfoliomoedas.portfolio.service.CoinGeckoService;
import com.potfoliomoedas.portfolio.service.user.CarteiraServiceUser;
import com.potfoliomoedas.portfolio.service.user.impl.CarteiraServiceUserImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("usuario/carteira") // Rota base (ex: /portfolio/adicionar)
public class CarteiraControllerUser {

    @Autowired
    private CarteiraServiceUser carteiraService;

    @Autowired
    private CoinGeckoService coinGeckoService;


    @PostMapping("/adicionar")
    public ResponseEntity<MoedaDTO> adicionarMoeda(@RequestBody MoedaRequest request) {
        // 2. O service agora retorna o DTO seguro
        MoedaDTO moedaSalvaDTO = carteiraService.adicionarMoeda(request);

        // 3. (Opcional, mas profissional) Retorne 201 Created
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest() // Pega a URL atual ("/usuario/carteira/adicionar")
                // Vamos assumir que temos um endpoint para ver a carteira
                .path("/../") // Volta para "/usuario/carteira"
                .build()
                .toUri();

        return ResponseEntity.created(location).body(moedaSalvaDTO);
    }

    @GetMapping
    public ResponseEntity<Carteira> getValorTotal() {
        Carteira carteira = carteiraService.calcularValorTotal();
        return ResponseEntity.ok(carteira);
    }

    @DeleteMapping("/{coinId}") // Ex: DELETE /usuario/carteira/bitcoin
    public ResponseEntity<Void> deletarMoeda(@PathVariable String coinId) {
        carteiraService.deletarMoeda(coinId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<UsuarioResponseDTO> editarQuantidade(@RequestBody MoedaRequest request){
        UsuarioResponseDTO usuarioResponseDTO = carteiraService.editarQuantidade(request);
        return ResponseEntity.ok(usuarioResponseDTO);
    }

    @GetMapping("/buscar-moeda") // URL: /usuario/carteira/buscar-moeda?query=bit
    public ResponseEntity<List<CoinGeckoSearchResponse.CoinThumb>> buscarMoedas(@RequestParam String query) {
        if(query.isEmpty()) return ResponseEntity.ok(List.of());

        var resultado = coinGeckoService.buscarMoedasNaCoinGecko(query);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/historico/{coinId}")
    public List<List<Number>> getHistorico(@PathVariable String coinId, @RequestParam(defaultValue = "7") String dias,
                                           @RequestParam(defaultValue = "brl") String currency) {
        return coinGeckoService.buscarHistorico(coinId, dias, currency);
    }
}
