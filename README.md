# Radpoint

Zadanie rekrutacyjne Java + SQLite (multitenancy)

## Technologie

- Java 25
- Maven
- SQLite
- JDBC
- JUnit 5
- Wbudowany `com.sun.net.httpserver.HttpServer`

Projekt nie korzysta z frameworków ORM, takich jak Hibernate czy JPA.
Dostęp do bazy danych odbywa się przez JDBC i bezpośrednie zapytania SQL.

## Wymagania

Przed uruchomieniem należy posiadać:

- JDK 25,
- Maven 3.9 lub nowszy,
- Git, jeśli projekt jest pobierany z repozytorium.

Sprawdzenie środowiska:

```bash
java -version
mvn -version
```

Maven powinien korzystać z JDK 25.

## Budowanie projektu

Aby wyczyścić poprzedni build i skompilować projekt, wykonaj:

```bash
mvn clean install
```

## Uruchomienie

Po poprawnym zakończeniu budowania uruchom serwer:

```bash
mvn exec:java
```

Alternatywnie, podczas pracy w Eclipse można uruchomić klasę:

```text
Application.java → Run As → Java Application
```

Serwer uruchamia się domyślnie na porcie `8080`:

```text
http://localhost:8080
```

Zatrzymanie serwera:

```text
Ctrl+C
```

## Struktura projektu

```text
src/
└── main/
    └── java/
        └── pl/pawelwieczorek/radpoint/
            ├── App.java
            ├── domain/
            │   └── SampleData.java
            ├── application/
            │   ├── SampleDataRepository.java
            │   └── SampleDataService.java
            ├── api/
            │   └── DataHttpHandler.java
            └── infrastructure/
                ├── tenant/
                │   └── TenantResolver.java
                └── sqlite/
                    ├── TenantConnectionProvider.java
                    └── SqliteSampleDataRepository.java
```

### Opis warstw

- `domain` zawiera model domenowy, w postaci `SampleData`.
- `application` zawiera przypadki użycia oraz interfejs repozytorium.
- `api` obsługuje żądania HTTP, routing, statusy i format JSON.
- `infrastructure.tenant` odpowiada za identyfikację tenant-a z nagłówków.
- `infrastructure.sqlite` zawiera implementację JDBC, SQLite, inicjalizację
  tabeli oraz cache połączeń.
- `App` tworzy zależności i uruchamia serwer HTTP.

## Identyfikacja tenant-a

Tenant może zostać wskazany na dwa sposoby.

### Nagłówek `X-Tenant-ID`

Nagłówek ma najwyższy priorytet:

```text
X-Tenant-ID: tenant1
```

Przykład:

```bash
curl.exe http://localhost:8080/data \
  -H "X-Tenant-ID: tenant1"
```

### Nagłówek `Host`

Aplikacja obsługuje format:

```text
<environment>.<tenant>.example.com
```

Przykład:

```text
dev.newco.example.com
```

W tym przypadku tenant-em jest:

```text
newco
```

Przykład:

```bash
curl.exe http://localhost:8080/data \
  -H "Host: dev.newco.example.com"
```

Jeżeli jednocześnie podano `X-Tenant-ID` i `Host`, aplikacja używa
wartości z `X-Tenant-ID`.

## Walidacja tenant-a

Identyfikator tenant-a:

- musi mieć od 1 do 32 znaków,
- może zawierać wyłącznie małe litery i cyfry `0-9`.

Przykładowe poprawne wartości:

```text
tenant1
newco
company123
```

Przykładowe niepoprawne wartości:

```text
tenant-1
tenant_1
Tenant1
../../secret
```

Ograniczenie zabezpiecza również nazwę ścieżki do bazy SQLite.

## Bazy danych

Dla każdego tenant-a tworzony jest osobny plik:

```text
tenants/<tenant-id>.db
```

Przykład:

```text
tenants/tenant1.db
tenants/newco.db
```

Jeżeli baza nie istnieje, aplikacja:

1. tworzy katalog `tenants/`,
2. tworzy plik SQLite,
3. tworzy bazę i tabelę `sample_data`, jeśli jeszcze nie istnieją,
4. zapamiętuje połączenie w cache.

Tabela jest tworzona za pomocą:

```sql
CREATE TABLE IF NOT EXISTS sample_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    value TEXT NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0
);
```

Połączenia są przechowywane w `ConcurrentHashMap`, dzięki czemu ta sama
baza nie jest otwierana ponownie przy każdym żądaniu.

## Endpointy

### GET `/data`

Zwraca aktywne dane bieżącego tenant-a.

```bash
curl.exe http://localhost:8080/data \
  -H "X-Tenant-ID: tenant1"
```

Przykładowa odpowiedź:

```json
[
  {
    "id": 1,
    "value": "Pierwszy rekord"
  }
]
```

Jeżeli tenant nie ma rekordów:

```json
[]
```

### POST `/data`

Dodaje rekord do bazy bieżącego tenant-a.

```bash
curl.exe -X POST http://localhost:8080/data \
  -H "X-Tenant-ID: tenant1" \
  -H "Content-Type: application/json" \
  -d "{\"value\":\"Pierwszy rekord\"}"
```

Przykładowa odpowiedź:

```json
{
  "id": 1,
  "value": "Pierwszy rekord"
}
```

### DELETE `/data/{id}`

Wykonuje soft delete rekordu.

```bash
curl.exe -X DELETE http://localhost:8080/data/1 \
  -H "X-Tenant-ID: tenant1"
```

Przykładowa odpowiedź:

```json
{
  "deleted": true
}
```

Rekord nie jest usuwany fizycznie. Jego kolumna `deleted` zostaje ustawiona
na `1`, dlatego nie pojawia się później w odpowiedzi `GET /data`.

## Test izolacji tenantów

Dodaj dane dla dwóch tenantów:

```bash
curl.exe -X POST http://localhost:8080/data \
  -H "X-Tenant-ID: tenant1" \
  -H "Content-Type: application/json" \
  -d "{\"value\":\"Dane tenant1\"}"
```

```bash
curl.exe -X POST http://localhost:8080/data \
  -H "X-Tenant-ID: tenant2" \
  -H "Content-Type: application/json" \
  -d "{\"value\":\"Dane tenant2\"}"
```

Następnie pobierz dane osobno:

```bash
curl.exe http://localhost:8080/data \
  -H "X-Tenant-ID: tenant1"
```

```bash
curl.exe http://localhost:8080/data \
  -H "X-Tenant-ID: tenant2"
```

Każdy tenant powinien otrzymać wyłącznie swoje dane. W katalogu `tenants/`
powinny powstać dwa niezależne pliki:

```text
tenants/
├── tenant1.db
└── tenant2.db
```

## Obsługa błędów

Przykładowe błędy zwracane przez aplikację:

- `400 Bad Request` — brak lub niepoprawny tenant,
- `400 Bad Request` — niepoprawne body albo ID,
- `404 Not Found` — nieistniejący endpoint lub rekord,
- `500 Internal Server Error` — błąd wewnętrzny albo błąd SQLite.

Odpowiedzi błędów mają format JSON:

```json
{
  "error": "Invalid tenant ID."
}
```

## Przyjęte uproszczenia

- Aplikacja korzysta z wbudowanego serwera HTTP JDK zamiast frameworka webowego.
- Dla uproszczenia obsługiwany jest JSON zawierający pole `value`.
- Pliki baz SQLite są lokalne i nie są przeznaczone do współdzielenia między wieloma instancjami aplikacji.
- Połączenia są przechowywane w pamięci i zamykane podczas zatrzymywania aplikacji.
- Pliki baz danych nie są przechowywane w repozytorium Git.
