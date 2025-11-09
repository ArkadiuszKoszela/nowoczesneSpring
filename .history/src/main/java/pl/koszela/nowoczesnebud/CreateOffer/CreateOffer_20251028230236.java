package pl.koszela.nowoczesnebud.CreateOffer;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.Collectors;

/**
 * Nowoczesny generator PDF oferty
 * Używa nowego modelu Product
 */
@Service
public class CreateOffer {

    private final ProductRepository productRepository;

    public CreateOffer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void createOffer(Offer offer) {
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, new FileOutputStream(new File(StaticValuesForOffer.URL_TO_PDF)));
            document.open();

            // ========== NAGŁÓWEK ==========
            addHeader(document);
            document.add(Chunk.NEWLINE);

            // ========== DANE KLIENTA ==========
            addClientInfo(document, offer);
            document.add(Chunk.NEWLINE);

            // ========== DACHÓWKI ==========
            List<Product> tiles = getMainProducts(ProductCategory.TILE);
            if (!tiles.isEmpty()) {
                addProductSection(document, "DACHÓWKI CERAMICZNE", tiles);
                document.add(Chunk.NEWLINE);
            }

            // ========== RYNNY ==========
            List<Product> gutters = getMainProducts(ProductCategory.GUTTER);
            if (!gutters.isEmpty()) {
                addProductSection(document, "SYSTEM RYNNOWY", gutters);
                document.add(Chunk.NEWLINE);
            }

            // ========== AKCESORIA ==========
            List<Product> accessories = getMainProducts(ProductCategory.ACCESSORY);
            if (!accessories.isEmpty()) {
                addProductSection(document, "AKCESORIA", accessories);
                document.add(Chunk.NEWLINE);
            }

            // ========== PODSUMOWANIE CAŁKOWITE ==========
            addTotalSummary(document, tiles, gutters, accessories);
            document.add(Chunk.NEWLINE);

            // ========== STOPKA ==========
            addFooter(document);

            document.close();
            System.out.println("✅ PDF utworzony pomyślnie!");

        } catch (IOException | DocumentException e) {
            System.err.println("❌ Błąd podczas tworzenia PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pobierz produkty z kategorii, które mają isMainOption = true
     */
    private List<Product> getMainProducts(ProductCategory category) {
        return productRepository.findByCategory(category).stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption())
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .collect(Collectors.toList());
    }

    /**
     * NAGŁÓWEK OFERTY
     */
    private void addHeader(Document document) throws DocumentException, IOException {
        // Data i ważność oferty
        Paragraph dateSection = new Paragraph();
        dateSection.setAlignment(Element.ALIGN_RIGHT);
        dateSection.add(new Chunk("OFERTA HANDLOWA\n", font(14, true)));
        dateSection.add(new Chunk("Sporządzona: " + LocalDate.now() + "\n", font(10)));
        dateSection.add(new Chunk("Ważna do: " + LocalDate.now().plusDays(14) + "\n", font(10)));
        document.add(dateSection);

        // Linia oddzielająca
        document.add(new Paragraph(StringUtils.repeat("_", 90), font(10)));
        document.add(Chunk.NEWLINE);

        // Dane firmy
        Paragraph companyInfo = new Paragraph();
        companyInfo.add(new Chunk("NOWOCZESNE BUDOWANIE\n", font(16, true)));
        companyInfo.add(new Chunk(StaticValuesForOffer.INFO + "\n", font(9)));
        
        Chunk websiteLink = new Chunk(StaticValuesForOffer.URL + "\n", font(9));
        websiteLink.setAnchor(StaticValuesForOffer.URL);
        websiteLink.setUnderline(0.2f, -2f);
        companyInfo.add(websiteLink);
        
        document.add(companyInfo);
    }

    /**
     * DANE KLIENTA
     */
    private void addClientInfo(Document document, Offer offer) throws DocumentException, IOException {
        User user = offer.getUser();
        
        document.add(new Paragraph(StringUtils.repeat("_", 90), font(10)));
        document.add(Chunk.NEWLINE);
        
        Paragraph clientSection = new Paragraph();
        clientSection.add(new Chunk("OFERTA DLA:\n", font(12, true)));
        clientSection.add(Chunk.NEWLINE);
        
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(60);
        clientTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        addClientRow(clientTable, "Imię i nazwisko:", user.getName() + " " + user.getSurname());
        addClientRow(clientTable, "Adres:", user.getAddress().getAddress());
        addClientRow(clientTable, "Telefon:", user.getTelephoneNumber());
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            addClientRow(clientTable, "E-mail:", user.getEmail());
        }
        
        document.add(clientTable);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph(StringUtils.repeat("_", 90), font(10)));
    }

    private void addClientRow(PdfPTable table, String label, String value) throws DocumentException, IOException {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font(10, true)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font(10)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);
        table.addCell(valueCell);
    }

    /**
     * SEKCJA PRODUKTÓW (Dachówki / Rynny / Akcesoria)
     */
    private void addProductSection(Document document, String title, List<Product> products) 
            throws DocumentException, IOException {
        
        // Nagłówek sekcji
        Paragraph sectionTitle = new Paragraph(title, font(14, true));
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Tabela produktów
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 1.2f, 1.2f, 1.5f, 1.5f, 1.8f});

        // Nagłówki kolumn
        addHeaderCell(table, "Nazwa produktu");
        addHeaderCell(table, "J.m.");
        addHeaderCell(table, "Ilość");
        addHeaderCell(table, "Cena jedn.");
        addHeaderCell(table, "Rabat");
        addHeaderCell(table, "Wartość");

        // Wiersze produktów
        double sectionTotal = 0;
        for (Product product : products) {
            double quantity = product.getQuantity() != null ? product.getQuantity() : 0;
            double sellingPrice = product.getSellingPrice() != null ? product.getSellingPrice() : 0;
            double totalValue = quantity * sellingPrice;
            sectionTotal += totalValue;

            // Oblicz rabat
            double retailPrice = product.getRetailPrice() != null ? product.getRetailPrice() : sellingPrice;
            double discount = 0;
            if (retailPrice > 0) {
                discount = ((retailPrice - sellingPrice) / retailPrice) * 100;
            }

            addDataCell(table, product.getName());
            addDataCell(table, product.getUnit() != null ? product.getUnit() : "szt");
            addDataCell(table, formatNumber(quantity));
            addDataCell(table, formatCurrency(sellingPrice));
            addDataCell(table, formatPercentage(discount));
            addDataCell(table, formatCurrency(totalValue));
        }

        // Wiersz podsumowania sekcji
        PdfPCell summaryLabelCell = new PdfPCell(new Phrase("SUMA " + title + ":", font(10, true)));
        summaryLabelCell.setColspan(5);
        summaryLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryLabelCell.setPadding(8);
        summaryLabelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        table.addCell(summaryLabelCell);

        PdfPCell summaryValueCell = new PdfPCell(new Phrase(formatCurrency(sectionTotal), font(11, true)));
        summaryValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryValueCell.setPadding(8);
        summaryValueCell.setBackgroundColor(new BaseColor(240, 240, 240));
        table.addCell(summaryValueCell);

        document.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text) throws DocumentException, IOException {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, true)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBackgroundColor(new BaseColor(102, 126, 234)); // Fioletowy jak w UI
        cell.setBorderColor(BaseColor.WHITE);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) throws DocumentException, IOException {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        table.addCell(cell);
    }

    /**
     * PODSUMOWANIE CAŁKOWITE
     */
    private void addTotalSummary(Document document, List<Product> tiles, List<Product> gutters, List<Product> accessories) 
            throws DocumentException, IOException {
        
        double totalCatalog = calculateTotal(tiles, gutters, accessories, p -> p.getRetailPrice());
        double totalPurchase = calculateTotal(tiles, gutters, accessories, p -> p.getPurchasePrice());
        double totalSelling = calculateTotal(tiles, gutters, accessories, p -> p.getSellingPrice());
        double totalProfit = totalSelling - totalPurchase;

        document.add(new Paragraph(StringUtils.repeat("═", 90), font(10)));
        document.add(Chunk.NEWLINE);

        Paragraph summaryTitle = new Paragraph("PODSUMOWANIE OFERTY", font(16, true));
        summaryTitle.setAlignment(Element.ALIGN_CENTER);
        summaryTitle.setSpacingBefore(10);
        summaryTitle.setSpacingAfter(15);
        document.add(summaryTitle);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.setWidths(new float[]{3f, 2f});

        addSummaryRow(summaryTable, "Wartość katalogowa:", formatCurrency(totalCatalog), false);
        addSummaryRow(summaryTable, "Koszt zakupu:", formatCurrency(totalPurchase), false);
        addSummaryRow(summaryTable, "Zysk:", formatCurrency(totalProfit), true);
        addSummaryRow(summaryTable, "WARTOŚĆ OFERTY NETTO:", formatCurrency(totalSelling), true);

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);
        
        Paragraph vatInfo = new Paragraph("* Ceny netto, VAT doliczany zgodnie z obowiązującą stawką", font(8));
        vatInfo.setAlignment(Element.ALIGN_CENTER);
        document.add(vatInfo);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, boolean bold) 
            throws DocumentException, IOException {
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, bold ? font(12, true) : font(11)));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(10);
        labelCell.setBorder(Rectangle.NO_BORDER);
        if (bold) {
            labelCell.setBackgroundColor(new BaseColor(240, 249, 255));
        }
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, bold ? font(12, true) : font(11)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(10);
        valueCell.setBorder(Rectangle.NO_BORDER);
        if (bold) {
            valueCell.setBackgroundColor(new BaseColor(240, 249, 255));
        }
        table.addCell(valueCell);
    }

    private double calculateTotal(List<Product> tiles, List<Product> gutters, List<Product> accessories, 
                                  ProductPriceExtractor extractor) {
        double total = 0;
        
        for (Product p : tiles) {
            Double price = extractor.getPrice(p);
            total += (price != null ? price : 0) * (p.getQuantity() != null ? p.getQuantity() : 0);
        }
        for (Product p : gutters) {
            Double price = extractor.getPrice(p);
            total += (price != null ? price : 0) * (p.getQuantity() != null ? p.getQuantity() : 0);
        }
        for (Product p : accessories) {
            Double price = extractor.getPrice(p);
            total += (price != null ? price : 0) * (p.getQuantity() != null ? p.getQuantity() : 0);
        }
        
        return total;
    }

    @FunctionalInterface
    private interface ProductPriceExtractor {
        Double getPrice(Product product);
    }

    /**
     * STOPKA
     */
    private void addFooter(Document document) throws DocumentException, IOException {
        document.add(new Paragraph(StringUtils.repeat("═", 90), font(10)));
        document.add(Chunk.NEWLINE);
        
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Chunk("Dziękujemy za zainteresowanie naszą ofertą!\n", font(10, true)));
        footer.add(new Chunk("W razie pytań prosimy o kontakt.\n", font(9)));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("Oferta ma charakter informacyjny i nie stanowi oferty handlowej w rozumieniu Kodeksu Cywilnego.", font(7)));
        document.add(footer);
    }

    // ========== POMOCNICZE METODY FORMATOWANIA ==========

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toString();
    }

    private String formatCurrency(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toString() + " zł";
    }

    private String formatPercentage(double value) {
        if (value == 0) return "-";
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .toString() + "%";
    }

    // ========== CZCIONKI ==========

    private Font font() throws IOException, DocumentException {
        return font(10, false);
    }

    private Font font(float size) throws IOException, DocumentException {
        return font(size, false);
    }

    private Font font(float size, boolean bold) throws IOException, DocumentException {
        final BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
        if (bold) {
            return new Font(baseFont, size, Font.BOLD, BaseColor.WHITE);
        }
        return new Font(baseFont, size, Font.NORMAL);
    }
}
