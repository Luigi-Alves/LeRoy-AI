
# LeRoy AI Assistant

MVP acadêmico de um assistente inteligente de compras para lojas físicas da Leroy Merlin.

O cliente descreve uma necessidade em linguagem natural, e o sistema utiliza inteligência artificial para interpretar a solicitação, pesquisar um catálogo demonstrativo, apresentar recomendações de produtos e indicar suas localizações dentro de um mapa ilustrativo da loja.

O objetivo é facilitar a jornada de compra, permitindo que o cliente encontre produtos sem precisar conhecer previamente seus nomes técnicos ou depender exclusivamente do atendimento de um vendedor.

## Funcionalidades

- **Busca inteligente:** pesquisa local por produtos e categorias.
- **Interpretação com IA:** utilização do Gemini para interpretar buscas que não possuem correspondência direta no catálogo.
- **Recomendação de produtos:** apresentação de produtos relacionados à necessidade do cliente, com descrição e preços ilustrativos.
- **Disponibilidade:** exibição da quantidade simulada de produtos disponíveis.
- **Localização:** apresentação do corredor, seção e prateleira de cada produto.
- **Mapa interativo:** mapa interno ilustrativo que permite selecionar corredores e visualizar os produtos disponíveis em cada um.
- **Integração com o mapa:** buscas destacam automaticamente a localização do primeiro produto recomendado.
- **Coleta anônima de interações:** pesquisas, mensagens, produtos recomendados e selecionados no mapa, além das avaliações das recomendações.
- **Tratamento de estoque:** produtos sem estoque são identificados no catálogo e não são recomendados pelo chat.
- **Orientação técnica:** respostas cautelosas sobre dúvidas de instalação e compatibilidade, sem inventar especificações que não estejam disponíveis no catálogo.

### Privacidade e armazenamento das interações

- Os eventos são salvos em `data/interactions.json`, em formato JSON, e permanecem disponíveis após reiniciar o servidor. O diretório `data/` é ignorado pelo Git.
- O navegador cria um identificador UUID anônimo por sessão da aba. Não são coletados nome, e-mail ou telefone como campos do evento.
- O tópico da pesquisa é classificado pelas categorias dos produtos encontrados (ou marcado como “sem correspondência”); o texto livre digitado não é persistido. Também são registrados identificadores e nomes de produtos do catálogo, avaliações, data/hora e identificador anônimo.
- Uma falha ao gravar os eventos não altera a resposta funcional do chat; o erro é registrado no log do servidor.

## Dados demonstrativos

O catálogo possui 55 produtos cadastrados em memória, com informações de preços, quantidades e localizações fictícias.

Os dados são utilizados exclusivamente para demonstrar o funcionamento do MVP e **não representam o catálogo, os preços, o estoque ou o layout real de nenhuma loja da Leroy Merlin**.

O catálogo contempla diferentes categorias, como:

- Tintas e pintura.
- Ferramentas.
- Iluminação.
- Hidráulica.
- Torneiras.
- Ferragens.

Um produto está cadastrado sem estoque para demonstrar o tratamento dessa situação.

## Tecnologias utilizadas

- **Java 21:** desenvolvimento do backend.
- **Maven:** gerenciamento de dependências e compilação.
- **JDK HttpServer:** criação do servidor HTTP.
- **Gson:** conversão e manipulação de dados JSON.
- **Google Gen AI SDK:** integração com a API Gemini.
- **HTML:** estrutura do frontend.
- **CSS:** estilização e responsividade.
- **JavaScript:** interações e comunicação com o backend.

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
├── data/
├── pom.xml
└── README.md
```

### Responsabilidades das classes

- **Main.java:** inicialização do servidor e registro dos endpoints.
- **ChatController.java:** recebimento e processamento das requisições do chat.
- **InteractionController.java:** valida e registra eventos de seleção/avaliação.
- **Produto.java:** representação dos produtos.
- **GeminiService.java:** integração com a inteligência artificial Gemini.
- **InteractionTrackingService.java:** persistência dos eventos em JSON.
- **ProdutoService.java:** gerenciamento do catálogo demonstrativo e pesquisa de produtos.
- **JsonUtil.java:** funções auxiliares para manipulação de JSON.

O catálogo demonstrativo é criado diretamente pelo `ProdutoService`. O projeto não depende de conexão com banco de dados próprio.

## Configuração e execução

### Pré-requisitos

- Java 21 ou superior.
- Maven.
- Chave de API do Google AI Studio.

### Configuração da API Gemini

Configure sua chave como variável de ambiente `GEMINI_API_KEY`.

A chave não deve ser inserida diretamente no código, no frontend ou em arquivos versionados.
O Java lê a variável do ambiente do processo e não carrega arquivos `.env` automaticamente.

**Linux/macOS:**

```bash
export GEMINI_API_KEY="sua-chave"
```

**Windows PowerShell:**

```powershell
$env:GEMINI_API_KEY = "sua-chave"
```

### Executando o projeto

Na raiz do projeto, execute:

```bash
mvn clean package
```

Em seguida, inicie a aplicação:

```bash
java --add-modules=jdk.httpserver -jar target/leroy-ai-assistant-1.0.0.jar
```

O servidor utiliza a porta `8080` por padrão.

Para utilizar outra porta, configure a variável de ambiente `PORT`.

**Observação:** buscas resolvidas diretamente pelo catálogo local não precisam chamar o Gemini. Buscas que dependem da interpretação da IA necessitam de uma chave válida, conexão com a internet e disponibilidade da API.

A chamada ao Gemini possui timeout de 15 segundos e uma tentativa, evitando que o chat fique aguardando novas tentativas prolongadas em caso de limitação de cota.

## Endpoints da API

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/health` | Verifica o estado do servidor. |
| GET | `/api/products` | Retorna o catálogo demonstrativo, incluindo quantidades. |
| GET | `/api/search?q=torneira` | Pesquisa produtos disponíveis. |
| POST | `/api/chat` | Interpreta mensagens em linguagem natural e recomenda produtos. |
| POST | `/api/analytics/events` | Registra seleções e avaliações; recebe o identificador anônimo no cabeçalho `X-Session-Id`. |
| GET | `/` | Disponibiliza o frontend. |

O endpoint de coleta aceita somente `PRODUCT_SELECTION` e `RATING`. Pesquisas, mensagens, categorias e recomendações são registradas diretamente no backend. Os dados não são expostos por uma interface ou endpoint de consulta.

### Exemplo de requisição para o chat

**POST `/api/chat`**

```json
{
  "mensagem": "Minha torneira está pingando"
}
```

A resposta contém os campos:

- `resposta`: mensagem apresentada ao cliente.
- `termoBusca`: termo identificado para a pesquisa.
- `produtos`: lista de produtos encontrados.

- `interactionId`: identificador técnico da recomendação para associar uma avaliação.


## Equipe e Entrega

**Grupo — Integrantes:**

- RM561523 — Marcos Gabriel dos Santos Silva
- RM565639 — Luigi Alves Carvalho
- RM561811 — Bruno Hemsi Fischman
- RM561781 — Levi Kazuo Calandra Schmidt Aoshima
- RM561814 — Maria Luiza Miyazaki Braz de Paula
