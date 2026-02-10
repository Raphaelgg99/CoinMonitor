package com.potfoliomoedas.portfolio.controller.user;

import com.potfoliomoedas.portfolio.dto.UsuarioRequestDTO;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.dto.VerificarEmailDTO;
import com.potfoliomoedas.portfolio.service.user.UsuarioServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuario")
public class UsuarioControllerUser {

    @Autowired
    private UsuarioServiceUser usuarioService;

    @DeleteMapping
    public ResponseEntity<Void> excluirUsuario(){
        usuarioService.excluirUsuario();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/criarusuario")
    public ResponseEntity<UsuarioResponseDTO> criarUsuario(@RequestBody UsuarioRequestDTO requestDTO) {
        // 1. Crie o usuário e pegue o DTO de resposta
        UsuarioResponseDTO usuarioCriado = usuarioService.criarUsuario(requestDTO);

        // 2. Construa a URI para o novo usuário (ex: http://localhost:8080/usuario/11)
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest() // Pega a URL atual ("/usuario/adicionar")
               .path("/{id}")        // Adiciona "/{id}"
                .buildAndExpand(usuarioCriado.id()) // Substitui o {id} pelo ID do usuário
                .toUri();             // Converte para URI

        // 3. Retorne 201 Created, com a URI no cabeçalho Location e o DTO no corpo
        return ResponseEntity.created(location).body(usuarioCriado);
    }

    @PostMapping("/verificar")
    public ResponseEntity<Object> verificarEmail(@RequestBody VerificarEmailDTO dados) {
        usuarioService.verificarEmail(dados);
        return ResponseEntity.ok(Map.of("message", "Conta verificada!"));
    }


    @PutMapping
    public ResponseEntity<UsuarioResponseDTO> atualizarUsuario(@RequestBody UsuarioRequestDTO requestDTO){
        return ResponseEntity.ok(usuarioService.atualizarUsuario(requestDTO));
    }

    @PostMapping("/reenviar")
    public ResponseEntity<Object> reenviarCodigo(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");

        usuarioService.reenviarCodigo(email);

        return ResponseEntity.ok(Map.of("message", "Email reenviado!"));
    }

    @PostMapping("/foto")
    public ResponseEntity<Object> uploadFoto(@RequestParam("file") MultipartFile file) throws IOException {
            usuarioService.salvarFoto(file);
            return ResponseEntity.ok(Map.of("message", "Foto atualizada com sucesso!"));
    }

    @GetMapping
    public ResponseEntity<UsuarioResponseDTO> verUsuario(){
        return  ResponseEntity.ok(usuarioService.verUsuario());
    }
}
