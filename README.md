# Radpoint

Zadanie rekrutacyjne Java + SQLite (multitenancy)


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

## Endpointy

Endpointy były testowane za pomocą poniższych instrukcji poprzez PowerShella

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

## Przyjęte uproszczenia

- Aplikacja korzysta z wbudowanego serwera HTTP JDK zamiast frameworka webowego.
- Dla uproszczenia obsługiwany jest JSON zawierający pole `value`.
- Pliki baz SQLite są lokalne i nie są przeznaczone do współdzielenia między wieloma instancjami aplikacji.
- Połączenia są przechowywane w pamięci i zamykane podczas zatrzymywania aplikacji.
- Pliki baz danych nie są przechowywane w repozytorium Git.
