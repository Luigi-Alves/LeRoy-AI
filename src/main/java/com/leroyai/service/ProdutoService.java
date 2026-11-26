
package com.leroyai.service;

import com.leroyai.model.Produto;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProdutoService {

    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
        "a", "ao", "aos", "as", "com", "da", "das", "de", "do", "dos",
        "e", "em", "esta", "está", "estou", "meu", "minha", "meus",
        "minhas", "na", "nas", "no", "nos", "o", "os", "para", "por",
        "preciso", "queria", "quero", "que", "um", "uma", "uns", "umas",
        "adequado", "adequada", "adequados", "adequadas", "pode", "podem",
        "produto", "produtos", "qual", "quais", "ser", "serve"
    );

    private final List<Produto> produtos;

    public ProdutoService() {
        this.produtos = criarCatalogo();
    }

    // Catálogo fictício para desenvolvimento e testes
    private List<Produto> criarCatalogo() {

        return List.of(

            new Produto(
                1,
                "Tinta Acrílica Branca 18L",
                "Tinta branca para pintura de paredes internas.",
                "Tintas",
                new BigDecimal("249.90"),
                12,
                "03",
                "Tintas acrílicas",
                "Prateleira A",
                ""
            ),

            new Produto(
                2,
                "Tinta Acrílica Azul 3,6L",
                "Tinta azul indicada para paredes internas.",
                "Tintas",
                new BigDecimal("89.90"),
                8,
                "03",
                "Tintas acrílicas",
                "Prateleira B",
                ""
            ),

            new Produto(
                3,
                "Rolo de Pintura 23cm",
                "Rolo para aplicação de tinta em paredes.",
                "Pintura",
                new BigDecimal("29.90"),
                25,
                "04",
                "Acessórios de pintura",
                "Prateleira A",
                ""
            ),

            new Produto(
                4,
                "Pincel 2 Polegadas",
                "Pincel para acabamento e pintura de superfícies.",
                "Pintura",
                new BigDecimal("14.90"),
                18,
                "04",
                "Acessórios de pintura",
                "Prateleira B",
                ""
            ),

            new Produto(
                5,
                "Fita Crepe 48mm",
                "Fita para proteger superfícies durante a pintura.",
                "Pintura",
                new BigDecimal("12.90"),
                30,
                "05",
                "Proteção e acabamento",
                "Prateleira A",
                ""
            ),

            new Produto(
                6,
                "Bandeja para Pintura",
                "Bandeja para facilitar a aplicação de tinta com rolo.",
                "Pintura",
                new BigDecimal("19.90"),
                10,
                "04",
                "Acessórios de pintura",
                "Prateleira C",
                ""
            ),

            new Produto(
                7,
                "Torneira para Lavatório",
                "Torneira cromada para banheiro.",
                "Hidráulica",
                new BigDecimal("119.90"),
                15,
                "08",
                "Torneiras",
                "Prateleira A",
                ""
            ),

            new Produto(
                8,
                "Fita Veda Rosca",
                "Fita para vedação de conexões hidráulicas.",
                "Hidráulica",
                new BigDecimal("5.90"),
                40,
                "08",
                "Vedação",
                "Prateleira B",
                ""
            ),

            new Produto(
                9,
                "Lâmpada LED 12W",
                "Lâmpada LED branca para iluminação residencial.",
                "Iluminação",
                new BigDecimal("17.90"),
                22,
                "10",
                "Lâmpadas LED",
                "Prateleira A",
                ""
            ),

            new Produto(
                10,
                "Furadeira de Impacto",
                "Furadeira elétrica para perfuração de diferentes superfícies.",
                "Ferramentas",
                new BigDecimal("299.90"),
                5,
                "12",
                "Ferramentas elétricas",
                "Prateleira C",
                ""
            ),

            new Produto(
                11,
                "Lixa para Parede Grão 150",
                "Folha abrasiva para preparar paredes antes da pintura.",
                "Pintura",
                new BigDecimal("3.90"),
                50,
                "04",
                "Preparação de superfícies",
                "Prateleira A",
                ""
            ),

            new Produto(
                12,
                "Massa Corrida 5,5kg",
                "Massa para nivelar e corrigir pequenas imperfeições em paredes internas.",
                "Tintas",
                new BigDecimal("39.90"),
                14,
                "03",
                "Preparação de paredes",
                "Prateleira B",
                ""
            ),

            new Produto(
                13,
                "Luva para Pintura",
                "Luva de proteção para trabalhos de pintura e acabamento.",
                "Pintura",
                new BigDecimal("12.90"),
                20,
                "04",
                "Acessórios de pintura",
                "Prateleira B",
                ""
            ),

            new Produto(
                14,
                "Chuveiro Elétrico 6800W",
                "Chuveiro elétrico com controle de temperatura para banheiro.",
                "Hidráulica",
                new BigDecimal("149.90"),
                9,
                "08",
                "Chuveiros e duchas",
                "Prateleira B",
                ""
            ),

            new Produto(
                15,
                "Selador Acrílico 18L",
                "Fundo preparador para paredes internas antes da pintura.",
                "Tintas",
                new BigDecimal("139.90"),
                7,
                "03",
                "Preparação de paredes",
                "Prateleira C",
                ""
            ),

            new Produto(
                16,
                "Espátula de Aço 10cm",
                "Ferramenta para aplicação de massa e acabamento em paredes.",
                "Pintura",
                new BigDecimal("18.90"),
                16,
                "04",
                "Acessórios de pintura",
                "Prateleira C",
                ""
            ),

            new Produto(
                17,
                "Luminária de Teto LED",
                "Luminária LED para iluminação de ambientes internos.",
                "Iluminação",
                new BigDecimal("59.90"),
                11,
                "10",
                "Luminárias",
                "Prateleira C",
                ""
            ),

            new Produto(
                18,
                "Serrote Manual 18 polegadas",
                "Serrote para cortes em madeira em reparos e projetos domésticos.",
                "Ferramentas",
                new BigDecimal("49.90"),
                6,
                "12",
                "Ferramentas manuais",
                "Prateleira C",
                ""
            ),

            produto(19, "Tinta Acrílica Cinza 18L",
                "Tinta cinza para pintura de paredes internas.",
                "Tintas", "259.90", 9, "03", "Tintas acrílicas",
                "Prateleira A"),
            produto(20, "Tinta Acrílica Bege 3,6L",
                "Tinta bege para renovar ambientes internos.",
                "Tintas", "94.90", 11, "01", "Tintas acrílicas",
                "Prateleira B"),
            produto(21, "Tinta para Piso Cinza 3,6L",
                "Tinta resistente para pintura de pisos de concreto.",
                "Tintas", "109.90", 6, "03", "Tintas para piso",
                "Prateleira C"),
            produto(22, "Tinta Esmalte à Base de Água 3,6L",
                "Tinta para acabamento de portas e superfícies de madeira.",
                "Tintas", "129.90", 8, "03", "Esmaltes",
                "Prateleira A"),
            produto(23, "Tinta Spray Branca 400ml",
                "Tinta spray para pequenos reparos e objetos.",
                "Tintas", "29.90", 20, "03", "Tintas em spray",
                "Prateleira B"),
            produto(24, "Fundo Preparador de Paredes 18L",
                "Produto para preparar superfícies antes da pintura.",
                "Tintas", "119.90", 5, "03", "Preparação de paredes",
                "Prateleira C"),
            produto(25, "Rolo de Pintura Antirrespingo 23cm",
                "Rolo para aplicação uniforme de tinta em paredes.",
                "Pintura", "34.90", 12, "02", "Acessórios de pintura",
                "Prateleira A"),
            produto(26, "Trincha para Pintura 2,5 polegadas",
                "Pincel tipo trincha para pinturas e acabamentos.",
                "Pintura", "16.90", 17, "04", "Acessórios de pintura",
                "Prateleira B"),
            produto(27, "Fita Crepe para Pintura 24mm",
                "Fita para mascaramento e proteção durante a pintura.",
                "Pintura", "8.90", 28, "05", "Proteção e acabamento",
                "Prateleira C"),
            produto(28, "Torneira para Cozinha de Mesa",
                "Torneira cromada para instalação na bancada da cozinha.",
                "Hidráulica", "139.90", 10, "07", "Torneiras",
                "Prateleira A"),
            produto(29, "Torneira para Cozinha de Parede",
                "Torneira para cozinha com instalação na parede.",
                "Hidráulica", "99.90", 8, "08", "Torneiras",
                "Prateleira B"),
            produto(30, "Torneira Monocomando para Lavatório",
                "Torneira monocomando para banheiro.",
                "Hidráulica", "249.90", 4, "08", "Torneiras",
                "Prateleira C"),
            produto(31, "Torneira com Filtro para Cozinha",
                "Torneira para cozinha com saída para filtro de água.",
                "Hidráulica", "189.90", 6, "08", "Torneiras",
                "Prateleira A"),
            produto(32, "Torneira para Tanque",
                "Torneira resistente para tanque de lavanderia.",
                "Hidráulica", "49.90", 14, "08", "Torneiras",
                "Prateleira B"),
            produto(33, "Torneira para Jardim",
                "Torneira metálica para áreas externas e jardim.",
                "Hidráulica", "39.90", 18, "08", "Torneiras",
                "Prateleira C"),
            produto(34, "Torneira com Acionamento por Alavanca",
                "Torneira de alavanca para lavatório.",
                "Hidráulica", "79.90", 7, "08", "Torneiras",
                "Prateleira A"),
            produto(35, "Torneira para Lavatório de Parede",
                "Torneira compacta para instalação na parede do banheiro.",
                "Hidráulica", "89.90", 9, "08", "Torneiras",
                "Prateleira B"),
            produto(36, "Torneira para Máquina de Lavar",
                "Torneira para ponto de água de máquina de lavar.",
                "Hidráulica", "29.90", 16, "08", "Torneiras",
                "Prateleira C"),
            produto(37, "Lâmpada LED 9W",
                "Lâmpada LED econômica para iluminação residencial.",
                "Iluminação", "12.90", 25, "09", "Lâmpadas LED",
                "Prateleira A"),
            produto(38, "Lâmpada LED 15W",
                "Lâmpada LED para iluminação de ambientes internos.",
                "Iluminação", "21.90", 19, "10", "Lâmpadas LED",
                "Prateleira B"),
            produto(39, "Lâmpada LED Filamento 4W",
                "Lâmpada decorativa LED de filamento.",
                "Iluminação", "24.90", 13, "10", "Lâmpadas decorativas",
                "Prateleira C"),
            produto(40, "Plafon LED de Sobrepor 18W",
                "Plafon LED para iluminação de quartos e corredores.",
                "Iluminação", "39.90", 10, "10", "Luminárias",
                "Prateleira A"),
            produto(41, "Fita LED 5m",
                "Fita LED para iluminação decorativa de ambientes.",
                "Iluminação", "69.90", 8, "10", "Iluminação decorativa",
                "Prateleira B"),
            produto(42, "Spot LED de Embutir 5W",
                "Spot LED para iluminação direcionada em ambientes internos.",
                "Iluminação", "19.90", 15, "10", "Spots",
                "Prateleira C"),
            produto(43, "Arandela Externa LED",
                "Luminária de parede para áreas externas.",
                "Iluminação", "79.90", 6, "10", "Luminárias externas",
                "Prateleira A"),
            produto(44, "Sensor de Presença para Lâmpada",
                "Sensor para acionamento automático da iluminação.",
                "Iluminação", "34.90", 9, "10", "Acessórios de iluminação",
                "Prateleira B"),
            produto(45, "Chave de Fenda 1/4 x 6 polegadas",
                "Ferramenta manual para apertar e soltar parafusos.",
                "Ferramentas", "18.90", 14, "11", "Ferramentas manuais",
                "Prateleira A"),
            produto(46, "Jogo de Chaves Allen 9 peças",
                "Conjunto de chaves Allen para reparos e montagem.",
                "Ferramentas", "29.90", 11, "12", "Ferramentas manuais",
                "Prateleira B"),
            produto(47, "Martelo Unha 25mm",
                "Martelo para trabalhos gerais e pequenos reparos.",
                "Ferramentas", "39.90", 8, "12", "Ferramentas manuais",
                "Prateleira C"),
            produto(48, "Alicate Universal 8 polegadas",
                "Alicate para segurar, cortar e dobrar materiais.",
                "Ferramentas", "34.90", 12, "12", "Ferramentas manuais",
                "Prateleira A"),
            produto(49, "Trena de 5 metros",
                "Trena retrátil para medições em obras e projetos.",
                "Ferramentas", "24.90", 20, "12", "Medição",
                "Prateleira B"),
            produto(50, "Nível de Alumínio 40cm",
                "Nível manual para conferir alinhamento e nivelamento.",
                "Ferramentas", "44.90", 7, "12", "Medição",
                "Prateleira C"),
            produto(51, "Arco de Serra Manual 12 polegadas",
                "Arco de serra para cortes em metal e plástico.",
                "Ferramentas", "32.90", 6, "12", "Ferramentas manuais",
                "Prateleira A"),
            produto(52, "Jogo de Brocas para Alvenaria",
                "Conjunto de brocas para perfuração de alvenaria.",
                "Ferramentas", "39.90", 9, "12", "Acessórios para ferramentas",
                "Prateleira B"),
            produto(53, "Torneira para Banheiro Econômica",
                "Torneira demonstrativa para lavatório de banheiro.",
                "Hidráulica", "69.90", 0, "08", "Torneiras",
                "Prateleira C"),
            produto(54, "Suporte para Prateleira",
                "Suporte metálico demonstrativo para montagem de prateleiras.",
                "Ferragens", "24.90", 12, "06", "Suportes e fixação",
                "Prateleira A"),
            produto(55, "Kit de Buchas e Parafusos",
                "Kit demonstrativo de fixadores para prateleiras e projetos domésticos.",
                "Ferragens", "19.90", 20, "06", "Fixadores",
                "Prateleira B")
        );
    }

    private Produto produto(
            int id,
            String nome,
            String descricao,
            String categoria,
            String preco,
            int quantidade,
            String corredor,
            String secao,
            String prateleira
    ) {
        return new Produto(
            id,
            nome,
            descricao,
            categoria,
            new BigDecimal(preco),
            quantidade,
            corredor,
            secao,
            prateleira,
            ""
        );
    }

    public List<Produto> listarProdutos() {
        return produtos;
    }

    public List<Produto> listarProdutosDisponiveis() {
        return produtos.stream()
            .filter(Produto::isDisponivel)
            .toList();
    }

    public List<Produto> buscarProdutos(String termo) {

        if (termo == null || termo.isBlank()) {
            return List.of();
        }

        List<String> palavras = List.of(
            normalizar(termo).split("\\s+")
        );

        return produtos.stream()
            .filter(Produto::isDisponivel)
            .filter(produto -> {

                String textoProduto = normalizar(
                    produto.getNome() + " " +
                    produto.getDescricao() + " " +
                    produto.getCategoria()
                );

                return palavras.stream()
                    .anyMatch(textoProduto::contains);
            })
            .toList();
    }

    public List<Produto> buscarProdutosDireto(String mensagem) {

        if (mensagem == null || mensagem.isBlank()) {
            return List.of();
        }

        String mensagemNormalizada = normalizar(mensagem);

        if (mensagemNormalizada.contains("pint")
                || mensagemNormalizada.contains("tinta")) {
            return produtos.stream()
                .filter(Produto::isDisponivel)
                .filter(produto ->
                    produto.getCategoria().equals("Tintas")
                        || produto.getCategoria().equals("Pintura")
                )
                .toList();
        }

        if (mensagemNormalizada.contains("torneira")) {
            return produtos.stream()
                .filter(Produto::isDisponivel)
                .filter(produto ->
                    normalizar(produto.getSecao()).contains("torneira")
                )
                .toList();
        }

        if (mensagemNormalizada.contains("prateleira")) {
            return produtos.stream()
                .filter(Produto::isDisponivel)
                .filter(produto ->
                    normalizar(produto.getNome() + " " + produto.getDescricao())
                        .contains("prateleira")
                )
                .toList();
        }

        List<String> palavrasRelevantes = Arrays.stream(
                mensagemNormalizada.split("\\s+")
            )
            .filter(palavra -> palavra.length() > 2)
            .filter(palavra -> !PALAVRAS_IGNORADAS.contains(palavra))
            .map(this::expandirTermo)
            .toList();

        return buscarProdutos(String.join(" ", palavrasRelevantes));
    }

    private String expandirTermo(String palavra) {
        return switch (palavra) {
            case "pintar", "pinte" -> "tinta";
            case "iluminar", "iluminacao" -> "lampada";
            default -> palavra;
        };
    }

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(
                texto.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
            )
            .replaceAll("\\p{M}", "")
            .replaceAll("[^\\p{Alnum}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
