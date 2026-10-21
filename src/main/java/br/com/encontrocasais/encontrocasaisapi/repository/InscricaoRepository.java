package br.com.encontrocasais.encontrocasaisapi.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.encontrocasais.encontrocasaisapi.entity.Inscricao;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscricao i where i.id = :id")
    Optional<Inscricao> buscarPorIdComBloqueio(@Param("id") Long id);

    @Query(value = """
            SELECT *
            FROM inscricoes i
            WHERE i.status_pagamento = 'PENDENTE'
              AND i.criado_em >= :desde
              AND (
                  regexp_replace(coalesce(i.telefone, ''), '[^0-9]', '', 'g') = :telefone
                  OR (
                      lower(trim(i.nome_conjuge_1)) = lower(trim(:nomeConjuge1))
                      AND lower(trim(i.nome_conjuge_2)) = lower(trim(:nomeConjuge2))
                  )
                  OR (
                      lower(trim(i.nome_conjuge_1)) = lower(trim(:nomeConjuge2))
                      AND lower(trim(i.nome_conjuge_2)) = lower(trim(:nomeConjuge1))
                  )
              )
            ORDER BY i.criado_em DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Inscricao> buscarPendenteRecente(
            @Param("telefone") String telefone,
            @Param("nomeConjuge1") String nomeConjuge1,
            @Param("nomeConjuge2") String nomeConjuge2,
            @Param("desde") LocalDateTime desde
    );
}
