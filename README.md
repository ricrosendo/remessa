# Remessa

Projeto Java com Micronaut para gerenciamento de usuários e remessas internacionais entre pessoas físicas e empresas.

## Estrutura

```text
remessa/
├── user-service/
├── remittance-service/
├── core/
├── k8s/
├── docker-compose.yml
└── README.md
```

## Tecnologias

- Java 21
- Micronaut 4.6.3
- Maven Wrapper
- Micronaut Data Hibernate JPA
- H2 Database
- Micronaut HTTP Client
- Micronaut OpenAPI / Swagger UI
- Docker
- Kubernetes
- JUnit 5
- Mockito

## Serviços

### user-service

Serviço responsável pelo cadastro e manutenção de usuários.

Recursos implementados:

- Cadastro de usuários pessoa física e pessoa jurídica.
- Listagem de usuários.
- Busca por ID.
- Atualização de usuário.
- Atualização de saldo em BRL e USD.
- Remoção de usuário.
- Validação de CPF obrigatório para pessoa física.
- Validação de CNPJ obrigatório para pessoa jurídica.
- Validação de unicidade para e-mail, CPF e CNPJ.

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

Serviço responsável por criar remessas internacionais entre usuários.

Uma remessa:

- Debita um valor em BRL do usuário remetente.
- Consulta a cotação do dólar na API PTAX do Banco Central.
- Converte o valor de BRL para USD usando `cotacaoCompra`.
- Credita o valor convertido em USD no usuário destinatário.
- Persiste o status da remessa.
- Compensa o saldo do remetente em caso de falha após o débito.

Endpoint:

```text
POST /api/remittances
```

Configuração local:

- Porta: `8082`
- Context path: `/api`
- URL do `user-service`: `http://localhost:8081/api`
- URL PTAX: `https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata`

## Regras de negócio

### Usuários

- E-mails devem ser únicos.
- CPFs devem ser únicos.
- CNPJs devem ser únicos.
- Pessoa física (`INDIVIDUAL`) deve possuir CPF.
- Pessoa jurídica (`COMPANY`) deve possuir CNPJ.

### Remessas

- Remetente e destinatário devem ser usuários diferentes.
- O remetente precisa ter saldo BRL suficiente.
- Não há restrição de remessa entre PF e PJ, nem entre PJ e PF.
- PF possui limite diário de `R$ 10.000,00` em remessas concluídas.
- PJ possui limite diário de `R$ 50.000,00` em remessas concluídas.
- A conversão BRL/USD usa arredondamento `HALF_UP` com 2 casas decimais.
- A cotação PTAX usa o formato de data `MM-dd-yyyy`.
- Quando a PTAX não retorna cotação, por exemplo em finais de semana, o serviço usa a última cotação válida obtida em cache.
- Se não existir cotação retornada pela PTAX nem cotação em cache, a remessa é rejeitada.

## Organização do remittance-service

O `remittance-service` foi organizado para separar responsabilidades:

```text
service/ExchangeRateService.java
service/impl/PtaxExchangeRateService.java
validator/DailyRemittanceLimitValidator.java
service/impl/RemittanceServiceImpl.java
```

Responsabilidades:

- `RemittanceServiceImpl`: orquestra o fluxo da remessa.
- `PtaxExchangeRateService`: consulta PTAX, seleciona a cotação válida e aplica cache da última cotação obtida.
- `DailyRemittanceLimitValidator`: valida limites diários de PF e PJ.
- `ExchangeRateService`: abstrai a origem da cotação.

## Como executar localmente

Execute cada serviço em um terminal separado.

### user-service

```powershell
cd user-service
.\mvnw.bat mn:run
```

### remittance-service

```powershell
cd remittance-service
.\mvnw.bat mn:run
```

O `user-service` deve estar em execução antes de criar remessas, pois o `remittance-service` consulta e atualiza usuários por HTTP.

## Swagger / OpenAPI

### user-service

Swagger UI:

```text
http://localhost:8081/api/swagger-ui/
```

OpenAPI:

```text
http://localhost:8081/api/swagger/user-service-0.1.yml
```

### remittance-service

Swagger UI:

```text
http://localhost:8082/api/swagger-ui/
```

OpenAPI:

```text
http://localhost:8082/api/swagger/swagger.yml
```

## Docker

O projeto possui `Dockerfile` para os dois serviços e um `docker-compose.yml` na raiz.

### Subir com Docker Compose

Na raiz do projeto:

```powershell
docker compose up --build
```

Serviços expostos:

- `user-service`: `http://localhost:8081/api`
- `remittance-service`: `http://localhost:8082/api`

No Docker Compose, o `remittance-service` acessa o `user-service` pela URL interna:

```text
http://user-service:8081/api
```

### Parar containers

```powershell
docker compose down
```

## Kubernetes

Os manifests Kubernetes ficam em:

```text
k8s/
├── user-service.yaml
├── remittance-service.yaml
└── README.md
```

### Build das imagens locais

Na raiz do projeto:

```powershell
docker build -t remessa/user-service:0.1 ./user-service
docker build -t remessa/remittance-service:0.1 ./remittance-service
```

### Aplicar manifests

```powershell
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/remittance-service.yaml
```

### Acessar localmente com port-forward

```powershell
kubectl port-forward service/user-service 8081:8081
kubectl port-forward service/remittance-service 8082:8082
```

### Remover recursos

```powershell
kubectl delete -f k8s/remittance-service.yaml
kubectl delete -f k8s/user-service.yaml
```

## Como testar

### user-service

A partir da pasta `user-service`:

```powershell
.\mvnw.bat test
```

### remittance-service

A partir da pasta `remittance-service`:

```powershell
.\mvnw.bat test
```

## Cobertura de testes

### user-service

Possui testes cobrindo:

- Criação de usuários PF e PJ.
- Validações de CPF/CNPJ obrigatórios.
- Validação de e-mail, CPF e CNPJ duplicados.
- Busca, listagem, atualização e remoção de usuários.
- Atualização de saldo.
- Tratamento de usuário não encontrado.

### remittance-service

Possui testes cobrindo:

- Criação de remessa.
- Validação de remetente e destinatário diferentes.
- Saldo insuficiente.
- Conversão BRL/USD e arredondamento.
- Cotação PTAX e fallback para cache.
- Compensação em caso de falha ao creditar destinatário.
- Limites diários de PF e PJ.
- Remessas PF para PJ e PJ para PF.
- Testes isolados de `PtaxExchangeRateService`.
- Testes isolados de `DailyRemittanceLimitValidator`.

Última validação executada:

- `user-service`: 35 testes passando.
- `remittance-service`: 33 testes passando.

## Exemplo de criação de usuário PF

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

Chamada:

```text
POST http://localhost:8081/api/users
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

Chamada:

```text
POST http://localhost:8082/api/remittances
```

## Observações

- Use o Maven Wrapper de cada serviço.
- Não é necessário ter `mvn` instalado globalmente.
- O banco H2 é configurado em memória para ambiente local e testes.
- Em Docker/Kubernetes, a configuração atual também usa H2 em memória.
- Para uso produtivo, substitua H2 por um banco persistente e configure secrets/configmaps adequados.