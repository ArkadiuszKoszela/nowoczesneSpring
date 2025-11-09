# 🚀 Instrukcja wdrożenia i uruchamiania

## 📍 Uruchamianie lokalnie (localhost)

### Backend (Spring Boot):
```bash
cd nowoczesne-bud

# Opcja 1: Maven
mvn spring-boot:run

# Opcja 2: Maven z явnym profilem dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Opcja 3: Zbuduj JAR i uruchom
mvn clean package
java -jar target/nowoczesne-bud-0.0.1-SNAPSHOT.jar
```

Backend będzie działał na: **http://localhost:8081**

### Frontend (Angular):
```bash
cd Angular

# Instalacja zależności (tylko raz)
npm install

# Uruchomienie z proxy (przekierowanie /api/ na localhost:8081)
npm start

# Lub jawnie:
ng serve --proxy-config proxy.config.json
```

Frontend będzie działał na: **http://localhost:4200**

---

## ☁️ Wdrożenie na Heroku

### Backend:

1. **Zaloguj się do Heroku:**
```bash
heroku login
```

2. **Wejdź do folderu backendu:**
```bash
cd nowoczesne-bud
```

3. **Inicjalizuj Git (jeśli jeszcze nie):**
```bash
git init
heroku git:remote -a nowoczesne-66bde1a28817
```

4. **Ustaw zmienną środowiskową na Heroku:**
```bash
heroku config:set SPRING_PROFILES_ACTIVE=prod
```

5. **Deploy:**
```bash
git add .
git commit -m "Deploy backend"
git push heroku master
```

6. **Sprawdź logi:**
```bash
heroku logs --tail
```

Backend na Heroku: **https://nowoczesne-66bde1a28817.herokuapp.com**

---

### Frontend:

1. **Wejdź do folderu Angulara:**
```bash
cd Angular
```

2. **Inicjalizuj Git (jeśli jeszcze nie):**
```bash
git init
heroku git:remote -a angular-nowoczesne-af04d5c56981
```

3. **Deploy:**
```bash
git add .
git commit -m "Deploy frontend"
git push heroku master
```

4. **Sprawdź logi:**
```bash
heroku logs --tail
```

Frontend na Heroku: **https://angular-nowoczesne-af04d5c56981.herokuapp.com**

---

## 🔧 Konfiguracja profili

### Backend używa 2 profile:

1. **dev** (localhost) - `application-dev.properties`
   - Port: 8081
   - Show SQL: true
   - Automatycznie aktywny lokalnie

2. **prod** (Heroku) - `application-prod.properties`
   - Port: z zmiennej $PORT (Heroku)
   - Show SQL: false
   - Aktywny przez `Procfile` na Heroku

### Frontend używa 2 environmenty:

1. **development** - `environment.ts`
   - Adres: `/api/` (przekierowywane przez proxy na localhost:8081)
   - Używane przez: `ng serve`

2. **production** - `environment.prod.ts`
   - Adres: `https://nowoczesne-66bde1a28817.herokuapp.com/api/`
   - Używane przez: `ng build --configuration production`

---

## ✅ Sprawdzenie działania

### Localhost:
1. Uruchom backend → otwórz http://localhost:8081/api/products/getAll
2. Uruchom frontend → otwórz http://localhost:4200
3. Frontend powinien komunikować się z backendem przez proxy

### Heroku:
1. Frontend → https://angular-nowoczesne-af04d5c56981.herokuapp.com
2. Backend → https://nowoczesne-66bde1a28817.herokuapp.com/api/products/getAll
3. Frontend komunikuje się bezpośrednio z backendem

---

## 🔍 Rozwiązywanie problemów

### Backend nie startuje lokalnie:
```bash
# Sprawdź czy port 8081 jest wolny
netstat -ano | findstr :8081

# Sprawdź aktywny profil
mvn spring-boot:run -Dspring-boot.run.arguments=--logging.level.org.springframework=DEBUG
```

### Frontend nie łączy się z backendem lokalnie:
1. Sprawdź czy backend działa na porcie 8081
2. Sprawdź `proxy.config.json` - powinno być `http://localhost:8081`
3. Uruchom frontend z flagą: `npm start` (używa proxy)

### Błędy CORS:
- Backend ma skonfigurowane CORS dla:
  - `http://localhost:4200` (development)
  - `https://angular-nowoczesne-af04d5c56981.herokuapp.com` (production)
- Jeśli używasz innego portu/URL, dodaj go w `WebConfig.java`

---

## 📦 Pliki konfiguracyjne

### Backend:
- `Procfile` - instrukcja dla Heroku jak uruchomić aplikację
- `system.properties` - wersja Java dla Heroku
- `application.properties` - wybór profilu (dev/prod)
- `application-dev.properties` - konfiguracja localhost
- `application-prod.properties` - konfiguracja Heroku

### Frontend:
- `server.js` - Express server dla Heroku
- `proxy.config.json` - przekierowanie /api/ na backend lokalnie
- `environment.ts` - konfiguracja development
- `environment.prod.ts` - konfiguracja production
- `package.json` - skrypt `heroku-postbuild` dla automatycznego buildu

---

## 🎯 Najlepsze praktyki

1. **Przed deploymentem na Heroku:**
   - Przetestuj lokalnie
   - Zbuduj produkcyjną wersję lokalnie: `ng build --configuration production`
   - Sprawdź czy nie ma błędów: `mvn clean package`

2. **Po każdej zmianie:**
   - Backend: `mvn clean package` + deploy
   - Frontend: `npm run build` sprawdź błędy, potem deploy

3. **Monitorowanie:**
   - `heroku logs --tail -a nowoczesne-66bde1a28817` (backend)
   - `heroku logs --tail -a angular-nowoczesne-af04d5c56981` (frontend)

