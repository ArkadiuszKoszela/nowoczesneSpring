package pl.koszela.nowoczesnebud.CreateOffer;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Profesjonalny generator PDF oferty handlowej
 * Elegancki design z czytelnym layoutem i profesjonalnym formatowaniem
 */
@Service
public class CreateOffer {

    private final ProductRepository productRepository;
    
    // Kolory firmowe
    private static final BaseColor PRIMARY_COLOR = new BaseColor(102, 126, 234); // Fioletowy
    private static final BaseColor SECONDARY_COLOR = new BaseColor(71, 85, 179); // Ciemniejszy fiolet
    private static final BaseColor ACCENT_COLOR = new BaseColor(241, 245, 249); // Jasny szary
    private static final BaseColor TEXT_DARK = new BaseColor(30, 41, 59); // Ciemny tekst
    private static final BaseColor TEXT_LIGHT = new BaseColor(148, 163, 184); // Jasny tekst

    public CreateOffer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Generuje elegancką ofertę PDF dla projektu
     */
    public void createOffer(Project project) {
        Document document = new Document(PageSize.A4, 50, 50, 80, 60);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(new File(StaticValuesForOffer.URL_TO_PDF)));
            document.open();

            // ========== NAGŁÓWEK ==========
            addElegantHeader(document, project);
            
            // ========== DANE KLIENTA ==========
            addElegantClientInfo(document, project);
            document.add(Chunk.NEWLINE);

            // ========== Pobierz produkty ==========
            List<Product> allTiles = getProductsWithQuantity(ProductCategory.TILE);
            List<Product> allGutters = getProductsWithQuantity(ProductCategory.GUTTER);
            List<Product> allAccessories = getProductsWithQuantity(ProductCategory.ACCESSORY);

            // Podziel produkty na główne i opcjonalne
            // ⚠️ WAŻNE: Tylko produkty z isMainOption == true (Główna) lub isMainOption == false (Opcjonalna)
            // Produkty z isMainOption == null (Nie wybrano) są POMIJANE i NIE trafiają do PDF!
            List<Product> mainTiles = allTiles.stream()
                    .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == true)
                    .collect(Collectors.toList());
            List<Product> optionalTiles = allTiles.stream()
                    .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == false)
                    .collect(Collectors.toList());
            
            // Rynny i akcesoria - tylko te które są oznaczone jako główne (true) lub opcjonalne (false)
            // null = Nie wybrano - pomijamy!
            List<Product> mainGutters = allGutters.stream()
                    .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == true)
                    .collect(Collectors.toList());
            List<Product> optionalGutters = allGutters.stream()
                    .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == false)
                    .collect(Collectors.toList());
            
            List<Product> mainAccessories = allAccessories.stream()
                    .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == true)
                    .collect(Collectors.toList());
            List<Product> optionalAccessories = allAccessories.stream()
                    .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == false)
                    .collect(Collectors.toList());

            // Sprawdź czy mamy jakiekolwiek produkty oznaczone jako główne lub opcjonalne
            boolean hasAnyProducts = !mainTiles.isEmpty() || !optionalTiles.isEmpty() || 
                                     !mainGutters.isEmpty() || !optionalGutters.isEmpty() ||
                                     !mainAccessories.isEmpty() || !optionalAccessories.isEmpty();
            
            if (!hasAnyProducts) {
                System.err.println("⚠️ BRAK PRODUKTÓW DO PDF! (Wszystkie produkty mają status 'Nie wybrano')");
                throw new IllegalStateException("Brak produktów do wygenerowania PDF. Oznacz produkty jako 'Główna' lub 'Opcjonalna'.");
            }

            // ========== OFERTA GŁÓWNA ==========
            boolean hasMainOffer = !mainTiles.isEmpty() || !mainGutters.isEmpty() || !mainAccessories.isEmpty();
            if (hasMainOffer) {
                addElegantSectionTitle(document, "OFERTA GŁÓWNA", true);
                document.add(Chunk.NEWLINE);

                // Dachówki główne
                if (!mainTiles.isEmpty()) {
                    addElegantProductSection(document, "Dachówki ceramiczne", mainTiles);
                    document.add(Chunk.NEWLINE);
                }

                // Rynny główne
                if (!mainGutters.isEmpty()) {
                    addElegantProductSection(document, "System rynnowy", mainGutters);
                    document.add(Chunk.NEWLINE);
                }

                // Akcesoria główne
                if (!mainAccessories.isEmpty()) {
                    addElegantProductSection(document, "Akcesoria", mainAccessories);
                    document.add(Chunk.NEWLINE);
                }

                // Podsumowanie oferty głównej
                double mainTotal = calculateTotal(mainTiles, mainGutters, mainAccessories);
                addElegantSummary(document, mainTotal, true);
                document.add(Chunk.NEWLINE);
            }

            // ========== OFERTY OPCJONALNE ==========
            if (!optionalTiles.isEmpty()) {
                addElegantSectionTitle(document, "PROPOZYCJE OPCJONALNE", false);
                document.add(Chunk.NEWLINE);
                
                // Pogrupuj opcjonalne produkty po producencie i grupie (każda grupa = osobna sekcja)
                Map<String, List<Product>> groupedOptional = optionalTiles.stream()
                    .collect(Collectors.groupingBy(product -> {
                        String manufacturer = product.getManufacturer() != null ? product.getManufacturer() : "Inne";
                        String groupName = product.getGroupName() != null ? product.getGroupName() : "Bez grupy";
                        return manufacturer + " - " + groupName;
                    }));
                
                // Każda grupa jako osobna sekcja z własnym podsumowaniem
                int optionNumber = 1;
                for (Map.Entry<String, List<Product>> entry : groupedOptional.entrySet()) {
                    String groupKey = entry.getKey();
                    List<Product> groupProducts = entry.getValue();
                    
                    if (!groupProducts.isEmpty()) {
                        // Tytuł opcji: "OPCJA 1: Producent - Grupa"
                        String optionTitle = "OPCJA " + optionNumber + ": " + groupKey;
                        addElegantProductSection(document, optionTitle, groupProducts);
                        
                        // Podsumowanie dla tej opcji
                        double optionTotal = calculateTotal(groupProducts, List.of(), List.of());
                        addElegantSummary(document, optionTotal, false);
                        
                        document.add(Chunk.NEWLINE);
                        optionNumber++;
                    }
                }
            }

            // ========== STOPKA ==========
            addElegantFooter(document);

            document.close();
            System.out.println("✅ PDF utworzony pomyślnie!");

        } catch (IOException | DocumentException e) {
            System.err.println("❌ Błąd podczas tworzenia PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pobierz produkty z ilością > 0
     */
    private List<Product> getProductsWithQuantity(ProductCategory category) {
        List<Product> allProducts = productRepository.findByCategory(category);
        return allProducts.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .collect(Collectors.toList());
    }

    /**
     * ELEGANCKI NAGŁÓWEK
     */
    private void addElegantHeader(Document document, Project project) throws DocumentException, IOException {
        // Górny pasek kolorowy
        PdfPTable headerBar = new PdfPTable(1);
        headerBar.setWidthPercentage(100);
        PdfPCell headerBarCell = new PdfPCell(new Phrase(""));
        headerBarCell.setFixedHeight(8f);
        headerBarCell.setBackgroundColor(PRIMARY_COLOR);
        headerBarCell.setBorder(Rectangle.NO_BORDER);
        headerBar.addCell(headerBarCell);
        document.add(headerBar);
        document.add(Chunk.NEWLINE);
        
        // Logo/Nazwa firmy - duże i eleganckie
        Paragraph companyHeader = new Paragraph();
        companyHeader.setAlignment(Element.ALIGN_CENTER);
        companyHeader.setSpacingAfter(8);
        companyHeader.add(new Chunk("NOWOCZESNE BUDOWANIE", createFont(28, true, PRIMARY_COLOR)));
        document.add(companyHeader);
        
        // Informacje kontaktowe
        Paragraph contactInfo = new Paragraph();
        contactInfo.setAlignment(Element.ALIGN_CENTER);
        contactInfo.setSpacingAfter(20);
        String[] infoLines = StaticValuesForOffer.INFO.split("\n");
        for (String line : infoLines) {
            contactInfo.add(new Chunk(line + "\n", createFont(9, false, TEXT_LIGHT)));
        }
        contactInfo.add(new Chunk(StaticValuesForOffer.URL, createFont(9, true, PRIMARY_COLOR)));
        document.add(contactInfo);
        
        document.add(Chunk.NEWLINE);
        
        // Tytuł oferty w eleganckim boxie
        PdfPTable titleBox = new PdfPTable(1);
        titleBox.setWidthPercentage(80);
        titleBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleBox.setSpacingAfter(20);
        
        PdfPCell titleCell = new PdfPCell(new Phrase("OFERTA HANDLOWA", createFont(22, true, BaseColor.WHITE)));
        titleCell.setPadding(15);
        titleCell.setBackgroundColor(PRIMARY_COLOR);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleBox.addCell(titleCell);
        document.add(titleBox);
        
        // Informacje o projekcie i datach
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(80);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoTable.setSpacingAfter(15);
        infoTable.setWidths(new float[]{1f, 1f});
        
        if (project.getProjectName() != null && !project.getProjectName().isEmpty()) {
            PdfPCell projectCell = new PdfPCell(new Phrase("Projekt: " + project.getProjectName(), createFont(10, false, TEXT_DARK)));
            projectCell.setPadding(8);
            projectCell.setBackgroundColor(ACCENT_COLOR);
            projectCell.setBorder(Rectangle.BOX);
            projectCell.setBorderWidth(1.5f);
            infoTable.addCell(projectCell);
        } else {
            infoTable.addCell(createEmptyCell());
        }
        
        PdfPCell dateCell = new PdfPCell(new Phrase(
            "Data: " + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n" +
            "Ważna do: " + LocalDate.now().plusDays(14).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            createFont(9, false, TEXT_DARK)));
        dateCell.setPadding(8);
        dateCell.setBackgroundColor(ACCENT_COLOR);
        dateCell.setBorder(Rectangle.BOX);
        dateCell.setBorderWidth(1.5f);
        infoTable.addCell(dateCell);
        
        document.add(infoTable);
    }

    /**
     * ELEGANCKIE DANE KLIENTA
     */
    private void addElegantClientInfo(Document document, Project project) throws DocumentException, IOException {
        User client = project.getClient();
        if (client == null) return;

        PdfPTable clientSection = new PdfPTable(1);
        clientSection.setWidthPercentage(80);
        clientSection.setHorizontalAlignment(Element.ALIGN_CENTER);
        clientSection.setSpacingBefore(10);
        clientSection.setSpacingAfter(15);
        
        // Nagłówek sekcji
        PdfPCell headerCell = new PdfPCell(new Phrase("OFERTA PRZYGOTOWANA DLA", createFont(12, true, BaseColor.WHITE)));
        headerCell.setPadding(12);
        headerCell.setBackgroundColor(SECONDARY_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBorder(Rectangle.NO_BORDER);
        clientSection.addCell(headerCell);
        
        // Tabela z danymi klienta
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        clientTable.setWidths(new float[]{1.5f, 2.5f});
        
        if (client.getName() != null && client.getSurname() != null) {
            addClientDataRow(clientTable, "Imię i nazwisko:", client.getName() + " " + client.getSurname());
        }
        if (client.getAddress() != null && client.getAddress().getAddress() != null) {
            addClientDataRow(clientTable, "Adres:", client.getAddress().getAddress());
        }
        if (client.getTelephoneNumber() != null) {
            addClientDataRow(clientTable, "Telefon:", client.getTelephoneNumber());
        }
        if (client.getEmail() != null && !client.getEmail().isEmpty()) {
            addClientDataRow(clientTable, "E-mail:", client.getEmail());
        }
        
        PdfPCell tableContainer = new PdfPCell(clientTable);
        tableContainer.setPadding(15);
        tableContainer.setBorder(Rectangle.BOX);
        tableContainer.setBorderWidth(1.5f);
        tableContainer.setBackgroundColor(BaseColor.WHITE);
        clientSection.addCell(tableContainer);
        
        document.add(clientSection);
    }

    private void addClientDataRow(PdfPTable table, String label, String value) throws DocumentException, IOException {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, createFont(10, true, TEXT_DARK)));
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(ACCENT_COLOR);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, createFont(10, false, TEXT_DARK)));
        valueCell.setPadding(8);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    /**
     * ELEGANCKI TYTUŁ SEKCJI
     */
    private void addElegantSectionTitle(Document document, String title, boolean isMain) throws DocumentException, IOException {
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(20);
        titleTable.setSpacingAfter(12);
        
        PdfPCell titleCell = new PdfPCell(new Phrase(title, createFont(16, true, BaseColor.WHITE)));
        titleCell.setPadding(12);
        titleCell.setBackgroundColor(isMain ? PRIMARY_COLOR : new BaseColor(148, 163, 184));
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleTable.addCell(titleCell);
        
        document.add(titleTable);
    }

    /**
     * ELEGANCKA SEKCJA PRODUKTÓW
     */
    private void addElegantProductSection(Document document, String sectionTitle, List<Product> products) 
            throws DocumentException, IOException {
        
        if (products.isEmpty()) return;
        
        // Podtytuł sekcji
        Paragraph subTitle = new Paragraph(sectionTitle.toUpperCase(), createFont(11, true, SECONDARY_COLOR));
        subTitle.setSpacingBefore(5);
        subTitle.setSpacingAfter(10);
        document.add(subTitle);
        
        // Tabela produktów
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.5f, 1.5f, 1.8f, 2.2f});
        table.setSpacingBefore(5);
        table.setSpacingAfter(15);

        // Nagłówki kolumn
        addElegantHeaderCell(table, "Nazwa produktu");
        addElegantHeaderCell(table, "Ilość");
        addElegantHeaderCell(table, "Jednostka");
        addElegantHeaderCell(table, "Wartość");

        // Wiersze produktów
        double sectionTotal = 0;
        boolean isEven = false;
        for (Product product : products) {
            double quantity = product.getQuantity() != null ? product.getQuantity() : 0;
            double sellingPrice = product.getSellingPrice() != null ? product.getSellingPrice() : 0;
            double totalValue = quantity * sellingPrice;
            sectionTotal += totalValue;
            
            String unit = product.getUnit() != null ? product.getUnit() : "szt";
            
            addElegantDataCell(table, product.getName() != null ? product.getName() : "", isEven);
            addElegantDataCell(table, formatQuantity(quantity), isEven);
            addElegantDataCell(table, unit, isEven);
            addElegantDataCell(table, formatCurrency(totalValue), isEven);
            
            isEven = !isEven;
        }

        // Wiersz podsumowania sekcji
        PdfPCell summaryLabelCell = new PdfPCell(new Phrase("Suma " + sectionTitle + ":", createFont(10, true, TEXT_DARK)));
        summaryLabelCell.setColspan(3);
        summaryLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryLabelCell.setPadding(10);
        summaryLabelCell.setBackgroundColor(ACCENT_COLOR);
        summaryLabelCell.setBorder(Rectangle.BOX);
        summaryLabelCell.setBorderWidth(1.5f);
        table.addCell(summaryLabelCell);

        PdfPCell summaryValueCell = new PdfPCell(new Phrase(formatCurrency(sectionTotal), createFont(11, true, PRIMARY_COLOR)));
        summaryValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryValueCell.setPadding(10);
        summaryValueCell.setBackgroundColor(ACCENT_COLOR);
        summaryValueCell.setBorder(Rectangle.BOX);
        summaryValueCell.setBorderWidth(1.5f);
        table.addCell(summaryValueCell);

        document.add(table);
    }

    private void addElegantHeaderCell(PdfPTable table, String text) throws DocumentException, IOException {
        PdfPCell cell = new PdfPCell(new Phrase(text, createFont(9, true, BaseColor.WHITE)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1f);
        cell.setBorderColor(PRIMARY_COLOR);
        table.addCell(cell);
    }

    private void addElegantDataCell(PdfPTable table, String text, boolean isEven) throws DocumentException, IOException {
        PdfPCell cell = new PdfPCell(new Phrase(text, createFont(9, false, TEXT_DARK)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBackgroundColor(isEven ? ACCENT_COLOR : BaseColor.WHITE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        
        // Wyrównanie dla liczb i walut
        if (text.contains("zł") || text.matches("\\d+(\\.\\d+)?")) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        
        table.addCell(cell);
    }

    /**
     * ELEGANCKIE PODSUMOWANIE
     */
    private void addElegantSummary(Document document, double total, boolean isMain) throws DocumentException, IOException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.setSpacingBefore(10);
        summaryTable.setSpacingAfter(15);
        summaryTable.setWidths(new float[]{2f, 1.5f});

        String labelText = isMain ? "WARTOŚĆ OFERTY GŁÓWNEJ:" : "Wartość tej opcji:";
        PdfPCell labelCell = new PdfPCell(new Phrase(labelText, createFont(isMain ? 14 : 11, true, BaseColor.WHITE)));
        labelCell.setPadding(15);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(isMain ? PRIMARY_COLOR : SECONDARY_COLOR);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderWidth(2f);
        summaryTable.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(total), createFont(isMain ? 18 : 13, true, BaseColor.WHITE)));
        valueCell.setPadding(15);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setBackgroundColor(isMain ? PRIMARY_COLOR : SECONDARY_COLOR);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderWidth(2f);
        summaryTable.addCell(valueCell);

        document.add(summaryTable);
        
        if (isMain) {
            Paragraph note = new Paragraph("* Ceny netto, VAT doliczany zgodnie z obowiązującą stawką", createFont(8, false, TEXT_LIGHT));
            note.setAlignment(Element.ALIGN_CENTER);
            note.setSpacingBefore(5);
            document.add(note);
        } else {
            // Dodaj większy odstęp po każdej opcji opcjonalnej
            document.add(Chunk.NEWLINE);
        }
    }

    /**
     * ELEGANCKA STOPKA
     */
    private void addElegantFooter(Document document) throws DocumentException, IOException {
        // Linia oddzielająca
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingBefore(25);
        PdfPCell separatorCell = new PdfPCell(new Phrase(""));
        separatorCell.setFixedHeight(2f);
        separatorCell.setBackgroundColor(PRIMARY_COLOR);
        separatorCell.setBorder(Rectangle.NO_BORDER);
        separator.addCell(separatorCell);
        document.add(separator);
        document.add(Chunk.NEWLINE);
        
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(15);
        
        footer.add(new Chunk("Dziękujemy za zainteresowanie naszą ofertą!\n\n", createFont(12, true, PRIMARY_COLOR)));
        footer.add(new Chunk("W razie pytań prosimy o kontakt.\n\n", createFont(10, false, TEXT_DARK)));
        footer.add(new Chunk("Oferta ma charakter informacyjny i nie stanowi oferty handlowej w rozumieniu Kodeksu Cywilnego.", createFont(8, false, TEXT_LIGHT)));
        
        document.add(footer);
    }

    // ========== POMOCNICZE METODY ==========

    private double calculateTotal(List<Product> tiles, List<Product> gutters, List<Product> accessories) {
        double total = 0;
        for (Product p : tiles) {
            total += (p.getSellingPrice() != null ? p.getSellingPrice() : 0) * (p.getQuantity() != null ? p.getQuantity() : 0);
        }
        for (Product p : gutters) {
            total += (p.getSellingPrice() != null ? p.getSellingPrice() : 0) * (p.getQuantity() != null ? p.getQuantity() : 0);
        }
        for (Product p : accessories) {
            total += (p.getSellingPrice() != null ? p.getSellingPrice() : 0) * (p.getQuantity() != null ? p.getQuantity() : 0);
        }
        return total;
    }

    private String formatQuantity(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toString();
    }

    private String formatQuantity(double value, String unit) {
        return formatQuantity(value) + " " + (unit != null ? unit : "szt");
    }

    private String formatCurrency(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toString() + " zł";
    }

    /**
     * Tworzy elegancką czcionkę
     */
    private Font createFont(float size, boolean bold, BaseColor color) throws IOException, DocumentException {
        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
        int style = bold ? Font.BOLD : Font.NORMAL;
        return new Font(baseFont, size, style, color);
    }

    /**
     * Pusty cell
     */
    private PdfPCell createEmptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }
}