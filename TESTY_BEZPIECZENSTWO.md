# 🔒 Bezpieczeństwo Testów na Bazie Produkcyjnej

## Czy testy modyfikują dane produkcyjne?

**NIE!** Testy są całkowicie bezpieczne dzięki `@Transactional`.

## Jak działa @Transactional w testach?

```
┌─────────────────────────────────────────────────────────────┐
│ TEST START                                                  │
│ ↓                                                           │
│ @Transactional → BEGIN TRANSACTION                         │
│ ↓                                                           │
│ Wykonaj operacje:                                          │
│   - INSERT draft changes                                   │
│   - UPDATE project products                                │
│   - DELETE starych rekordów                                │
│ ↓                                                           │
│ Sprawdź wyniki (assertions)                                │
│ ↓                                                           │
│ @Transactional → ROLLBACK (automatycznie!)                │
│ ↓                                                           │
│ TEST END                                                    │
│                                                             │
│ ✅ WSZYSTKIE ZMIANY COFNĘTE!                               │
│ ✅ Użytkownik NIE ZOBACZY zmian                            │
│ ✅ Baza produkcyjna NIE ZOSTAJE zmodyfikowana             │
└─────────────────────────────────────────────────────────────┘
```

## Co się dzieje krok po kroku?

### 1. **Przed testem:**
- Baza: `project_draft_changes_ws` - 0 rekordów
- Baza: `project_products` - dane produkcyjne (bez zmian)

### 2. **Podczas testu:**
- Test tworzy transakcję (`BEGIN TRANSACTION`)
- Wykonuje operacje:
  - INSERT 8685 draft changes
  - UPDATE project products
  - DELETE starych rekordów
- **Wszystko jest w transakcji (nie commitowane!)**

### 3. **Sprawdzenie wyników:**
- Test sprawdza czy wszystko działa (assertions)
- **Dane są widoczne TYLKO w tej transakcji**

### 4. **Po teście:**
- `@Transactional` automatycznie wykonuje `ROLLBACK`
- **Wszystkie zmiany są cofnięte**
- Baza wraca do stanu sprzed testu

## Przykład:

```java
@Test
@Transactional  // ← To zapewnia rollback!
void testSaveDraftChanges() {
    // BEFORE: project_draft_changes_ws = 0 rekordów
    
    projectService.saveDraftChanges(projectId, request);
    // W transakcji: project_draft_changes_ws = 8685 rekordów
    
    assertEquals(8685, draftChanges.size());  // ✅ Sprawdza w transakcji
    
    // AFTER: @Transactional → ROLLBACK
    // project_draft_changes_ws = 0 rekordów (jak przed testem)
}
```

## Czy użytkownik zobaczy zmiany?

**NIE!** 

- Testy używają **osobnej transakcji**
- Zmiany są **widoczne tylko w tej transakcji**
- Po teście → **ROLLBACK** → zmiany znikają
- Użytkownik **NIE ZOBACZY** żadnych zmian

## Czy mogę użyć tej samej bazy co produkcja?

**TAK!** Jest to **BEZPIECZNE** dzięki `@Transactional`:

✅ **Zalety:**
- Nie musisz tworzyć osobnej bazy
- Testujesz na prawdziwych danych (struktura, indeksy, etc.)
- Rollback automatyczny

⚠️ **Uwagi:**
- Testy mogą **spowolnić** bazę podczas wykonywania (ale rollback na końcu)
- Długie testy mogą **blokować** tabele (ale tylko w transakcji testowej)
- Jeśli test się **crashuje**, rollback może nie zadziałać (rzadko)

## Alternatywa: Osobna baza testowa

Jeśli chcesz być **100% pewny**, możesz użyć osobnej bazy:

```sql
CREATE DATABASE defaultdb_test;
```

I zmień w `application-test-mysql.properties`:
```properties
# Osobna baza testowa
spring.datasource.url=jdbc:mysql://.../defaultdb_test?...
```

## Rekomendacja

**Dla większości przypadków:**
- ✅ Użyj **tej samej bazy** co produkcja
- ✅ `@Transactional` zapewnia bezpieczeństwo
- ✅ Rollback automatyczny

**Jeśli masz obawy:**
- ⚠️ Utwórz **osobną bazę testową** (`defaultdb_test`)
- ⚠️ Zmień URL w `application-test-mysql.properties`

## Podsumowanie

| Aspekt | Ta sama baza + @Transactional | Osobna baza testowa |
|--------|-------------------------------|---------------------|
| **Bezpieczeństwo** | ✅ Bezpieczne (rollback) | ✅ Bezpieczne |
| **Wymagania** | ✅ Brak (używa istniejącej) | ⚠️ Wymaga utworzenia bazy |
| **Realizm** | ✅ Wysoki (prawdziwe dane) | ⚠️ Średni (pusta baza) |
| **Wydajność** | ⚠️ Może spowolnić | ✅ Nie wpływa na prod |
| **Rekomendacja** | ✅ **Dla większości** | ⚠️ Jeśli masz obawy |

---

**Wniosek:** `@Transactional` w testach zapewnia **100% bezpieczeństwo**. Możesz użyć tej samej bazy co produkcja bez obaw! 🎯




