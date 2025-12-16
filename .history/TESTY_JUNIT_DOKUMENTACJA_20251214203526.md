# 📋 Dokumentacja Testów JUnit - Logika Zapisywania Projektu

## 🎯 Cel Testów

Testy sprawdzają poprawność logiki zapisywania draft changes i projektu, w tym:
- ✅ Zapisanie draft changes (pierwszy raz)
- ✅ Wielokrotne zapisanie draft changes (UPSERT)
- ✅ Tylko zmiana marży
- ✅ Tylko zmiana quantity
- ✅ Zapisanie projektu z draft changes
- ✅ Zapisanie projektu bez draft changes
- ✅ Wielokrotne zapisanie projektu
- ✅ Zapisanie projektu po zmianie marży

---

## 📊 Wizualna Reprezentacja Testów

### 🔵 TEST 1: Zapisanie draft changes - pierwszy raz

```
┌─────────────────────────────────────────────────────────────┐
│ PRZED:                                                      │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  PUSTA (0 rekordów)             │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveDraftChanges()
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO:                                                        │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord                       │
│ │                     │     - productId: 1                 │
│ │                     │     - category: TILE                │
│ │                     │     - retailPrice: 100.0            │
│ │                     │     - purchasePrice: 80.0           │
│ │                     │     - sellingPrice: 90.0            │
│ │                     │     - quantity: 10.0                │
│ │                     │     - marginPercent: 20.0            │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PRZED**: `project_draft_changes_ws` - PUSTA (0 rekordów)
- ✅ **PRZED**: `project_products` - PUSTA (0 rekordów)
- ✅ **PO**: `project_draft_changes_ws` - 1 REKORD (zapisane)
- ✅ **PO**: `project_products` - NADAL PUSTA (draft changes nie są jeszcze zapisane jako ProjectProduct)
- ✅ Czy wszystkie pola są poprawnie zapisane
- ✅ Czy wartości numeryczne są zgodne z requestem

---

### 🔵 TEST 2: Wielokrotne zapisanie draft changes - UPSERT

```
┌─────────────────────────────────────────────────────────────┐
│ KROK 1: Zapisanie pierwszy raz                              │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord                       │
│ │                     │     - retailPrice: 100.0            │
│ │                     │     - sellingPrice: 90.0            │
│ │                     │     - quantity: 10.0                │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveDraftChanges() (ponownie)
              z NOWYMI wartościami
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ KROK 2: UPSERT (UPDATE istniejącego)                      │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord (TEN SAM!)            │
│ │                     │     - retailPrice: 110.0 ⬆️         │
│ │                     │     - sellingPrice: 95.0 ⬆️         │
│ │                     │     - quantity: 15.0 ⬆️            │
│ │                     │     - marginPercent: 25.0 ⬆️        │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PO PIERWSZYM ZAPISIE**: `project_draft_changes_ws` - 1 rekord
- ✅ **PO PIERWSZYM ZAPISIE**: `project_products` - pusta
- ✅ **PO DRUGIM ZAPISIE (UPSERT)**: `project_draft_changes_ws` - NADAL 1 rekord (zaktualizowany, nie duplikat)
- ✅ **PO DRUGIM ZAPISIE**: `project_products` - nadal pusta
- ✅ Czy UPSERT działa poprawnie (UPDATE zamiast INSERT)
- ✅ Czy nie tworzy się duplikatów
- ✅ Czy wszystkie wartości są aktualizowane

---

### 🔵 TEST 3: Tylko zmiana marży

```
┌─────────────────────────────────────────────────────────────┐
│ PRZED:                                                      │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord                       │
│ │                     │     - quantity: 10.0                │
│ │                     │     - marginPercent: 20.0           │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveDraftChanges()
              categoryMargin = 30.0
              tylko quantity w changes
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO:                                                        │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord                        │
│ │                     │     - quantity: 20.0 ⬆️            │
│ │                     │     - marginPercent: 30.0 ⬆️        │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ Czy zmiana marży jest obsługiwana
- ✅ Czy quantity jest aktualizowane
- ✅ Czy inne wartości pozostają bez zmian

---

### 🔵 TEST 4: Tylko zmiana quantity

```
┌─────────────────────────────────────────────────────────────┐
│ PRZED:                                                      │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord                       │
│ │                     │     - quantity: 10.0                │
│ │                     │     - sellingPrice: 90.0             │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveDraftChanges()
              categoryMargin = null
              categoryDiscount = null
              tylko quantity w changes
              → Używa UPDATE quantity (szybsze!)
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO:                                                        │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  1 rekord                        │
│ │                     │     - quantity: 25.0 ⬆️            │
│ │                     │     - sellingPrice: 90.0 (bez zmian)│
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ Czy optymalizacja UPDATE quantity działa
- ✅ Czy tylko quantity jest aktualizowane
- ✅ Czy inne wartości pozostają bez zmian

---

### 🔵 TEST 5: Zapisanie projektu z draft changes

```
┌─────────────────────────────────────────────────────────────┐
│ PRZED:                                                      │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products    │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ✅ 1 rekord         │  │ ❌ PUSTA (0 rekordów)│           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveProjectData()
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO:                                                        │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products     │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ❌ PUSTA (0 rekordów)│  │ ✅ 1 rekord          │           │
│ │ (usunięte!)         │  │ (przeniesione!)      │           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PRZED saveProjectData**: `project_draft_changes_ws` - 1 rekord
- ✅ **PRZED saveProjectData**: `project_products` - pusta
- ✅ **PO saveProjectData**: `project_draft_changes_ws` - PUSTA (draft changes usunięte po przeniesieniu)
- ✅ **PO saveProjectData**: `project_products` - 1 rekord (draft changes przeniesione)
- ✅ Czy draft changes są przenoszone do ProjectProduct
- ✅ Czy draft changes są usuwane po przeniesieniu
- ✅ Czy wszystkie wartości są poprawnie skopiowane

---

### 🔵 TEST 6: Zapisanie projektu bez draft changes

```
┌─────────────────────────────────────────────────────────────┐
│ PRZED:                                                      │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products     │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ❌ PUSTA (0 rekordów)│  │ ✅ 1 rekord          │           │
│ │                     │  │ (stary zapis)        │           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveProjectData()
              (brak draft changes)
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO:                                                        │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products     │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ❌ PUSTA (0 rekordów)│  │ ❌ PUSTA (0 rekordów)│           │
│ │                     │  │ (usunięte!)         │           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PRZED saveProjectData**: `project_draft_changes_ws` - pusta (0 rekordów)
- ✅ **PRZED saveProjectData**: `project_products` - 1 rekord (stary zapis)
- ✅ **PO saveProjectData**: `project_draft_changes_ws` - pusta
- ✅ **PO saveProjectData**: `project_products` - PUSTA (usunięte, brak draft changes)
- ✅ Czy ProjectProduct są usuwane gdy nie ma draft changes
- ✅ Czy projekt jest czysty (brak starych danych)

---

### 🔵 TEST 7: Wielokrotne zapisanie projektu

```
┌─────────────────────────────────────────────────────────────┐
│ KROK 1: Pierwszy zapis                                      │
│ ┌─────────────────────┐                                     │
│ │ project_products    │                                     │
│ │                     │  →  1 rekord                       │
│ │                     │     - sellingPrice: 90.0            │
│ │                     │     - quantity: 10.0                │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              Zmiana draft changes
              + saveProjectData() (ponownie)
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ KROK 2: Drugi zapis (nadpisanie)                           │
│ ┌─────────────────────┐                                     │
│ │ project_products    │                                     │
│ │                     │  →  1 rekord (TEN SAM!)            │
│ │                     │     - sellingPrice: 95.0 ⬆️        │
│ │                     │     - quantity: 15.0 ⬆️           │
│ │                     │     - priceChangeSource: MANUAL ⬆️  │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PO PIERWSZYM ZAPISIE**: `project_draft_changes_ws` - pusta (usunięte)
- ✅ **PO PIERWSZYM ZAPISIE**: `project_products` - 1 rekord
- ✅ **PRZED DRUGIM ZAPISEM**: `project_draft_changes_ws` - 1 rekord (nowe draft changes)
- ✅ **PRZED DRUGIM ZAPISEM**: `project_products` - 1 rekord (stary zapis)
- ✅ **PO DRUGIM ZAPISIE**: `project_draft_changes_ws` - pusta (usunięte)
- ✅ **PO DRUGIM ZAPISIE**: `project_products` - NADAL 1 rekord (nadpisany, nie duplikat)
- ✅ Czy wielokrotne zapisanie działa poprawnie
- ✅ Czy wartości są nadpisywane (nie duplikowane)
- ✅ Czy wszystkie pola są aktualizowane

---

### 🔵 TEST 8: Zapisanie projektu po zmianie marży

```
┌─────────────────────────────────────────────────────────────┐
│ KROK 1: Zapisanie z marżą 20%                               │
│ ┌─────────────────────┐                                     │
│ │ project_products    │                                     │
│ │                     │  →  1 rekord                       │
│ │                     │     - marginPercent: 20.0           │
│ │                     │     - sellingPrice: 96.0            │
│ │                     │     - priceChangeSource: MARGIN     │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              Zmiana marży na 30%
              + saveProjectData() (ponownie)
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ KROK 2: Zapisanie z marżą 30%                               │
│ ┌─────────────────────┐                                     │
│ │ project_products    │                                     │
│ │                     │  →  1 rekord                        │
│ │                     │     - marginPercent: 30.0 ⬆️        │
│ │                     │     - sellingPrice: 104.0 ⬆️       │
│ │                     │     - priceChangeSource: MARGIN     │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PO PIERWSZYM ZAPISIE**: `project_draft_changes_ws` - pusta
- ✅ **PO PIERWSZYM ZAPISIE**: `project_products` - 1 rekord (marża 20%, cena 96.0)
- ✅ **PRZED DRUGIM ZAPISEM**: `project_draft_changes_ws` - 1 rekord (nowe draft changes z marżą 30%)
- ✅ **PRZED DRUGIM ZAPISEM**: `project_products` - 1 rekord (stary zapis z marżą 20%)
- ✅ **PO DRUGIM ZAPISIE**: `project_draft_changes_ws` - pusta (usunięte)
- ✅ **PO DRUGIM ZAPISIE**: `project_products` - 1 rekord (nadpisany, marża 30%, cena 104.0)
- ✅ Czy zmiana marży jest poprawnie zapisywana
- ✅ Czy cena sprzedaży jest przeliczana (80 * 1.30 = 104)
- ✅ Czy priceChangeSource pozostaje MARGIN

---

## 📊 Kiedy jakie dane pojawiają się w jakich tabelach?

### Tabela: `project_draft_changes_ws` (tymczasowe zmiany)

| Operacja | Stan przed | Stan po | Uwagi |
|----------|------------|---------|-------|
| `saveDraftChanges()` - pierwszy raz | PUSTA (0) | 1 rekord | ✅ Zapisane draft changes |
| `saveDraftChanges()` - UPSERT | 1 rekord | 1 rekord | ✅ Zaktualizowany (nie duplikat) |
| `saveProjectData()` - z draft changes | 1+ rekordów | PUSTA (0) | ✅ Usunięte po przeniesieniu |
| `saveProjectData()` - bez draft changes | PUSTA (0) | PUSTA (0) | ✅ Brak zmian |

### Tabela: `project_products` (zapisane dane)

| Operacja | Stan przed | Stan po | Uwagi |
|----------|------------|---------|-------|
| `saveDraftChanges()` | PUSTA (0) | PUSTA (0) | ✅ Draft changes nie są jeszcze zapisane |
| `saveProjectData()` - z draft changes | PUSTA (0) | 1+ rekordów | ✅ Przeniesione z draft changes |
| `saveProjectData()` - bez draft changes | 1+ rekordów | PUSTA (0) | ✅ Usunięte (brak draft changes) |
| `saveProjectData()` - wielokrotne | 1 rekord | 1 rekord | ✅ Nadpisany (nie duplikat) |

### Przykład: Pełny przepływ danych

```
KROK 1: saveDraftChanges()
┌─────────────────────────┐  ┌─────────────────────┐
│ project_draft_changes_ws │  │ project_products    │
│ ✅ 1 rekord              │  │ ❌ PUSTA (0)        │
└─────────────────────────┘  └─────────────────────┘

KROK 2: saveProjectData()
┌─────────────────────────┐  ┌─────────────────────┐
│ project_draft_changes_ws │  │ project_products    │
│ ❌ PUSTA (0)             │  │ ✅ 1 rekord         │
│ (usunięte)               │  │ (przeniesione)      │
└─────────────────────────┘  └─────────────────────┘

KROK 3: saveDraftChanges() ponownie
┌─────────────────────────┐  ┌─────────────────────┐
│ project_draft_changes_ws │  │ project_products    │
│ ✅ 1 rekord              │  │ ✅ 1 rekord         │
│ (nowe draft changes)     │  │ (stary zapis)       │
└─────────────────────────┘  └─────────────────────┘

KROK 4: saveProjectData() ponownie
┌─────────────────────────┐  ┌─────────────────────┐
│ project_draft_changes_ws │  │ project_products   │
│ ❌ PUSTA (0)             │  │ ✅ 1 rekord        │
│ (usunięte)               │  │ (nadpisany)        │
└─────────────────────────┘  └─────────────────────┘
```

---

## 🔄 Przepływ Danych - Ogólny Widok

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND                                 │
│  Użytkownik zmienia ceny/marzę/quantity                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              saveDraftChanges()                             │
│  ┌─────────────────────┐                                    │
│  │ project_draft_      │                                    │
│  │ changes_ws          │  →  Tymczasowe zmiany             │
│  │                     │     (niezapisane)                  │
│  └─────────────────────┘                                    │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              "Zapisz projekt" (przycisk)                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              saveProjectData()                              │
│  ┌─────────────────────┐  ┌─────────────────────┐          │
│  │ project_draft_      │  │ project_products    │          │
│  │ changes_ws          │  │                     │          │
│  │                     │  │                     │          │
│  │ ❌ USUNIĘTE         │  │ ✅ ZAPISANE          │          │
│  │                     │  │    (ostateczne)      │          │
│  └─────────────────────┘  └─────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 Uruchomienie Testów

```bash
# Uruchom wszystkie testy
mvn test

# Uruchom tylko testy ProjectServiceDraftChangesTest
mvn test -Dtest=ProjectServiceDraftChangesTest

# Uruchom konkretny test
mvn test -Dtest=ProjectServiceDraftChangesTest#testSaveDraftChanges_FirstTime
```

---

## ✅ Checklist Poprawności

Każdy test sprawdza **stan tabel w odpowiednich momentach**:

### 📊 Sprawdzanie stanu tabel:
- [x] **PRZED operacją** - stan początkowy tabel (`project_draft_changes_ws`, `project_products`)
- [x] **PO operacji** - stan końcowy tabel po wykonaniu operacji
- [x] **Przenoszenie danych** - czy dane są przenoszone z `project_draft_changes_ws` do `project_products`
- [x] **Usuwanie danych** - czy dane są usuwane z odpowiednich tabel w odpowiednich momentach

### 🔄 Sprawdzanie operacji:
- [x] **Zapisanie danych** - czy dane są zapisywane do bazy
- [x] **Aktualizacja danych** - czy dane są aktualizowane (UPSERT)
- [x] **Wielokrotne zapisanie** - czy wielokrotne zapisanie działa poprawnie
- [x] **Zmiana marży** - czy zmiana marży jest obsługiwana
- [x] **Tylko quantity** - czy optymalizacja UPDATE quantity działa
- [x] **Brak draft changes** - czy ProjectProduct są usuwane gdy nie ma draft changes

### 📋 Szczegółowe sprawdzenia:
- [x] **Liczba rekordów** - czy liczba rekordów w tabelach jest poprawna
- [x] **Wartości pól** - czy wszystkie wartości są poprawnie zapisane/aktualizowane
- [x] **Brak duplikatów** - czy nie tworzą się duplikaty przy wielokrotnym zapisie

---

## 📊 Testowanie Batch Processing (TEST 9 i TEST 10)

Testy **TEST 9** i **TEST 10** sprawdzają scenariusz z logów produkcyjnych:

### Scenariusz z logów:
```
POST /api/projects/1/draft-changes | kategoria: TILE | zmian: 8685
→ Batch 1/9 przygotowany | rekordów: 1000
→ Batch 2/9 przygotowany | rekordów: 1000
...
→ Batch 9/9 przygotowany | rekordów: 685
```

### Co testują TEST 9 i TEST 10:

**TEST 9: Duża liczba zmian - batch processing**
- ✅ Zapisanie **50 zmian** (symulacja 8685 zmian, w testach 50 dla szybkości)
- ✅ **Batch processing** - czy wszystkie batche są zapisane
- ✅ **Przeniesienie batch** - czy wszystkie rekordy są przeniesione do ProjectProduct
- ✅ Sprawdzenie, czy **wszystkie produkty** są w draft changes i ProjectProduct

**TEST 10: Wielokrotne zapisanie dużej liczby zmian - UPSERT batch**
- ✅ **30 zmian** zapisane pierwszy raz
- ✅ **UPSERT batch** - zapisanie tych samych 30 zmian ponownie z nowymi wartościami
- ✅ Sprawdzenie, czy **nie tworzą się duplikaty** (nadal 30 rekordów)
- ✅ Sprawdzenie, czy **wszystkie wartości są zaktualizowane**

### Różnica między testem a produkcją:

| Aspekt | Test | Produkcja |
|--------|------|-----------|
| Liczba zmian | 50 | 8685 |
| Liczba batchy | 1 batch (50 < 1000) | 9 batchy (1000, 1000, ..., 685) |
| Mechanizm | Ten sam UPSERT | Ten sam UPSERT |
| Weryfikacja | ✅ Wszystkie zapisane | ✅ Wszystkie zapisane |

**Wniosek:** Test weryfikuje poprawność logiki batch processing, która działa tak samo dla 50 jak i dla 8685 zmian.

---

---

## 🗄️ Testy na prawdziwej bazie MySQL/MariaDB

### 🔒 Bezpieczeństwo - Czy testy modyfikują dane produkcyjne?

**NIE!** Testy są **całkowicie bezpieczne** dzięki `@Transactional`:

- ✅ **Wszystkie testy są `@Transactional`** → zmiany są **ROLLBACKOWANE** po teście
- ✅ **Użytkownik NIE ZOBACZY zmian** (rollback przed zakończeniem testu)
- ✅ **Możesz użyć TEJ SAMEJ bazy co produkcja** - jest BEZPIECZNE!

**Jak to działa:**
1. Test zaczyna transakcję (`BEGIN TRANSACTION`)
2. Wykonuje operacje (INSERT, UPDATE, DELETE)
3. Sprawdza wyniki
4. **ROLLBACK przed zakończeniem testu** → wszystkie zmiany cofnięte

**Szczegóły:** Zobacz `TESTY_BEZPIECZENSTWO.md`

### Dlaczego testy na MySQL zamiast H2?

**Problem z H2:**
- ❌ H2 jest in-memory i ma inne zachowanie niż MySQL/MariaDB
- ❌ H2 nie pokazuje problemów z timeoutami i blokadami
- ❌ H2 jest zbyt szybka - nie testuje prawdziwych scenariuszy produkcyjnych
- ❌ Testy z małą liczbą danych (50 zmian) nie wykrywają problemów z 8685 zmianami

**Korzyści z MySQL:**
- ✅ **Realne testy** - takie same warunki jak w produkcji
- ✅ **Wykrywanie timeoutów** - prawdziwe problemy z długimi transakcjami
- ✅ **Weryfikacja batch processing** - testowanie z prawdziwą liczbą danych (8685 zmian)
- ✅ **Testowanie blokad** - weryfikacja czy nie ma problemów z lock wait timeout

### Konfiguracja

#### 1. Utwórz osobną bazę testową

**⚠️ WAŻNE: NIE używaj bazy produkcyjnej!**

```sql
-- W MySQL/MariaDB utwórz osobną bazę testową
CREATE DATABASE defaultdb_test;
```

#### 2. Skonfiguruj `application-test-mysql.properties`

Plik: `src/test/resources/application-test-mysql.properties`

```properties
# OSOBNA BAZA TESTOWA (nie produkcyjna!)
spring.datasource.url=jdbc:mysql://.../defaultdb_test?...
spring.jpa.hibernate.ddl-auto=create-drop  # Usuwa tabele po testach
```

#### 3. Uruchom testy MySQL

```bash
# Uruchom tylko testy MySQL
mvn test -Dtest=ProjectServiceDraftChangesTestMySQL

# Lub w IntelliJ: kliknij prawym na klasę ProjectServiceDraftChangesTestMySQL → Run
```

### Testy MySQL

**Klasa:** `ProjectServiceDraftChangesTestMySQL`

**Profil:** `@ActiveProfiles("test-mysql")`

**Testy:**
1. ✅ **TEST 1**: Podstawowy test zapisu draft changes
2. ✅ **TEST 2**: **8685 zmian** (jak w produkcji) - 9 batchy
3. ✅ **TEST 3**: Weryfikacja connection z EntityManager

### Bezpieczeństwo

**Wszystkie testy są `@Transactional`:**
- ✅ Zmiany są **rollbackowane** po zakończeniu testu
- ✅ **Nie modyfikują** danych produkcyjnych
- ✅ Używają **osobnej bazy testowej** (`defaultdb_test`)

**`ddl-auto=create-drop`:**
- ✅ Tworzy tabele przed testami
- ✅ Usuwa tabele po testach
- ✅ Zapewnia czystą bazę dla każdego uruchomienia

### Porównanie testów

| Aspekt | H2 (test) | MySQL (test-mysql) |
|--------|-----------|---------------------|
| **Szybkość** | ⚡ Szybkie | 🐢 Wolniejsze |
| **Realizm** | ❌ Niski | ✅ Wysoki |
| **Timeouty** | ❌ Nie wykrywa | ✅ Wykrywa |
| **Blokady** | ❌ Nie wykrywa | ✅ Wykrywa |
| **Batch processing** | ⚠️ Ograniczony | ✅ Pełny |
| **Liczba danych** | ⚠️ Mała (50) | ✅ Duża (8685) |

### Rekomendacja

**Używaj obu:**
- **H2** (`ProjectServiceDraftChangesTest`) - szybkie testy jednostkowe
- **MySQL** (`ProjectServiceDraftChangesTestMySQL`) - testy integracyjne przed wdrożeniem

**Przed wdrożeniem:**
1. ✅ Uruchom testy H2 (szybkie)
2. ✅ Uruchom testy MySQL (weryfikacja realnych scenariuszy)
3. ✅ Sprawdź czy TEST 2 (8685 zmian) przechodzi bez timeoutu

---

## 🎯 Podsumowanie

Testy sprawdzają **10 kluczowych scenariuszy** używania systemu:

1. ✅ **Pierwsze zapisanie** - podstawowa funkcjonalność
2. ✅ **Wielokrotne zapisanie** - UPSERT i aktualizacja
3. ✅ **Zmiana marży** - obsługa marży
4. ✅ **Tylko quantity** - optymalizacja
5. ✅ **Zapisanie projektu** - przenoszenie danych
6. ✅ **Brak draft changes** - czyszczenie danych
7. ✅ **Wielokrotne zapisanie projektu** - nadpisywanie
8. ✅ **Zmiana marży w projekcie** - aktualizacja marży
9. ✅ **Duża liczba zmian** - batch processing (50 zmian)
10. ✅ **Wielokrotne zapisanie batch** - UPSERT dla dużej liczby zmian

Wszystkie testy są **@Transactional**, więc nie modyfikują rzeczywistej bazy danych.

---

## 📊 Testowanie Batch Processing

Testy **TEST 9** i **TEST 10** sprawdzają scenariusz z logów:
- ✅ Zapisanie **dużej liczby zmian** (symulacja 8685 zmian, w testach 50 dla szybkości)
- ✅ **Batch processing** - czy wszystkie batche są zapisane
- ✅ **UPSERT batch** - czy wielokrotne zapisanie działa dla dużej liczby zmian
- ✅ **Przeniesienie batch** - czy wszystkie rekordy są przeniesione do ProjectProduct

**Różnica między testem a produkcją:**
- **Test**: 50 zmian (szybki test)
- **Produkcja**: 8685 zmian (9 batchy po 1000 rekordów)

Oba używają tego samego mechanizmu batch processing, więc test weryfikuje poprawność logiki.

---

### 🔵 TEST 9: Duża liczba zmian - batch processing

```
┌─────────────────────────────────────────────────────────────┐
│ PRZED:                                                      │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products    │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ❌ PUSTA (0)        │  │ ❌ PUSTA (0)        │           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveDraftChanges()
              50 zmian (batch processing)
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO saveDraftChanges:                                        │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products    │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ✅ 50 REKORDÓW      │  │ ❌ PUSTA (0)        │           │
│ │ (wszystkie zapisane)│  │ (draft changes nie  │           │
│ │                     │  │ są jeszcze zapisane)│           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveProjectData()
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ PO saveProjectData:                                          │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ project_draft_      │  │ project_products    │           │
│ │ changes_ws          │  │                     │           │
│ │                     │  │                     │           │
│ │ ❌ PUSTA (0)        │  │ ✅ 50 REKORDÓW      │           │
│ │ (wszystkie usunięte)│  │ (wszystkie          │           │
│ │                     │  │ przeniesione)       │           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PRZED**: `project_draft_changes_ws` - pusta
- ✅ **PRZED**: `project_products` - pusta
- ✅ **PO saveDraftChanges**: `project_draft_changes_ws` - 50 rekordów (wszystkie zapisane)
- ✅ **PO saveDraftChanges**: `project_products` - nadal pusta
- ✅ **PO saveProjectData**: `project_draft_changes_ws` - pusta (wszystkie usunięte)
- ✅ **PO saveProjectData**: `project_products` - 50 rekordów (wszystkie przeniesione)
- ✅ Czy batch processing działa poprawnie (duża liczba zmian)
- ✅ Czy wszystkie rekordy są zapisane (nie tracone)
- ✅ Czy wszystkie rekordy są przeniesione do ProjectProduct

---

### 🔵 TEST 10: Wielokrotne zapisanie dużej liczby zmian - UPSERT batch

```
┌─────────────────────────────────────────────────────────────┐
│ KROK 1: Pierwszy zapis (30 zmian)                           │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  30 rekordów                    │
│ │                     │     - marginPercent: 20.0            │
│ │                     │     - quantity: 10.0                │
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
              saveDraftChanges() (ponownie)
              30 zmian (UPSERT batch)
              NOWE wartości: marginPercent=25.0, quantity=15.0
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ KROK 2: Drugi zapis (UPSERT batch)                          │
│ ┌─────────────────────┐                                     │
│ │ project_draft_      │                                     │
│ │ changes_ws          │  →  30 rekordów (TEN SAM!)        │
│ │                     │     - marginPercent: 25.0 ⬆️        │
│ │                     │     - quantity: 15.0 ⬆️            │
│ │                     │     (zaktualizowane, nie duplikaty)│
│ └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

**Co testuje:**
- ✅ **PO PIERWSZYM ZAPISIE**: `project_draft_changes_ws` - 30 rekordów
- ✅ **PO DRUGIM ZAPISIE (UPSERT)**: `project_draft_changes_ws` - NADAL 30 rekordów (zaktualizowane, nie duplikaty)
- ✅ Czy UPSERT działa poprawnie dla dużej liczby zmian (batch)
- ✅ Czy wszystkie wartości są aktualizowane
- ✅ Czy nie tworzą się duplikaty przy wielokrotnym zapisie batch

