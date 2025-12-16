package pl.koszela.nowoczesnebud.Service;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.koszela.nowoczesnebud.Model.Address;
import pl.koszela.nowoczesnebud.Model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🎯 TESTY TWORZENIA KLIENTA - Brzegowe warianty
 * 
 * Testuje różne scenariusze tworzenia klienta:
 * - Podstawowy scenariusz
 * - Bardzo długie imię i nazwisko
 * - Specjalne znaki w adresie
 * - Różne formaty adresów
 */
@DisplayName("Testy tworzenia klienta - brzegowe warianty")
class ProjectServiceClientCreationTest extends BaseProjectServiceTest {

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Test
    @DisplayName("TEST 1: Tworzenie klienta - podstawowy scenariusz")
    void testCreateClient_Basic() {
        // GIVEN: Nowy użytkownik
        User user = new User();
        user.setName("Jan");
        user.setSurname("Kowalski");
        
        Address address = new Address();
        address.setAddress("ul. Testowa 1, Warszawa");
        address.setLatitude(52.2297);
        address.setLongitude(21.0122);
        address.setZoom(15.0);
        user.setAddress(address);
        
        // WHEN: Zapisujemy użytkownika
        User savedUser = userRepository.save(user);
        
        // THEN: Użytkownik został zapisany poprawnie
        assertNotNull(savedUser.getId(), "✅ Użytkownik powinien mieć ID");
        assertEquals("Jan", savedUser.getName(), "✅ Imię powinno być zapisane");
        assertEquals("Kowalski", savedUser.getSurname(), "✅ Nazwisko powinno być zapisane");
        assertNotNull(savedUser.getAddress(), "✅ Adres powinien być zapisany");
        assertEquals(52.2297, savedUser.getAddress().getLatitude(), "✅ Szerokość geograficzna powinna być zapisana");
    }

    @Test
    @DisplayName("TEST 2: Tworzenie klienta - brzegowy przypadek: bardzo długie imię i nazwisko")
    void testCreateClient_LongName() {
        // GIVEN: Użytkownik z bardzo długim imieniem i nazwiskiem
        User user = new User();
        user.setName("Jan-Maria-Kazimierz-Władysław");
        user.setSurname("Kowalski-Nowak-Wiśniewski-Zieliński");
        
        Address address = new Address();
        address.setAddress("ul. Testowa 1, Warszawa");
        address.setLatitude(52.2297);
        address.setLongitude(21.0122);
        address.setZoom(15.0);
        user.setAddress(address);
        
        // WHEN: Zapisujemy użytkownika
        User savedUser = userRepository.save(user);
        
        // THEN: Użytkownik został zapisany poprawnie
        assertNotNull(savedUser.getId(), "✅ Użytkownik powinien mieć ID");
        assertTrue(savedUser.getName().length() > 20, "✅ Długie imię powinno być zapisane");
        assertTrue(savedUser.getSurname().length() > 30, "✅ Długie nazwisko powinno być zapisane");
    }

    @Test
    @DisplayName("TEST 3: Tworzenie klienta - brzegowy przypadek: specjalne znaki w adresie")
    void testCreateClient_SpecialCharacters() {
        // GIVEN: Użytkownik z adresem zawierającym specjalne znaki
        User user = new User();
        user.setName("Jan");
        user.setSurname("Kowalski");
        
        Address address = new Address();
        address.setAddress("ul. Żółwińska 123/45, 00-001 Warszawa, Polska");
        address.setLatitude(52.2297);
        address.setLongitude(21.0122);
        address.setZoom(15.0);
        user.setAddress(address);
        
        // WHEN: Zapisujemy użytkownika
        User savedUser = userRepository.save(user);
        
        // THEN: Użytkownik został zapisany poprawnie
        assertNotNull(savedUser.getId(), "✅ Użytkownik powinien mieć ID");
        assertTrue(savedUser.getAddress().getAddress().contains("Żółwińska"), 
                  "✅ Adres ze specjalnymi znakami powinien być zapisany");
    }

    @Test
    @DisplayName("TEST 4: Tworzenie klienta - brzegowy przypadek: minimalne wartości współrzędnych")
    void testCreateClient_MinimalCoordinates() {
        // GIVEN: Użytkownik z minimalnymi wartościami współrzędnych
        User user = new User();
        user.setName("Jan");
        user.setSurname("Kowalski");
        
        Address address = new Address();
        address.setAddress("ul. Testowa 1");
        address.setLatitude(-90.0);  // Minimalna szerokość geograficzna
        address.setLongitude(-180.0);  // Minimalna długość geograficzna
        address.setZoom(1.0);  // Minimalny zoom
        user.setAddress(address);
        
        // WHEN: Zapisujemy użytkownika
        User savedUser = userRepository.save(user);
        
        // THEN: Użytkownik został zapisany poprawnie
        assertNotNull(savedUser.getId(), "✅ Użytkownik powinien mieć ID");
        assertEquals(-90.0, savedUser.getAddress().getLatitude(), "✅ Minimalna szerokość geograficzna powinna być zapisana");
        assertEquals(-180.0, savedUser.getAddress().getLongitude(), "✅ Minimalna długość geograficzna powinna być zapisana");
    }

    @Test
    @DisplayName("TEST 5: Tworzenie klienta - brzegowy przypadek: maksymalne wartości współrzędnych")
    void testCreateClient_MaximalCoordinates() {
        // GIVEN: Użytkownik z maksymalnymi wartościami współrzędnych
        User user = new User();
        user.setName("Jan");
        user.setSurname("Kowalski");
        
        Address address = new Address();
        address.setAddress("ul. Testowa 1");
        address.setLatitude(90.0);  // Maksymalna szerokość geograficzna
        address.setLongitude(180.0);  // Maksymalna długość geograficzna
        address.setZoom(20.0);  // Maksymalny zoom
        user.setAddress(address);
        
        // WHEN: Zapisujemy użytkownika
        User savedUser = userRepository.save(user);
        
        // THEN: Użytkownik został zapisany poprawnie
        assertNotNull(savedUser.getId(), "✅ Użytkownik powinien mieć ID");
        assertEquals(90.0, savedUser.getAddress().getLatitude(), "✅ Maksymalna szerokość geograficzna powinna być zapisana");
        assertEquals(180.0, savedUser.getAddress().getLongitude(), "✅ Maksymalna długość geograficzna powinna być zapisana");
    }

    @Test
    @DisplayName("TEST 6: Tworzenie klientów - wydajność dla 1000 klientów (batch insert)")
    void testCreateClients_Performance_1000Clients() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 1000 klientów używając batch insert (jak w createProductsBatch)
        logger.info("🔄 TEST 6: Tworzenie 1000 klientów testowych (JDBC batch insert)...");
        long createStart = System.currentTimeMillis();
        
        int count = 1000;
        String sql = "INSERT INTO \"user\" " +
                    "(name, surname, address, latitude, longitude, zoom, " +
                    "telephone_number, date_of_meeting, email, create_date_time, update_date_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 500;
        int totalBatches = (int)Math.ceil((double)count / batchSize);
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, count);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchStart = System.currentTimeMillis();
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, "Jan" + i);
                            pstmt.setString(paramIndex++, "Kowalski" + i);
                            pstmt.setString(paramIndex++, "ul. Testowa " + i + ", Warszawa");
                            pstmt.setDouble(paramIndex++, 52.2297 + (i * 0.001));
                            pstmt.setDouble(paramIndex++, 21.0122 + (i * 0.001));
                            pstmt.setDouble(paramIndex++, 15.0);
                            pstmt.setString(paramIndex++, null); // telephone_number
                            pstmt.setDate(paramIndex++, null); // date_of_meeting
                            pstmt.setString(paramIndex++, null); // email
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        pstmt.executeBatch();
                        
                        long batchDuration = System.currentTimeMillis() - batchStart;
                        logger.info("✅ Utworzono {} klientów (batch {}/{}) w {}ms",
                                  recordsInBatch, batchIndex + 1, totalBatches, batchDuration);
                    }
                } catch (SQLException e) {
                    logger.error("❌ Błąd podczas batch insert klientów: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch insert klientów", e);
                }
            }
        });
        
        entityManager.flush();
        
        long createEnd = System.currentTimeMillis();
        long createDuration = createEnd - createStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Utworzenie 1000 klientów (batch insert): {}ms ({}s)", 
                   createDuration, createDuration / 1000.0);
        
        // THEN: Wszyscy klienci powinni być zapisani
        long verifyStart = System.currentTimeMillis();
        long dbCount = userRepository.count();
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Weryfikacja (count): {}ms", verifyDuration);
        
        assertTrue(dbCount >= 1000, "✅ Powinno być zapisanych co najmniej 1000 klientów. Znaleziono: " + dbCount);
        assertTrue(createDuration < 10000, 
                  "✅ Operacja powinna zakończyć się w < 10s. Czas: " + createDuration + "ms");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - CAŁKOWITY CZAS: {}ms ({}s) | create: {}ms | verify: {}ms", 
                   testDuration, testDuration / 1000.0, createDuration, verifyDuration);
    }
}

