package pl.koszela.nowoczesnebud.Service;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.DTO.DraftChangeDTO;
import pl.koszela.nowoczesnebud.DTO.SaveDraftChangesRequest;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.*;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 🎯 BAZOWA KLASA TESTOWA - Wspólne metody pomocnicze dla wszystkich testów
 * 
 * Zawiera:
 * - Wspólne pola (@Autowired)
 * - Metody pomocnicze (createProductsBatch, setUp, etc.)
 * - Wspólne konfiguracje
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public abstract class BaseProjectServiceTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(BaseProjectServiceTest.class);

    @Autowired
    protected ProjectService projectService;

    @Autowired
    protected ProjectRepository projectRepository;

    @Autowired
    protected ProjectDraftChangeRepository projectDraftChangeRepository;

    @Autowired
    protected ProjectProductRepository projectProductRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected EntityManager entityManager;

    protected Project testProject;
    protected User testUser;
    protected Product testProduct;

    /**
     * Metoda setUp() - do nadpisania w klasach dziedziczących
     */
    protected void setUpBase() {
        // Utwórz testowego użytkownika (klienta)
        testUser = new User();
        testUser.setName("Test");
        testUser.setSurname("User");
        
        Address address = new Address();
        address.setAddress("Test Address");
        address.setLatitude(52.0);
        address.setLongitude(21.0);
        address.setZoom(10.0);
        testUser.setAddress(address);
        
        testUser = userRepository.save(testUser);

        // Utwórz testowy projekt
        testProject = new Project();
        testProject.setClient(testUser);
        testProject = projectRepository.save(testProject);

        // Utwórz testowy produkt (dla podstawowych testów)
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setCategory(ProductCategory.TILE);
        testProduct.setRetailPrice(100.0);
        testProduct.setPurchasePrice(80.0);
        testProduct.setManufacturer("Test Manufacturer");
        testProduct.setGroupName("Test Group");
        testProduct = productRepository.save(testProduct);
    }

    /**
     * Pomocnicza metoda do tworzenia requestu draft changes
     */
    protected SaveDraftChangesRequest createDraftChangesRequest(
            Long productId, String category, 
            Double retailPrice, Double purchasePrice, Double sellingPrice,
            Double quantity, Double marginPercent, String priceChangeSource) {
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(category);
        
        DraftChangeDTO change = new DraftChangeDTO(productId, category);
        change.setDraftRetailPrice(retailPrice);
        change.setDraftPurchasePrice(purchasePrice);
        change.setDraftSellingPrice(sellingPrice);
        change.setDraftQuantity(quantity);
        change.setDraftMarginPercent(marginPercent);
        change.setPriceChangeSource(priceChangeSource);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        changes.add(change);
        request.setChanges(changes);
        
        return request;
    }

    /**
     * Pomocnicza metoda do tworzenia requestu dla dużej liczby zmian
     */
    protected SaveDraftChangesRequest createLargeBatchRequest(List<Product> products, double marginPercent, double quantity) {
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftRetailPrice(product.getRetailPrice());
            change.setDraftPurchasePrice(product.getPurchasePrice());
            change.setDraftSellingPrice(product.getPurchasePrice() * (1 + marginPercent / 100));
            change.setDraftQuantity(quantity);
            change.setDraftMarginPercent(marginPercent);
            change.setPriceChangeSource(PriceChangeSource.MARGIN.name());
            changes.add(change);
        }
        request.setChanges(changes);
        return request;
    }

    /**
     * ⚡ REALISTYCZNY BATCH INSERT: Tworzy produkty testowe używając JDBC batch insert
     * (tak samo jak w ProjectService.upsertDraftChanges - najszybsze podejście)
     * 
     * @param count Liczba produktów do utworzenia
     * @return Lista utworzonych produktów (z ID z bazy)
     */
    protected List<Product> createProductsBatch(int count) {
        long startTime = System.currentTimeMillis();
        logger.info("🔄 Tworzenie {} produktów testowych (JDBC batch insert)...", count);
        
        String sql = "INSERT INTO products " +
                    "(name, manufacturer, category, group_name, retail_price, purchase_price, " +
                    "selling_price, unit, quantity_converter, quantity, mapper_name, discount, " +
                    "discount_calculation_method, basic_discount, promotion_discount, " +
                    "additional_discount, skonto_discount, margin_percent, accessory_type, " +
                    "product_type, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 1000;
        int totalBatches = (int)Math.ceil((double)count / batchSize);
        
        final Timestamp beforeInsert = Timestamp.valueOf(LocalDateTime.now());
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, count);
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, "Test Product " + i);
                            pstmt.setString(paramIndex++, "Test Manufacturer");
                            pstmt.setString(paramIndex++, ProductCategory.TILE.name());
                            pstmt.setString(paramIndex++, "Test Group");
                            pstmt.setDouble(paramIndex++, 100.0 + i);
                            pstmt.setDouble(paramIndex++, 80.0 + i);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setDouble(paramIndex++, 1.0);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setString(paramIndex++, null);
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        pstmt.executeBatch();
                    }
                } catch (SQLException e) {
                    logger.error("❌ Błąd podczas batch insert produktów: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch insert produktów", e);
                }
            }
        });
        
        entityManager.flush();
        
        String selectSql = "SELECT id FROM products WHERE name LIKE 'Test Product%' AND created_at >= ? ORDER BY id LIMIT ?";
        List<Long> productIds = new ArrayList<>();
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                    selectStmt.setTimestamp(1, beforeInsert);
                    selectStmt.setInt(2, count);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        while (rs.next()) {
                            productIds.add(rs.getLong(1));
                        }
                    }
                }
            }
        });
        
        // ⚡ OPTYMALIZACJA: Użyj findAllById() zamiast pojedynczych findById() - znacznie szybsze!
        List<Product> products = new ArrayList<>();
        if (!productIds.isEmpty()) {
            // Pobierz wszystkie produkty jednym zapytaniem (batch select)
            products = productRepository.findAllById(productIds);
            // Upewnij się, że produkty są w tej samej kolejności co productIds
            products.sort((p1, p2) -> Long.compare(
                productIds.indexOf(p1.getId()),
                productIds.indexOf(p2.getId())
            ));
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("✅ Utworzono wszystkie {} produktów w {}ms ({}s)", count, duration, duration / 1000.0);
        
        return products;
    }

    /**
     * ⚡ REALISTYCZNY BATCH INSERT: Tworzy produkty testowe używając JDBC batch insert z określoną kategorią
     * 
     * @param count Liczba produktów do utworzenia
     * @param category Kategoria produktów
     * @return Lista utworzonych produktów (z ID z bazy)
     */
    protected List<Product> createProductsBatch(int count, ProductCategory category) {
        long startTime = System.currentTimeMillis();
        logger.info("🔄 Tworzenie {} produktów testowych kategorii {} (JDBC batch insert)...", count, category);
        
        String sql = "INSERT INTO products " +
                    "(name, manufacturer, category, group_name, retail_price, purchase_price, " +
                    "selling_price, unit, quantity_converter, quantity, mapper_name, discount, " +
                    "discount_calculation_method, basic_discount, promotion_discount, " +
                    "additional_discount, skonto_discount, margin_percent, accessory_type, " +
                    "product_type, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 1000;
        int totalBatches = (int)Math.ceil((double)count / batchSize);
        
        final Timestamp beforeInsert = Timestamp.valueOf(LocalDateTime.now());
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, count);
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, "Test Product " + i);
                            pstmt.setString(paramIndex++, "Test Manufacturer");
                            pstmt.setString(paramIndex++, category.name());
                            pstmt.setString(paramIndex++, "Test Group");
                            pstmt.setDouble(paramIndex++, 100.0 + i);
                            pstmt.setDouble(paramIndex++, 80.0 + i);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, category == ProductCategory.ACCESSORY ? "szt" : null);
                            pstmt.setDouble(paramIndex++, category == ProductCategory.ACCESSORY ? null : 1.0);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, category == ProductCategory.ACCESSORY ? "TYPE1" : null);
                            pstmt.setString(paramIndex++, category == ProductCategory.ACCESSORY ? null : "TYPE1");
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        pstmt.executeBatch();
                    }
                } catch (SQLException e) {
                    logger.error("❌ Błąd podczas batch insert produktów: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch insert produktów", e);
                }
            }
        });
        
        entityManager.flush();
        
        String selectSql = "SELECT id FROM products WHERE name LIKE 'Test Product%' AND category = ? AND created_at >= ? ORDER BY id LIMIT ?";
        List<Long> productIds = new ArrayList<>();
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                    selectStmt.setString(1, category.name());
                    selectStmt.setTimestamp(2, beforeInsert);
                    selectStmt.setInt(3, count);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        while (rs.next()) {
                            productIds.add(rs.getLong(1));
                        }
                    }
                }
            }
        });
        
        // ⚡ OPTYMALIZACJA: Użyj findAllById() zamiast pojedynczych findById() - znacznie szybsze!
        List<Product> products = new ArrayList<>();
        if (!productIds.isEmpty()) {
            // Pobierz wszystkie produkty jednym zapytaniem (batch select)
            products = productRepository.findAllById(productIds);
            // Upewnij się, że produkty są w tej samej kolejności co productIds
            products.sort((p1, p2) -> Long.compare(
                productIds.indexOf(p1.getId()),
                productIds.indexOf(p2.getId())
            ));
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("✅ Utworzono wszystkie {} produktów kategorii {} w {}ms ({}s)", 
                   count, category, duration, duration / 1000.0);
        
        return products;
    }
}

