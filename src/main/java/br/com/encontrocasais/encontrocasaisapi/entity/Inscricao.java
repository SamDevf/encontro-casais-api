package br.com.encontrocasais.encontrocasaisapi.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inscricoes")
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_conjuge_1", nullable = false, length = 150)
    private String nomeConjuge1;

    @Column(name = "nome_conjuge_2", nullable = false, length = 150)
    private String nomeConjuge2;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Column(length = 254)
    private String email;

    @Column(length = 200)
    private String igreja;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "preference_id", length = 100)
    private String preferenceId;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Inscricao() {
    }

    public Inscricao(
            String nomeConjuge1,
            String nomeConjuge2,
            String telefone,
            String email,
            String igreja,
            BigDecimal valor
    ) {
        this.nomeConjuge1 = nomeConjuge1;
        this.nomeConjuge2 = nomeConjuge2;
        this.telefone = telefone;
        this.email = email;
        this.igreja = igreja;
        this.valor = valor;
        this.statusPagamento = StatusPagamento.PENDENTE;
    }

    @PrePersist
    void definirCriadoEm() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getNomeConjuge1() {
        return nomeConjuge1;
    }

    public void setNomeConjuge1(String nomeConjuge1) {
        this.nomeConjuge1 = nomeConjuge1;
    }

    public String getNomeConjuge2() {
        return nomeConjuge2;
    }

    public void setNomeConjuge2(String nomeConjuge2) {
        this.nomeConjuge2 = nomeConjuge2;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIgreja() {
        return igreja;
    }

    public void setIgreja(String igreja) {
        this.igreja = igreja;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public StatusPagamento getStatusPagamento() {
        return statusPagamento;
    }

    public void setStatusPagamento(StatusPagamento statusPagamento) {
        this.statusPagamento = statusPagamento;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
