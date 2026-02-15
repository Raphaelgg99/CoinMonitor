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
@RequestMapping("usuario/carteira")
public class CarteiraControllerUser {

    @Autowired
    private CarteiraServiceUser carteiraService;

    @Autowired
    private CoinGeckoService coinGeckoService;


    @PostMapping("/adicionar")
    public ResponseEntity<MoedaDTO> adicionarMoeda(@RequestBody MoedaRequest request) {
        MoedaDTO moedaSalvaDTO = carteiraService.adicionarMoeda(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/../")
                .build()
                .toUri();

        return ResponseEntity.created(location).body(moedaSalvaDTO);
    }

    @GetMapping
    public ResponseEntity<Carteira> getValorTotal() {
        Carteira carteira = carteiraService.calcularValorTotal();
        return ResponseEntity.ok(carteira);
    }

    @DeleteMapping("/{coinId}")
    public ResponseEntity<Void> deletarMoeda(@PathVariable String coinId) {
        carteiraService.deletarMoeda(coinId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<UsuarioResponseDTO> editarQuantidade(@RequestBody MoedaRequest request){
        UsuarioResponseDTO usuarioResponseDTO = carteiraService.editarQuantidade(request);
        return ResponseEntity.ok(usuarioResponseDTO);
    }

    @GetMapping("/buscar-moeda")
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
