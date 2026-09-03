# MBA Hexagonal Architecture

Projeto multi-módulo em Java 17 (Gradle) organizado em `domain`, `application` e
`infrastructure`, seguindo arquitetura hexagonal (ports & adapters).

## Como subir o projeto

Pré-requisitos: Java 17 e Docker.

```bash
docker-compose up -d          # sobe o MySQL usado pela aplicação
./gradlew :infrastructure:bootRun
```

A API REST fica disponível em `http://localhost:8080` e o GraphiQL em
`http://localhost:8080/graphiql`.

## Como rodar a suíte de testes

```bash
./gradlew test
```

Isso executa os testes de domínio (`:domain:test`), de casos de uso com
repositórios em memória (`:application:test`) e os testes de integração/REST
sobre um banco H2 em memória (`:infrastructure:test`).

## Cancelamento de evento e a cascata assíncrona

Ao cancelar um evento (`POST /events/{id}/cancel` ou a mutation GraphQL
`cancelEvent`), o agregado `Event` registra o evento de domínio
`EventCancelled` (`domain/.../event/EventCancelled.java`). Esse evento é
gravado na tabela `outbox` na mesma transação em que o evento é salvo
(`EventDatabaseRepository`), e publicado periodicamente pelo `OutboxRelay`
(`infrastructure/.../job/OutboxRelay.java`) na fila.

O `ConsumerQueueGateway` (`infrastructure/.../gateways/ConsumerQueueGateway.java`)
identifica o tipo `event.cancelled` e aciona, de forma assíncrona, o caso de
uso `CancelEventTicketsUseCase` (`application/.../ticket/CancelEventTicketsUseCase.java`),
que busca todos os ingressos do evento (`TicketRepository.ticketsByEventId`) e
os cancela um a um. O agregado `Event` nunca chama o agregado `Ticket`
diretamente — a cascata acontece inteiramente reagindo ao evento de domínio
que trafega pela outbox e pela fila, no mesmo molde do fluxo existente de
`EventTicketReserved` → `CreateTicketForCustomerUseCase`.
