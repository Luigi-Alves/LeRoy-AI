# LeRoy AI Assistant

MVP acadêmico de um assistente inteligente de compras para lojas físicas da Leroy Merlin.

O cliente descreve uma necessidade em linguagem natural, e o sistema utiliza inteligência artificial para interpretar a solicitação, explicar quais tipos de produto resolvem o problema, pesquisar um catálogo demonstrativo, apresentar recomendações e indicar suas localizações dentro de um mapa ilustrativo da loja.

O objetivo é facilitar a jornada de compra, permitindo que o cliente encontre produtos sem precisar conhecer previamente seus nomes técnicos ou depender exclusivamente do atendimento de um vendedor.

## Links da entrega

* **Aplicação publicada:** https://leroy-ai-3epl.onrender.com/
* **Resumo anônimo da coleta de dados:** https://leroy-ai-3epl.onrender.com/api/analytics/summary

## Funcionalidades

* **Interpretação com IA:** a cada mensagem, o Gemini identifica a necessidade do cliente e gera uma explicação curta com os tipos de produto indicados. A IA não informa preços, estoque nem especificações técnicas.
* **Busca inteligente:** pesquisa local por produtos e categorias, com pontuação por relevância (nome, categoria/seção e descrição). Quando a busca local não encontra nada, o termo interpretado pela IA é usado como alternativa.
* **Resultados enxutos:** no máximo 6 produtos por busca, com variedade entre seções (por exemplo, "pintar uma parede" retorna tinta, massa, lixa, rolo, fita e esmalte).
* **Funciona sem IA:** se a chave não estiver configurada, ou se o Gemini estiver indisponível ou sem cota, o sistema continua respondendo pela busca local, sem erro para o usuário.
* **Recomendação de produtos:** descrição e preços ilustrativos de cada produto.
* **Disponibilidade:** exibição da quantidade simulada de produtos disponíveis.
* **Localização:** apresentação do corredor, seção e prateleira de cada produto.
* **Mapa interativo:** mapa interno ilustrativo com 12 corredores, que permite selecionar um corredor e ver os produtos disponíveis nele.
* **Integração com o mapa:** buscas destacam automaticamente a localização do primeiro produto recomendado, com uma rota esquemática a partir da entrada.
* **Coleta anônima de interações:** pesquisas, produtos recomendados e selecionados no mapa, além das avaliações das recomendações, com resumo agregado em `/api/analytics/summary`.
* **Tratamento de estoque:** produtos sem estoque são identificados no catálogo e não são recomendados pelo chat.
* **Orientação técnica:** respostas cautelosas sobre dúvidas de instalação e compatibilidade, sem inventar especificações que não estejam disponíveis no catálogo.

### Privacidade e armazenamento das interações

* Os eventos são salvos em `data/interactions.json`, em formato JSON. O diretório `data/` é ignorado pelo Git.
* Em hospedagens com disco efêmero (como o plano gratuito do Render), esse arquivo é apagado quando o servidor reinicia. Em um produto real, os eventos iriam para um banco de dados.
* O navegador cria um identificador UUID anônimo por sessão da aba. Não são coletados nome, e-mail ou telefone.
* O tópico da pesquisa é classificado pelas categorias dos produtos encontrados (ou marcado como "sem correspondência"); o texto livre digitado não é persistido. Também são registrados identificadores e nomes de produtos do catálogo, avaliações, data/hora e o identificador anônimo.
* O endpoint `/api/analytics/summary` expõe apenas contagens agregadas, sem texto livre e sem identificadores de sessão.
* Uma falha ao gravar os eventos não altera a resposta funcional do chat; o erro é registrado no log do servidor.
* As mensagens enviadas ao assistente são limitadas a 300 caracteres e processadas pelo Gemini para interpretação.

## Dados demonstrativos

O catálogo possui 55 produtos cadastrados em memória, com informações de preços, quantidades e localizações fictícias, distribuídos em 12 corredores:

| Corredor | Conteúdo |
|---|---|
| 01 | Tintas (acrílicas, esmaltes, piso, spray) |
| 02 | Preparação de paredes |
| 03 | Acessórios de pintura |
| 04 | Proteção e acabamento |
| 05 | Torneiras |
| 06 | Hidráulica (vedação, chuveiros) |
| 07 | Lâmpadas LED |
| 08 | Luminárias e iluminação decorativa |
| 09 | Ferramentas manuais |
| 10 | Ferramentas elétricas e acessórios |
| 11 | Medição |
| 12 | Ferragens e fixação |

Os dados são utilizados exclusivamente para demonstrar o funcionamento do MVP e **não representam o catálogo, os preços, o estoque ou o layout real de nenhuma loja da Leroy Merlin**.

Um produto está cadastrado sem estoque para demonstrar o tratamento dessa situação.

## Tecnologias utilizadas

* **Java 21:** desenvolvimento do backend.
* **Maven:** gerenciamento de dependências e compilação (JAR único com Shade).
* **JDK HttpServer:** criação do servidor HTTP.
* **Gson:** conversão e manipulação de dados JSON.
* **Google Gen AI SDK:** integração com a API Gemini.
* **HTML, CSS e JavaScript:** frontend responsivo, sem frameworks.
* **Docker e Render:** empacotamento e hospedagem.

## Organização do projeto

```text
LeRoy-AI/
├── src/
│   └── main/
│       ├── java/com/leroyai/
│       │   ├── Main.java
│       │   ├── controller/
│       │   │   ├── InteractionController.java
│       │   │   └── ChatController.java
│       │   ├── model/
│       │   │   └── Produto.java
│       │   ├── service/
│       │   │   ├── GeminiService.java
│       │   │   ├── InteractionTrackingService.java
│       │   │   └── ProdutoService.java
│       │   └── util/
│       │       └── JsonUtil.java
│       └── resources/
│           └── static/
│               └── index.html
├── Dockerfile
├── pom.xml
└── README.md
```

### Responsabilidades das classes

* **Main.java:** inicialização do servidor e registro dos endpoints.
* **ChatController.java:** recebe o chat, combina IA e busca local e monta a resposta.
* **InteractionController.java:** valida e registra eventos de seleção e avaliação.
* **Produto.java:** representação dos produtos.
* **GeminiService.java:** interpreta a necessidade do cliente com o Gemini (termo de busca e explicação curta).
* **InteractionTrackingService.java:** persistência dos eventos em JSON e resumo agregado.
* **ProdutoService.java:** catálogo demonstrativo, busca com pontuação e seleção de resultados.
* **JsonUtil.java:** funções auxiliares para manipulação de JSON.

O catálogo demonstrativo é criado diretamente pelo `ProdutoService`. O projeto não usa banco de dados: por ser um MVP com dados fictícios, o catálogo fica em memória e as interações em arquivo JSON.

## Configuração e execução

### Pré-requisitos

* Java 21 ou superior.
* Maven.
* Chave de API do Google AI Studio (opcional: sem ela, o sistema usa só a busca local).

### Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `GEMINI_API_KEY` | Não | Chave do Google AI Studio. Sem ela, a IA fica desativada. |
| `GEMINI_MODEL` | Não | Modelo do Gemini. Padrão: `gemini-3.8-flash`. |
| `PORT` | Não | Porta do servidor. Padrão: `8080`. |

A chave não deve ser inserida diretamente no código, no frontend ou em arquivos versionados. O Java lê as variáveis do ambiente do processo e não carrega arquivos `.env` automaticamente.

**Linux/macOS:**

```bash
export GEMINI_API_KEY="sua-chave"
```

**Windows PowerShell:**

```powershell
$env:GEMINI_API_KEY = "sua-chave"
```

### Executando localmente

Na raiz do projeto, execute:

```bash
mvn clean package
```

Em seguida, inicie a aplicação:

```bash
java --add-modules=jdk.httpserver -jar target/leroy-ai-assistant-1.0.0.jar
```

Acesse `http://localhost:8080`. O endpoint `/api/health` informa se a IA está configurada (`"ia": true`).

### Executando com Docker

```bash
docker build -t leroy-ai .
docker run -p 8080:8080 -e GEMINI_API_KEY="sua-chave" leroy-ai
```

### Comportamento da IA

A chamada ao Gemini tem timeout de 8 segundos e uma tentativa. Se falhar (chave ausente, cota esgotada, instabilidade), o erro é registrado no log e a resposta é gerada apenas com a busca local. A IA nunca escolhe produtos nem inventa estoque ou preço: ela só interpreta o pedido e escreve a explicação curta. Os produtos vêm sempre do catálogo.

## Endpoints da API

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/health` | Estado do servidor e se a IA está configurada. |
| GET | `/api/products` | Retorna o catálogo demonstrativo, incluindo quantidades. |
| GET | `/api/search?q=torneira` | Pesquisa produtos disponíveis. |
| POST | `/api/chat` | Interpreta mensagens em linguagem natural e recomenda produtos. |
| POST | `/api/analytics/events` | Registra seleções e avaliações; recebe o identificador anônimo no cabeçalho `X-Session-Id`. |
| GET | `/api/analytics/summary` | Resumo agregado e anônimo das interações. |
| GET | `/` | Disponibiliza o frontend. |

O endpoint de eventos aceita somente `PRODUCT_SELECTION` e `RATING`. Pesquisas, mensagens, categorias e recomendações são registradas diretamente no backend.

### Exemplo de requisição para o chat

**POST `/api/chat`**

```json
{
  "mensagem": "Minha torneira está pingando"
}
```

A resposta contém os campos:

* `resposta`: mensagem apresentada ao cliente (explicação da IA, quando disponível, e aviso de dados demonstrativos).
* `termoBusca`: termo usado na pesquisa.
* `produtos`: lista de produtos encontrados (até 6).
* `interactionId`: identificador técnico da recomendação, usado para associar uma avaliação.

### Exemplo de resumo da coleta

**GET `/api/analytics/summary`** retorna contagens como interações de chat, sessões anônimas, buscas sem correspondência, categorias mais buscadas, produtos mais recomendados, produtos mais selecionados no mapa e avaliações positivas e negativas.

## Equipe e Entrega

**Grupo, integrantes:**

* RM561523: Marcos Gabriel dos Santos Silva
* RM565639: Luigi Alves Carvalho
* RM561811: Bruno Hemsi Fischman
* RM561781: Levi Kazuo Calandra Schmidt Aoshima
* RM561814: Maria Luiza Miyazaki Braz de Paula
