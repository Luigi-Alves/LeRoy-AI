package com.leroyai.service;

import com.leroyai.model.Produto;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProdutoService {

    private static final int MAX_RESULTADOS = 6;

    private static final Set<String> PALAVRAS_IGNORADAS = Set.copyOf(List.of(
        "a", "ao", "aos", "as", "com", "da", "das", "de", "do", "dos",
        "e", "em", "esta", "estou", "meu", "minha", "meus", "minhas",
        "na", "nas", "no", "nos", "o", "os", "para", "por", "preciso",
        "queria", "quero", "que", "um", "uma", "uns", "umas",
        "adequado", "adequada", "adequados", "adequadas", "pode",
        "podem", "produto", "produtos", "qual", "quais", "ser", "serve",
        "como", "onde", "comprar", "trocar", "instalar", "tenho", "fazer"
    ));

    private static final List<String> ORDEM_PINTURA = List.of(
        "Tintas acrílicas",
        "Preparação de paredes",
        "Preparação de superfícies",
        "Acessórios de pintura",
        "Proteção e acabamento",
        "Esmaltes",
        "Tintas para piso",
        "Tintas em spray"
    );

    private final List<Produto> produtos;

    public ProdutoService() {
        this.produtos = criarCatalogo();
    }

    // Catálogo fictício. Corredores: 01 Tintas, 02 Preparação de paredes,
    // 03 Acessórios de pintura, 04 Proteção e acabamento, 05 Torneiras,
    // 06 Hidráulica, 07 Lâmpadas LED, 08 Luminárias e decoração,
    // 09 Ferramentas manuais, 10 Ferramentas elétricas, 11 Medição,
    // 12 Ferragens e fixação.
    private List<Produto> criarCatalogo() {
        return List.of(
            p(1, "Tinta Acrílica Branca 18L", "Tinta branca para pintura de paredes internas.", "Tintas", "249.90", 12, "01", "Tintas acrílicas", "Prateleira A"),
            p(2, "Tinta Acrílica Azul 3,6L", "Tinta azul indicada para paredes internas.", "Tintas", "89.90", 8, "01", "Tintas acrílicas", "Prateleira B"),
            p(3, "Rolo de Pintura 23cm", "Rolo para aplicação de tinta em paredes.", "Pintura", "29.90", 25, "03", "Acessórios de pintura", "Prateleira A"),
            p(4, "Pincel 2 Polegadas", "Pincel para acabamento e pintura de superfícies.", "Pintura", "14.90", 18, "03", "Acessórios de pintura", "Prateleira B"),
            p(5, "Fita Crepe 48mm", "Fita para proteger superfícies durante a pintura.", "Pintura", "12.90", 30, "04", "Proteção e acabamento", "Prateleira A"),
            p(6, "Bandeja para Pintura", "Bandeja para facilitar a aplicação de tinta com rolo.", "Pintura", "19.90", 10, "03", "Acessórios de pintura", "Prateleira C"),
            p(7, "Torneira para Lavatório", "Torneira cromada para banheiro.", "Hidráulica", "119.90", 15, "05", "Torneiras", "Prateleira A"),
            p(8, "Fita Veda Rosca", "Fita para vedação de conexões hidráulicas.", "Hidráulica", "5.90", 40, "06", "Vedação", "Prateleira B"),
            p(9, "Lâmpada LED 12W", "Lâmpada LED branca para iluminação residencial.", "Iluminação", "17.90", 22, "07", "Lâmpadas LED", "Prateleira A"),
            p(10, "Furadeira de Impacto", "Furadeira elétrica para perfuração de diferentes superfícies.", "Ferramentas", "299.90", 5, "10", "Ferramentas elétricas", "Prateleira C"),
            p(11, "Lixa para Parede Grão 150", "Folha abrasiva para preparar paredes antes da pintura.", "Pintura", "3.90", 50, "02", "Preparação de superfícies", "Prateleira A"),
            p(12, "Massa Corrida 5,5kg", "Massa para nivelar e corrigir pequenas imperfeições em paredes internas.", "Tintas", "39.90", 14, "02", "Preparação de paredes", "Prateleira B"),
            p(13, "Luva para Pintura", "Luva de proteção para trabalhos de pintura e acabamento.", "Pintura", "12.90", 20, "03", "Acessórios de pintura", "Prateleira B"),
            p(14, "Chuveiro Elétrico 6800W", "Chuveiro elétrico com controle de temperatura para banheiro.", "Hidráulica", "149.90", 9, "06", "Chuveiros e duchas", "Prateleira B"),
            p(15, "Selador Acrílico 18L", "Fundo preparador para paredes internas antes da pintura.", "Tintas", "139.90", 7, "02", "Preparação de paredes", "Prateleira C"),
            p(16, "Espátula de Aço 10cm", "Ferramenta para aplicação de massa e acabamento em paredes.", "Pintura", "18.90", 16, "02", "Preparação de paredes", "Prateleira C"),
            p(17, "Luminária de Teto LED", "Luminária LED para iluminação de ambientes internos.", "Iluminação", "59.90", 11, "08", "Luminárias", "Prateleira C"),
            p(18, "Serrote Manual 18 polegadas", "Serrote para cortes em madeira em reparos e projetos domésticos.", "Ferramentas", "49.90", 6, "09", "Ferramentas manuais", "Prateleira C"),
            p(19, "Tinta Acrílica Cinza 18L", "Tinta cinza para pintura de paredes internas.", "Tintas", "259.90", 9, "01", "Tintas acrílicas", "Prateleira A"),
            p(20, "Tinta Acrílica Bege 3,6L", "Tinta bege para renovar ambientes internos.", "Tintas", "94.90", 11, "01", "Tintas acrílicas", "Prateleira B"),
            p(21, "Tinta para Piso Cinza 3,6L", "Tinta resistente para pintura de pisos de concreto.", "Tintas", "109.90", 6, "01", "Tintas para piso", "Prateleira C"),
            p(22, "Tinta Esmalte à Base de Água 3,6L", "Tinta para acabamento de portas e superfícies de madeira.", "Tintas", "129.90", 8, "01", "Esmaltes", "Prateleira A"),
            p(23, "Tinta Spray Branca 400ml", "Tinta spray para pequenos reparos e objetos.", "Tintas", "29.90", 20, "01", "Tintas em spray", "Prateleira B"),
            p(24, "Fundo Preparador de Paredes 18L", "Produto para preparar superfícies antes da pintura.", "Tintas", "119.90", 5, "02", "Preparação de paredes", "Prateleira C"),
            p(25, "Rolo de Pintura Antirrespingo 23cm", "Rolo para aplicação uniforme de tinta em paredes.", "Pintura", "34.90", 12, "03", "Acessórios de pintura", "Prateleira A"),
            p(26, "Trincha para Pintura 2,5 polegadas", "Pincel tipo trincha para pinturas e acabamentos.", "Pintura", "16.90", 17, "03", "Acessórios de pintura", "Prateleira B"),
            p(27, "Fita Crepe para Pintura 24mm", "Fita para mascaramento e proteção durante a pintura.", "Pintura", "8.90", 28, "04", "Proteção e acabamento", "Prateleira C"),
            p(28, "Torneira para Cozinha de Mesa", "Torneira cromada para instalação na bancada da cozinha.", "Hidráulica", "139.90", 10, "05", "Torneiras", "Prateleira A"),
            p(29, "Torneira para Cozinha de Parede", "Torneira para cozinha com instalação na parede.", "Hidráulica", "99.90", 8, "05", "Torneiras", "Prateleira B"),
            p(30, "Torneira Monocomando para Lavatório", "Torneira monocomando para banheiro.", "Hidráulica", "249.90", 4, "05", "Torneiras", "Prateleira C"),
            p(31, "Torneira com Filtro para Cozinha", "Torneira para cozinha com saída para filtro de água.", "Hidráulica", "189.90", 6, "05", "Torneiras", "Prateleira A"),
            p(32, "Torneira para Tanque", "Torneira resistente para tanque de lavanderia.", "Hidráulica", "49.90", 14, "05", "Torneiras", "Prateleira B"),
            p(33, "Torneira para Jardim", "Torneira metálica para áreas externas e jardim.", "Hidráulica", "39.90", 18, "05", "Torneiras", "Prateleira C"),
            p(34, "Torneira com Acionamento por Alavanca", "Torneira de alavanca para lavatório.", "Hidráulica", "79.90", 7, "05", "Torneiras", "Prateleira A"),
            p(35, "Torneira para Lavatório de Parede", "Torneira compacta para instalação na parede do banheiro.", "Hidráulica", "89.90", 9, "05", "Torneiras", "Prateleira B"),
            p(36, "Torneira para Máquina de Lavar", "Torneira para ponto de água de máquina de lavar.", "Hidráulica", "29.90", 16, "05", "Torneiras", "Prateleira C"),
            p(37, "Lâmpada LED 9W", "Lâmpada LED econômica para iluminação residencial.", "Iluminação", "12.90", 25, "07", "Lâmpadas LED", "Prateleira A"),
            p(38, "Lâmpada LED 15W", "Lâmpada LED para iluminação de ambientes internos.", "Iluminação", "21.90", 19, "07", "Lâmpadas LED", "Prateleira B"),
            p(39, "Lâmpada LED Filamento 4W", "Lâmpada decorativa LED de filamento.", "Iluminação", "24.90", 13, "08", "Lâmpadas decorativas", "Prateleira C"),
            p(40, "Plafon LED de Sobrepor 18W", "Plafon LED para iluminação de quartos e corredores.", "Iluminação", "39.90", 10, "08", "Luminárias", "Prateleira A"),
            p(41, "Fita LED 5m", "Fita LED para iluminação decorativa de ambientes.", "Iluminação", "69.90", 8, "08", "Iluminação decorativa", "Prateleira B"),
            p(42, "Spot LED de Embutir 5W", "Spot LED para iluminação direcionada em ambientes internos.", "Iluminação", "19.90", 15, "08", "Spots", "Prateleira C"),
            p(43, "Arandela Externa LED", "Luminária de parede para áreas externas.", "Iluminação", "79.90", 6, "08", "Luminárias externas", "Prateleira A"),
            p(44, "Sensor de Presença para Lâmpada", "Sensor para acionamento automático da iluminação.", "Iluminação", "34.90", 9, "08", "Acessórios de iluminação", "Prateleira B"),
            p(45, "Chave de Fenda 1/4 x 6 polegadas", "Ferramenta manual para apertar e soltar parafusos.", "Ferramentas", "18.90", 14, "09", "Ferramentas manuais", "Prateleira A"),
            p(46, "Jogo de Chaves Allen 9 peças", "Conjunto de chaves Allen para reparos e montagem.", "Ferramentas", "29.90", 11, "09", "Ferramentas manuais", "Prateleira B"),
            p(47, "Martelo Unha 25mm", "Martelo para trabalhos gerais e pequenos reparos.", "Ferramentas", "39.90", 8, "09", "Ferramentas manuais", "Prateleira C"),
            p(48, "Alicate Universal 8 polegadas", "Alicate para segurar, cortar e dobrar materiais.", "Ferramentas", "34.90", 12, "09", "Ferramentas manuais", "Prateleira A"),
            p(49, "Trena de 5 metros", "Trena retrátil para medições em obras e projetos.", "Ferramentas", "24.90", 20, "11", "Medição", "Prateleira B"),
            p(50, "Nível de Alumínio 40cm", "Nível manual para conferir alinhamento e nivelamento.", "Ferramentas", "44.90", 7, "11", "Medição", "Prateleira C"),
            p(51, "Arco de Serra Manual 12 polegadas", "Arco de serra para cortes em metal e plástico.", "Ferramentas", "32.90", 6, "09", "Ferramentas manuais", "Prateleira A"),
            p(52, "Jogo de Brocas para Alvenaria", "Conjunto de brocas para perfuração de alvenaria.", "Ferramentas", "39.90", 9, "10", "Acessórios para ferramentas", "Prateleira B"),
            p(53, "Torneira para Banheiro Econômica", "Torneira demonstrativa para lavatório de banheiro.", "Hidráulica", "69.90", 0, "05", "Torneiras", "Prateleira C"),
            p(54, "Suporte para Prateleira", "Suporte metálico demonstrativo para montagem de prateleiras.", "Ferragens", "24.90", 12, "12", "Suportes e fixação", "Prateleira A"),
            p(55, "Kit de Buchas e Parafusos", "Kit demonstrativo de fixadores para prateleiras e projetos domésticos.", "Ferragens", "19.90", 20, "12", "Fixadores", "Prateleira B")
        );
    }

    private Produto p(
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

    /** Busca por palavras com pontuação (nome 3, categoria/seção 2, descrição 1). */
    public List<Produto> buscarProdutos(String termo) {

        List<String> palavras = extrairPalavras(termo);

        if (palavras.isEmpty()) {
            return List.of();
        }

        Map<Produto, Integer> pontos = new LinkedHashMap<>();

        for (Produto produto : produtos) {
            if (!produto.isDisponivel()) {
                continue;
            }

            int pontuacao = pontuar(produto, palavras);

            if (pontuacao > 0) {
                pontos.put(produto, pontuacao);
            }
        }

        return pontos.entrySet().stream()
            .sorted(
                Map.Entry.<Produto, Integer>comparingByValue().reversed()
                    .thenComparing(
                        entry -> entry.getKey().getId(),
                        Comparator.naturalOrder()
                    )
            )
            .map(Map.Entry::getKey)
            .limit(MAX_RESULTADOS)
            .toList();
    }

    public List<Produto> buscarProdutosDireto(String mensagem) {

        if (mensagem == null || mensagem.isBlank()) {
            return List.of();
        }

        String mensagemNormalizada = normalizar(mensagem);

        if (mensagemNormalizada.contains("pint")
                || mensagemNormalizada.contains("tinta")) {
            List<Produto> pintura = produtos.stream()
                .filter(Produto::isDisponivel)
                .filter(produto ->
                    produto.getCategoria().equals("Tintas")
                        || produto.getCategoria().equals("Pintura")
                )
                .sorted(Comparator.comparingInt(
                    produto -> posicaoPintura(produto.getSecao())
                ))
                .toList();

            return diversificar(pintura, MAX_RESULTADOS);
        }

        if (mensagemNormalizada.contains("torneira")) {
            return produtos.stream()
                .filter(Produto::isDisponivel)
                .filter(produto ->
                    normalizar(produto.getSecao()).contains("torneira")
                )
                .limit(MAX_RESULTADOS)
                .toList();
        }

        if (mensagemNormalizada.contains("prateleira")) {
            return produtos.stream()
                .filter(Produto::isDisponivel)
                .filter(produto ->
                    normalizar(produto.getNome() + " " + produto.getDescricao())
                        .contains("prateleira")
                )
                .limit(MAX_RESULTADOS)
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

    private int posicaoPintura(String secao) {
        int indice = ORDEM_PINTURA.indexOf(secao);
        return indice < 0 ? ORDEM_PINTURA.size() : indice;
    }

    /** Pega um produto de cada seção por rodada, para variar os resultados. */
    private List<Produto> diversificar(List<Produto> lista, int limite) {

        Map<String, List<Produto>> grupos = new LinkedHashMap<>();

        for (Produto produto : lista) {
            grupos
                .computeIfAbsent(produto.getSecao(), chave -> new ArrayList<>())
                .add(produto);
        }

        List<Produto> resultado = new ArrayList<>();
        int rodada = 0;

        while (resultado.size() < limite) {
            boolean adicionou = false;

            for (List<Produto> grupo : grupos.values()) {
                if (rodada < grupo.size() && resultado.size() < limite) {
                    resultado.add(grupo.get(rodada));
                    adicionou = true;
                }
            }

            if (!adicionou) {
                break;
            }

            rodada++;
        }

        return resultado;
    }

    private List<String> extrairPalavras(String termo) {

        if (termo == null || termo.isBlank()) {
            return List.of();
        }

        String normalizado = normalizar(termo);

        if (normalizado.contains("sem resultado")) {
            return List.of();
        }

        return Arrays.stream(normalizado.split("\\s+"))
            .filter(palavra -> palavra.length() >= 3)
            .filter(palavra -> !PALAVRAS_IGNORADAS.contains(palavra))
            .map(this::singular)
            .distinct()
            .toList();
    }

    private String singular(String palavra) {
        if (palavra.length() > 4 && palavra.endsWith("s")) {
            return palavra.substring(0, palavra.length() - 1);
        }
        return palavra;
    }

    private int pontuar(Produto produto, List<String> palavras) {

        String nome = normalizar(produto.getNome());
        String grupo = normalizar(
            produto.getCategoria() + " " + produto.getSecao()
        );
        String descricao = normalizar(produto.getDescricao());

        int pontuacao = 0;

        for (String palavra : palavras) {
            if (nome.contains(palavra)) {
                pontuacao += 3;
            }
            if (grupo.contains(palavra)) {
                pontuacao += 2;
            }
            if (descricao.contains(palavra)) {
                pontuacao += 1;
            }
        }

        return pontuacao;
    }

    private String expandirTermo(String palavra) {
        return switch (palavra) {
            case "pintar", "pinte" -> "tinta";
            case "iluminar", "iluminacao" -> "lampada";
            case "pendurar" -> "suporte";
            case "furar", "furo" -> "furadeira";
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