# Remessa

Projeto Java com Micronaut para gerenciamento de usuários e remessas internacionais.

## Estrutura

```text
remessa/
├── user-service/
├── remittance-service/
└── core/
```

## Tecnologias

- Java 21
- Micronaut
- Maven Wrapper
- Micronaut Data Hibernate JPA
- H2 Database
- JUnit 5
- Mockito
- Lombok

## Serviços

### user-service

Serviço responsável pelo cadastro e manutenção de usuários.

Recursos implementados:

- Cadastro de usuário
- Listagem de usuários
- Busca por ID
- Atualização de usuário
- Remoção de usuário
- Validação de CPF para pessoa física
- Validação de CNPJ para pessoa jurídica
- Validação de unicidade para e-mail, CPF e CNPJ

O ID do usuário utiliza `UUID`.

Tipos de usuário:

- `INDIVIDUAL`
- `COMPANY`

Endpoints:

```text
POST   /api/users
GET    /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
PUT    /api/users/{id}/balance
DELETE /api/users/{id}
```

Configuração local:

- Porta: `8081`
- Context path: `/api`
- Banco: H2 em memória

### remittance-service

Serviço responsável por executar remessas entre usuários.

Uma remessa:

- Debita um valor em Real do usuário remetente
- Consulta a cotação do Dólar na API PTAX do Banco Central
- Converte o valor de Real para Dólar usando o campo `cotacaoCompra`
- Credita o valor convertido em Dólar no usuário destinatário

Endpoint:

```text
POST /api/remittances
```

Configuração local:

- Porta: `8082`
- Context path: `/api`
- URL do `user-service`: `http://localhost:8081/api`
- URL PTAX: `https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata`

## Como executar

### user-service

Executar a partir da pasta `user-service`:

```powershell
.\mvnw.bat mn:run
```

### remittance-service

Executar a partir da pasta `remittance-service`:

```powershell
.\mvnw.bat mn:run
```

## Documentação Swagger

Com o `user-service` em execução, a documentação da API pode ser acessada pelo navegador.

Swagger UI:

```text
http://localhost:8081/api/swagger-ui
```

Arquivo OpenAPI gerado:

```text
http://localhost:8081/api/swagger/user-service-0.1.yml
```

Caso o navegador não abra a UI diretamente, acesse:

```text
http://localhost:8081/api/swagger-ui/index.html
```

## Como testar

### user-service

Executar a partir da pasta `user-service`:

```powershell
.\mvnw.bat test
```

### remittance-service

Executar a partir da pasta `remittance-service`:

```powershell
.\mvnw.bat test
```

## Testes

O `user-service` possui testes com JUnit 5 e Mockito cobrindo as principais regras de negócio do `UserServiceImpl`, incluindo:

- Criação de usuários pessoa física e empresa
- Validação de documentos obrigatórios
- Validação de e-mail, CPF e CNPJ duplicados
- Busca de usuários
- Atualização de usuários
- Remoção de usuários
- Tratamento de usuário não encontrado

## Exemplo de criação de usuário

```json
{
  "fullName": "John Doe",
  "email": "john.doe@email.com",
  "password": "password",
  "type": "INDIVIDUAL",
  "cpf": "12345678901",
  "cnpj": null,
  "brlBalance": 1000.00,
  "usdBalance": 100.00
}
```

## Exemplo de criação de remessa

```json
{
  "senderUserId": "00000000-0000-0000-0000-000000000001",
  "receiverUserId": "00000000-0000-0000-0000-000000000002",
  "brlAmount": 500.00,
  "quotationDate": "2025-01-30"
}
```

Exemplo de chamada:

```text
POST http://localhost:8082/api/remittances
```

Antes de chamar a API de remessa, o `user-service` precisa estar em execução, pois o `remittance-service` consulta e atualiza os saldos dos usuários por HTTP.

## Observações

- Use o Maven Wrapper de cada serviço.
- Não é necessário ter `mvn` instalado globalmente.
- O banco H2 é configurado em memória para ambiente local/testes.
