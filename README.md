# Remessa

Projeto Java com Micronaut para gerenciamento de usuarios e remessas internacionais entre pessoas fisicas e empresas.

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

## Servicos

### user-service

Servico responsavel pelo cadastro e manutencao de usuarios.

Recursos implementados:

- Cadastro de usuarios pessoa fisica e pessoa juridica.
- Listagem de usuarios.
- Busca por ID.
- Atualizacao de usuario.
- Atualizacao de saldo em BRL e USD.
- Remocao de usuario.
- Validacao de CPF obrigatorio para pessoa fisica.
- Validacao de CNPJ obrigatorio para pessoa juridica.
- Validacao de unicidade para e-mail, CPF e CNPJ.

Tipos de usuario:

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

Configuracao local:

- Porta: `8081`
- Context path: `/api`
- Banco: H2 em memoria

### remittance-service

Servico responsavel por criar remessas internacionais entre usuarios.

Uma remessa:

- Debita um valor em BRL do usuario remetente.
- Consulta a cotacao do dolar na API PTAX do Banco Central.
- Converte o valor de BRL para USD usando `cotacaoCompra`.
- Credita o valor convertido em USD no usuario destinatario.
- Persiste o status da remessa.
- Compensa o saldo do remetente em caso de falha apos o debito.

Endpoint:

```text
POST /api/remittances
```

Configuracao local:

- Porta: `8082`
- Context path: `/api`
- URL do `user-service`: `http://localhost:8081/api`
- URL PTAX: `https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata`

## Regras de negocio

### Usuarios

- E-mails devem ser unicos.
- CPFs devem ser unicos.
- CNPJs devem ser unicos.
- Pessoa fisica (`INDIVIDUAL`) deve possuir CPF.
- Pessoa juridica (`COMPANY`) deve possuir CNPJ.

### Remessas

- Remetente e destinatario devem ser usuarios diferentes.
- O remetente precisa ter saldo BRL suficiente.
- Nao ha restricao de remessa entre PF e PJ, nem entre PJ e PF.
- PF possui limite diario de `R$ 10.000,00` em remessas concluidas.
- PJ possui limite diario de `R$ 50.000,00` em remessas concluidas.
- A conversao BRL/USD usa arredondamento `HALF_UP` com 2 casas decimais.
- A cotacao PTAX usa o formato de data `MM-dd-yyyy`.
- Quando a PTAX nao retorna cotacao, por exemplo em finais de semana, o servico usa a ultima cotacao valida obtida em cache.
- Se nao existir cotacao retornada pela PTAX nem cotacao em cache, a remessa e rejeitada.

## Organizacao do remittance-service

O `remittance-service` foi organizado para separar responsabilidades:

```text
service/ExchangeRateService.java
service/impl/PtaxExchangeRateService.java
validator/DailyRemittanceLimitValidator.java
service/impl/RemittanceServiceImpl.java
```

Responsabilidades:

- `RemittanceServiceImpl`: orquestra o fluxo da remessa.
- `PtaxExchangeRateService`: consulta PTAX, seleciona a cotacao valida e aplica cache da ultima cotacao obtida.
- `DailyRemittanceLimitValidator`: valida limites diarios de PF e PJ.
- `ExchangeRateService`: abstrai a origem da cotacao.

## Como executar localmente

Execute cada servico em um terminal separado.

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

O `user-service` deve estar em execucao antes de criar remessas, pois o `remittance-service` consulta e atualiza usuarios por HTTP.

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

O projeto possui `Dockerfile` para os dois servicos e um `docker-compose.yml` na raiz.

### Subir com Docker Compose

Na raiz do projeto:

```powershell
docker compose up --build
```

Servicos expostos:

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

- Criacao de usuarios PF e PJ.
- Validacoes de CPF/CNPJ obrigatorios.
- Validacao de e-mail, CPF e CNPJ duplicados.
- Busca, listagem, atualizacao e remocao de usuarios.
- Atualizacao de saldo.
- Tratamento de usuario nao encontrado.

### remittance-service

Possui testes cobrindo:

- Criacao de remessa.
- Validacao de remetente e destinatario diferentes.
- Saldo insuficiente.
- Conversao BRL/USD e arredondamento.
- Cotacao PTAX e fallback para cache.
- Compensacao em caso de falha ao creditar destinatario.
- Limites diarios de PF e PJ.
- Remessas PF para PJ e PJ para PF.
- Testes isolados de `PtaxExchangeRateService`.
- Testes isolados de `DailyRemittanceLimitValidator`.

Ultima validacao executada:

- `user-service`: 35 testes passando.
- `remittance-service`: 33 testes passando.

## Exemplo de criacao de usuario PF

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

## Exemplo de criacao de remessa

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

## Observacoes

- Use o Maven Wrapper de cada servico.
- Nao e necessario ter `mvn` instalado globalmente.
- O banco H2 e configurado em memoria para ambiente local e testes.
- Em Docker/Kubernetes, a configuracao atual tambem usa H2 em memoria.
- Para uso produtivo, substitua H2 por um banco persistente e configure secrets/configmaps adequados.