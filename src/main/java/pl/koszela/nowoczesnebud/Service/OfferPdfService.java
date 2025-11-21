package pl.koszela.nowoczesnebud.Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.OfferTemplate;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Repository.OfferTemplateRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serwis do generowania PDF ofert z szablonów
 * Używa Thymeleaf do renderowania HTML i Flying Saucer do konwersji PDF
 */
@Service
public class OfferPdfService {

    private static final Logger logger = LoggerFactory.getLogger(OfferPdfService.class);
    
    private final OfferTemplateRepository templateRepository;
    private final SpringTemplateEngine templateEngine;

    public OfferPdfService(OfferTemplateRepository templateRepository,
                          @Qualifier("stringTemplateEngine") SpringTemplateEngine templateEngine) {
        this.templateRepository = templateRepository;
        this.templateEngine = templateEngine;
    }

    /**
     * Generuje PDF z szablonu dla projektu
     * 
     * @param project Projekt do wygenerowania oferty
     * @param templateId ID szablonu (opcjonalne - jeśli null, użyje domyślnego)
     * @return PDF jako byte array
     */
    public byte[] generatePdfFromTemplate(Project project, Long templateId) throws IOException {
        logger.info("Generowanie PDF dla projektu ID {} z szablonem ID {}", project.getId(), templateId);
        
        // Pobierz szablon
        OfferTemplate template;
        if (templateId != null) {
            template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Szablon o ID " + templateId + " nie istnieje"));
        } else {
            template = templateRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("Brak domyślnego szablonu. Utwórz szablon i ustaw jako domyślny."));
        }
        
        // Renderuj HTML z danymi projektu
        String htmlContent = renderTemplateWithData(template, project);
        
        // Konwertuj HTML → PDF
        return convertHtmlToPdf(htmlContent);
    }

    /**
     * Renderuje szablon HTML z danymi projektu
     */
    private String renderTemplateWithData(OfferTemplate template, Project project) {
        logger.debug("Renderowanie szablonu ID {} dla projektu ID {}", template.getId(), project.getId());
        
        // Przygotuj dane dla Thymeleaf
        Context context = new Context();
        context.setVariable("project", project);
        context.setVariable("client", project.getClient());
        
        // TODO: Przepisać aby używać ProjectProduct zamiast snapshotów
        // Pobierz produkty ze snapshotów (podobnie jak w CreateOffer.java)
        List<Product> allProducts = new ArrayList<>(); // TODO: getProductsFromProjectProducts(project);
        
        // Filtruj tylko produkty z quantity > 0
        allProducts = allProducts.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .collect(Collectors.toList());
        
        // Podziel po kategoriach
        List<Product> allTiles = allProducts.stream()
                .filter(p -> p.getCategory() == ProductCategory.TILE)
                .collect(Collectors.toList());
        List<Product> allGutters = allProducts.stream()
                .filter(p -> p.getCategory() == ProductCategory.GUTTER)
                .collect(Collectors.toList());
        List<Product> allAccessories = allProducts.stream()
                .filter(p -> p.getCategory() == ProductCategory.ACCESSORY)
                .collect(Collectors.toList());
        
        // Podziel produkty na główne i opcjonalne
        List<Product> mainTiles = allTiles.stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == true)
                .collect(Collectors.toList());
        List<Product> optionalTiles = allTiles.stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == false)
                .collect(Collectors.toList());
        
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
        
        // Dodaj produkty do kontekstu
        context.setVariable("mainTiles", mainTiles);
        context.setVariable("optionalTiles", optionalTiles);
        context.setVariable("mainGutters", mainGutters);
        context.setVariable("optionalGutters", optionalGutters);
        context.setVariable("mainAccessories", mainAccessories);
        context.setVariable("optionalAccessories", optionalAccessories);
        context.setVariable("allProducts", allProducts);
        
        // Oblicz sumy
        double mainTotal = calculateTotal(mainTiles, mainGutters, mainAccessories);
        double optionalTotal = calculateTotal(optionalTiles, optionalGutters, optionalAccessories);
        double totalAll = mainTotal + optionalTotal;
        
        context.setVariable("mainTotal", mainTotal);
        context.setVariable("optionalTotal", optionalTotal);
        context.setVariable("totalAll", totalAll);
        
        // Formatuj datę projektu (LocalDateTime -> String)
        String formattedDate = "";
        if (project.getCreatedAt() != null) {
            formattedDate = project.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        context.setVariable("formattedDate", formattedDate);
        
        // Jeśli szablon ma HTML content, użyj go
        String htmlTemplate = template.getHtmlContent();
        if (htmlTemplate == null || htmlTemplate.isEmpty()) {
            htmlTemplate = "<html><body><p>Szablon nie ma zawartości HTML</p></body></html>";
        }
        
        // Renderuj HTML przez Thymeleaf
        String renderedHtml = templateEngine.process(htmlTemplate, context);
        
        // Pobierz CSS - z cssContent lub wyodrębnij z HTML
        String css = template.getCssContent();
        if (css == null || css.trim().isEmpty()) {
            // Wyodrębnij CSS z HTML jeśli istnieje
            java.util.regex.Pattern stylePattern = java.util.regex.Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher styleMatcher = stylePattern.matcher(renderedHtml);
            if (styleMatcher.find()) {
                css = styleMatcher.group(1).trim();
            }
        }
        
        // NIE konwertuj CSS - użyj dokładnie takiego samego CSS jak w podglądzie
        // Jeśli Flying Saucer nie obsłuży niektórych rzeczy, możemy dodać headless Chrome jako alternatywę
        // if (css != null && !css.trim().isEmpty()) {
        //     css = convertCssForFlyingSaucer(css);
        // }
        
        // Sprawdź, czy HTML ma już pełną strukturę (DOCTYPE, html, head, body)
        boolean hasFullStructure = renderedHtml.contains("<!DOCTYPE") || renderedHtml.contains("<!doctype") ||
                                   (renderedHtml.contains("<html") && renderedHtml.contains("<head") && renderedHtml.contains("<body"));
        
        String fullHtml;
        
        if (hasFullStructure) {
            // HTML ma już pełną strukturę - dodaj/zastąp CSS w <head>
            fullHtml = renderedHtml;
            
            if (css != null && !css.trim().isEmpty()) {
                // Sprawdź, czy HTML ma już tag <style>
                java.util.regex.Pattern styleTagPattern = java.util.regex.Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher styleTagMatcher = styleTagPattern.matcher(fullHtml);
                
                if (styleTagMatcher.find()) {
                    // Zastąp istniejący <style>
                    fullHtml = styleTagMatcher.replaceFirst("<style>" + css + "</style>");
                } else {
                    // Dodaj <style> do <head>
                    if (fullHtml.contains("</head>")) {
                        fullHtml = fullHtml.replace("</head>", "<style>" + css + "</style></head>");
                    } else if (fullHtml.contains("<head>")) {
                        fullHtml = fullHtml.replace("<head>", "<head><style>" + css + "</style>");
                    } else if (fullHtml.contains("<head ")) {
                        // <head> z atrybutami
                        fullHtml = fullHtml.replaceFirst("<head[^>]*>", "$0<style>" + css + "</style>");
                    } else {
                        // Brak <head> - dodaj przed <body>
                        if (fullHtml.contains("<body>")) {
                            fullHtml = fullHtml.replace("<body>", "<head><style>" + css + "</style></head><body>");
                        } else if (fullHtml.contains("<body ")) {
                            fullHtml = fullHtml.replaceFirst("<body[^>]*>", "<head><style>" + css + "</style></head><body>");
                        }
                    }
                }
            }
        } else {
            // HTML nie ma pełnej struktury - wrap w pełny dokument XHTML
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            htmlBuilder.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
            htmlBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
            htmlBuilder.append("<head>\n");
            htmlBuilder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
            
            if (css != null && !css.trim().isEmpty()) {
                htmlBuilder.append("<style>").append(css).append("</style>\n");
            }
            
            htmlBuilder.append("</head>\n");
            htmlBuilder.append("<body>\n").append(renderedHtml).append("\n</body>\n");
            htmlBuilder.append("</html>");
            
            fullHtml = htmlBuilder.toString();
        }
        
        logger.debug("Wygenerowany HTML (pierwsze 500 znaków): {}", 
            fullHtml.length() > 500 ? fullHtml.substring(0, 500) : fullHtml);
        
        return fullHtml;
    }

    /**
     * TODO: Przepisać na nowy model - używa ProjectProduct zamiast PriceListSnapshot
     * Pobiera produkty ze snapshotów projektu (kopiowane z CreateOffer.java)
     */
    /* ZAKOMENTOWANE - używa PriceListSnapshot
    private List<Product> getProductsFromSnapshots(Project project) {
        List<Product> allProducts = new ArrayList<>();
        
        if (project.getSnapshotDate() == null) {
            return allProducts;
        }
        
        // Pobierz Input z formularza (mapperName -> quantity)
        Map<String, Double> inputMap = new HashMap<>();
        if (project.getInputs() != null) {
            inputMap = project.getInputs().stream()
                .filter(input -> input.getMapperName() != null && input.getQuantity() != null)
                .collect(Collectors.toMap(
                    input -> input.getMapperName().toLowerCase().trim(),
                    Input::getQuantity,
                    (existing, replacement) -> existing
                ));
        }
        
        // Pobierz produkty ze snapshotów dla wszystkich kategorii
        for (ProductCategory category : ProductCategory.values()) {
            Optional<PriceListSnapshot> snapshotOpt = priceListSnapshotService.findSnapshotForDate(
                project.getSnapshotDate(), category);
            
            if (snapshotOpt.isPresent()) {
                List<PriceListSnapshotItem> snapshotItems = priceListSnapshotService.getSnapshotItems(
                    snapshotOpt.get().getId());
                
                for (PriceListSnapshotItem item : snapshotItems) {
                    Product product = new Product();
                    product.setId(item.getProductId());
                    product.setName(item.getName());
                    product.setManufacturer(item.getManufacturer());
                    product.setGroupName(item.getGroupName());
                    product.setCategory(item.getCategory());
                    product.setMapperName(item.getMapperName());
                    product.setRetailPrice(item.getRetailPrice());
                    product.setPurchasePrice(item.getPurchasePrice());
                    product.setSellingPrice(item.getSellingPrice());
                    product.setBasicDiscount(item.getBasicDiscount() != null ? item.getBasicDiscount() : 0);
                    product.setPromotionDiscount(item.getPromotionDiscount() != null ? item.getPromotionDiscount() : 0);
                    product.setAdditionalDiscount(item.getAdditionalDiscount() != null ? item.getAdditionalDiscount() : 0);
                    product.setSkontoDiscount(item.getSkontoDiscount() != null ? item.getSkontoDiscount() : 0);
                    product.setMarginPercent(item.getMarginPercent() != null ? item.getMarginPercent() : 0.0);
                    product.setUnit(item.getUnit());
                    product.setQuantityConverter(item.getQuantityConverter() != null ? item.getQuantityConverter() : 1.0);
                    product.setIsMainOption(item.getIsMainOption());
                    
                    // Dopasuj quantity z Input z formularza
                    if (item.getMapperName() != null) {
                        String mapperKey = item.getMapperName().toLowerCase().trim();
                        Double inputQuantity = inputMap.get(mapperKey);
                        if (inputQuantity != null && inputQuantity > 0) {
                            double quantityConverter = product.getQuantityConverter() != null ? product.getQuantityConverter() : 1.0;
                            product.setQuantity(inputQuantity * quantityConverter);
                        } else {
                            product.setQuantity(0.0);
                        }
                    } else {
                        product.setQuantity(0.0);
                    }
                    
                    allProducts.add(product);
                }
            }
        }
        
        return allProducts;
    } */

    /**
     * Oblicza sumę wartości produktów
     */
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

    /**
     * Konwertuje CSS na bardziej kompatybilny z Flying Saucer
     * Usuwa CSS Variables i zamienia na wartości bezpośrednie
     */
    private String convertCssForFlyingSaucer(String css) {
        if (css == null || css.isEmpty()) {
            return css;
        }
        
        // Zamień CSS Variables na wartości bezpośrednie
        css = css.replaceAll("var\\(--bg\\)", "#0f172a");
        css = css.replaceAll("var\\(--card\\)", "#ffffff");
        css = css.replaceAll("var\\(--ink\\)", "#0b1220");
        css = css.replaceAll("var\\(--muted\\)", "#6b7280");
        css = css.replaceAll("var\\(--brand\\)", "#2563eb");
        css = css.replaceAll("var\\(--brand-2\\)", "#22c55e");
        css = css.replaceAll("var\\(--brand-3\\)", "#f59e0b");
        css = css.replaceAll("var\\(--line\\)", "#e5e7eb");
        
        // Usuń definicje :root (nie są obsługiwane)
        css = css.replaceAll(":root\\s*\\{[^}]*\\}", "");
        
        // Zamień display: grid na display: block (Flying Saucer nie obsługuje Grid)
        css = css.replaceAll("display:\\s*grid", "display: block");
        css = css.replaceAll("grid-template-columns:[^;]+;", "");
        css = css.replaceAll("grid-column:[^;]+;", "");
        
        // Zamień display: flex na display: block (częściowa obsługa flexbox)
        // Możemy zostawić flex, ale lepiej użyć block dla większej kompatybilności
        // css = css.replaceAll("display:\\s*flex", "display: block");
        
        return css;
    }

    /**
     * Konwertuje HTML do poprawnego XHTML (wymagane przez Flying Saucer)
     * Używa jsoup do parsowania i konwersji HTML5 → XHTML
     */
    private String convertHtmlToXhtml(String html) {
        try {
            // Parsuj HTML używając jsoup (obsługuje HTML5)
            // Używamy htmlParser() zamiast xmlParser(), bo HTML5 może mieć niepoprawny XML
            Document doc = Jsoup.parse(html);
            
            // Ustaw output settings dla XHTML
            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
            doc.outputSettings().prettyPrint(false);
            doc.outputSettings().charset(StandardCharsets.UTF_8);
            
            // Konwertuj na XHTML string
            String xhtml = doc.html();
            
            // Upewnij się, że DOCTYPE jest XHTML (zamień HTML5 DOCTYPE na XHTML)
            if (xhtml.contains("<!doctype html>") || xhtml.contains("<!DOCTYPE html>")) {
                xhtml = xhtml.replaceFirst("<!doctype html>", "");
                xhtml = xhtml.replaceFirst("<!DOCTYPE html>", "");
                xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                    xhtml;
            } else if (!xhtml.contains("<!DOCTYPE")) {
                xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                    xhtml;
            }
            
            // Upewnij się, że html ma namespace
            if (!xhtml.contains("xmlns=")) {
                xhtml = xhtml.replaceFirst("<html(\\s|>)", "<html xmlns=\"http://www.w3.org/1999/xhtml\"$1");
            }
            
            logger.debug("HTML skonwertowany na XHTML (długość: {} znaków)", xhtml.length());
            return xhtml;
        } catch (Exception e) {
            logger.warn("Błąd podczas konwersji HTML → XHTML przez jsoup, używam prostego regex: {}", e.getMessage());
            
            // Fallback: prosta konwersja regex
            html = html.replaceAll("<meta([^>]*?)(?<!/)>", "<meta$1 />");
            html = html.replaceAll("<br([^>]*?)(?<!/)>", "<br$1 />");
            html = html.replaceAll("<hr([^>]*?)(?<!/)>", "<hr$1 />");
            html = html.replaceAll("<img([^>]*?)(?<!/)>", "<img$1 />");
            html = html.replaceAll("<input([^>]*?)(?<!/)>", "<input$1 />");
            html = html.replaceAll("<link([^>]*?)(?<!/)>", "<link$1 />");
            html = html.replaceAll("<area([^>]*?)(?<!/)>", "<area$1 />");
            html = html.replaceAll("<base([^>]*?)(?<!/)>", "<base$1 />");
            html = html.replaceAll("<col([^>]*?)(?<!/)>", "<col$1 />");
            html = html.replaceAll("<embed([^>]*?)(?<!/)>", "<embed$1 />");
            html = html.replaceAll("<source([^>]*?)(?<!/)>", "<source$1 />");
            html = html.replaceAll("<track([^>]*?)(?<!/)>", "<track$1 />");
            html = html.replaceAll("<wbr([^>]*?)(?<!/)>", "<wbr$1 />");
            
            return html;
        }
    }

    /**
     * Konwertuje HTML do PDF używając Flying Saucer
     */
    private byte[] convertHtmlToPdf(String htmlContent) throws IOException {
        logger.debug("Konwersja HTML → PDF");
        
        // Konwertuj HTML na poprawny XHTML
        String xhtmlContent = convertHtmlToXhtml(htmlContent);
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            
            // Upewnij się, że XHTML ma poprawne kodowanie UTF-8
            // Flying Saucer powinien automatycznie obsłużyć polskie znaki jeśli HTML ma UTF-8
            // i używamy standardowych fontów (Helvetica, Arial, Times-Roman)
            
            renderer.setDocumentFromString(xhtmlContent, "UTF-8");
            renderer.layout();
            renderer.createPDF(outputStream);
            
            byte[] pdfBytes = outputStream.toByteArray();
            logger.info("PDF wygenerowany: {} bajtów", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            logger.error("Błąd podczas konwersji HTML → PDF", e);
            logger.error("Problematic HTML content (first 500 chars): {}", 
                xhtmlContent.length() > 500 ? xhtmlContent.substring(0, 500) : xhtmlContent);
            throw new IOException("Nie udało się wygenerować PDF: " + e.getMessage(), e);
        }
    }
}

