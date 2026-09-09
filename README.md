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

## Testowanie

Endpoint `GET /data` został początkowo przetestowany z poziomu PowerShella.
Ze względu na problemy z przekazywaniem body JSON w PowerShellu, pozostałe testy
zostały wykonane w Postmanie, który ułatwia wysyłanie żądań HTTP z nagłówkami
oraz body w formacie JSON.

## Przyjęte uproszczenia

- Aplikacja korzysta z wbudowanego serwera HTTP JDK zamiast frameworka webowego.
- Dla uproszczenia obsługiwany jest JSON zawierający pole `value`.
- Pliki baz SQLite są lokalne i nie są przeznaczone do współdzielenia między wieloma instancjami aplikacji.
- Połączenia są przechowywane w pamięci i zamykane podczas zatrzymywania aplikacji.
- Pliki baz danych nie są przechowywane w repozytorium Git.
