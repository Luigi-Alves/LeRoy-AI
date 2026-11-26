package com.leroyai.model;

import java.math.BigDecimal;

public class Produto {

    private int id;
    private String nome;
    private String descricao;
    private String categoria;
    private BigDecimal preco;
    private int quantidade;
    private String corredor;
    private String secao;
    private String prateleira;
    private String imagemUrl;

    public Produto(
            int id,
            String nome,
            String descricao,
            String categoria,
            BigDecimal preco,
            int quantidade,
            String corredor,
            String secao,
            String prateleira,
            String imagemUrl
    ) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidade = quantidade;
        this.corredor = corredor;
        this.secao = secao;
        this.prateleira = prateleira;
        this.imagemUrl = imagemUrl;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getCorredor() {
        return corredor;
    }

    public String getSecao() {
        return secao;
    }

    public String getPrateleira() {
        return prateleira;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public boolean isDisponivel() {
        return quantidade > 0;
    }
}