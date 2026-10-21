package br.com.encontrocasais.encontrocasaisapi.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.encontrocasais.encontrocasaisapi.dto.InscricaoPagamentoResponse;
import br.com.encontrocasais.encontrocasaisapi.dto.InscricaoRequestDTO;
import br.com.encontrocasais.encontrocasaisapi.dto.InscricaoResponseDTO;
import br.com.encontrocasais.encontrocasaisapi.entity.Inscricao;
import br.com.encontrocasais.encontrocasaisapi.service.InscricaoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/inscricoes")
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    public ResponseEntity<InscricaoResponseDTO> criarInscricao(
            @Valid @RequestBody InscricaoRequestDTO request
    ) {
        Inscricao dados = new Inscricao(
                request.nomeConjuge1(),
                request.nomeConjuge2(),
                request.telefone(),
                request.email(),
                request.igreja(),
                request.valor()
        );
        InscricaoPagamentoResponse pagamento = inscricaoService.criarInscricao(dados);
        InscricaoResponseDTO response = new InscricaoResponseDTO(
                pagamento.id(),
                request.nomeConjuge1(),
                request.nomeConjuge2(),
                request.telefone(),
                request.igreja(),
                request.valor(),
                pagamento.statusPagamento(),
                pagamento.urlCheckout()
        );

        return ResponseEntity.created(URI.create("/api/v1/inscricoes/" + pagamento.id()))
                .body(response);
    }
}
