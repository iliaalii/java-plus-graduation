# Explore With Me

**Explore With Me** — Приложение афиша.
В этой афише можно предложить какое-либо событие от выставки до похода в кино и собрать компанию для участия в нём.

---

## Архитектура

Проект состоит из нескольких выделенных сервисов:

| Сервис                        | Назначение                                                   |
|-------------------------------|--------------------------------------------------------------|
| **event-service**             | Управление событиями (создание, публикация, просмотр)        |
| **request-service**           | Управление заявками на участие в событиях                    |
| **user-service**              | Управление пользователями                                    |
| **category-service**    | Управление категориями событий                               |
| **comment-service**           | Управление комментариями к событиям                          |
| **compilation-service** | Управление подборками событий                                |
| **stats-service**             | Сбор и предоставление статистики просмотров и взаимодействий |

### Взаимодействие между сервисами

- Все сервисы зарегистрированы в **Eureka Discovery Service**, что позволяет использовать `@FeignClient` для вызовов
  между сервисами.
- Для предотвращения падений при недоступности сервисов используется **Resilience4j Circuit Breaker**.
- Внутренний API реализован через REST-контроллеры с DTO. Каждый сервис предоставляет **fallback-фабрики**, чтобы
  клиентские сервисы получали заглушки, если сервис недоступен.

---

## Порядок запуска сервисов

Сервисы необходимо запускать в следующем порядке:

1. **discovery-server**
2. **config-server**
3. **stats-server**
4. **event-service**
5. **user-service**
6. **category-service**
7. **request-service**
8. **comment-service**
9. **gateway-server**

Сервисы **с 3 по 8** не имеют жёстких зависимостей по порядку запуска и могут быть запущены **в любом порядке** после старта `discovery-server` и `config-server`.

После запуска всех сервисов приложение полностью готово к работе и приёму внешних HTTP-запросов через **Gateway**.

---

## Внутренний API

Ниже перечислены основные эндпоинты каждого сервиса, с указанием **ответа fallback**, который возвращается, если сервис
недоступен.

### Event Service

| Метод                                 | Описание                                | Fallback                                        |
|---------------------------------------|-----------------------------------------|-------------------------------------------------|
| `GET /events/{id}`                    | Получение полной информации о событии   | `ConflictException("event-service недоступен")` |
| `GET /events/category/{catId}/exists` | Проверка наличия категории              | `true`                                          |
| `GET /events/by-ids?ids=1,2,3`        | Получение краткой информации о событиях | `Set.of()`                                      |

### Request Service

| Метод                                | Описание                                        | Fallback         |
|--------------------------------------|-------------------------------------------------|------------------|
| `GET /requests/{id}/{status}`        | Подсчёт заявок события по статусу               | `0L`             |
| `GET /requests/count?eventIds=1,2,3` | Подсчёт подтверждённых заявок по списку событий | Все значения `0` |

### User Service

| Метод                               | Описание                                        | Fallback                                       |
|-------------------------------------|-------------------------------------------------|------------------------------------------------|
| `GET /admin/users/{id}`             | Получение информации о пользователе             | `NotFoundException("user-service недоступен")` |
| `GET /admin/users/{id}/name`        | Получение имени пользователя                    | `"Unknown"`                                    |
| `GET /admin/users/by-ids?ids=1,2,3` | Получение информации о нескольких пользователях | `NotFoundException("user-service недоступен")` |

### Comment Service

| Метод                    | Описание                                | Fallback                  |
|--------------------------|-----------------------------------------|---------------------------|
| `GET /comments/{id}/all` | Получение всех комментариев для события | `Collections.emptyList()` |

### Category Service

| Метод                              | Описание                       | Fallback                                             |
|------------------------------------|--------------------------------|------------------------------------------------------|
| `GET /categories/{id}`             | Получение категории            | `ConditionsException("category-service недоступен")` |
| `GET /categories/by-ids?ids=1,2,3` | Получение нескольких категорий | `ConditionsException("category-service недоступен")` |

---

## Внешний API

Внешний REST API сервиса описан спецификацией в формате OpenAPI (Swagger).  
Эта спецификация может быть использована для генерации клиента, тестирования, или изучения доступных эндпоинтов и
структур данных.

- Основной сервис:  
  https://github.com/imaspa/java-explore-with-me-plus/blob/main/ewm-main-service-spec.json

  Эта спецификация описывает все публичные эндпоинты основного сервиса (Event, User, Request, Comment, Category) и
  включает необходимые схемы данных.


- Сервис сбора статистики::  
  https://github.com/imaspa/java-explore-with-me-plus/blob/main/ewm-stats-service-spec.json

---

## Конфигурация

Все сервисы подтягивают настройки из **Spring Cloud Config Server**:

- `server.port: 0` — позволяет запускать сервис на случайном свободном порту.
- `stats-server.url` — URL сервиса статистики.
- Инициализация SQL через `spring.sql.init.schema-locations` (используется `schema.sql`).
- Настройки подключения к базе данных и Hibernate через `spring.datasource` и `spring.jpa`
- Feign-клиенты используют таймауты подключения и чтения 5 секунд.

Пример для `event-service`:

```yaml
server:
  port: 0

stats-server:
  url: http://localhost:9090

spring:
  application:
    name: event-service
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true

  mvc:
    format:
      date: yyyy-MM-dd
      date-time: yyyy-MM-dd HH:mm:ss
      time: HH:mm:ss

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Europe/Moscow
    locale: ru_RU

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5436/event-db
    username: postgres
    password: postgres

  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect

feign:
  circuitbreaker:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
