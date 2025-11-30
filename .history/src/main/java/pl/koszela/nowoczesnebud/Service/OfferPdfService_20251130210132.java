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
import org.xhtmlrenderer.pdf.ITextFontResolver;
import pl.koszela.nowoczesnebud.DTO.ProductComparisonDTO;
import pl.koszela.nowoczesnebud.Model.GroupOption;
import pl.koszela.nowoczesnebud.Model.OfferTemplate;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.OfferTemplateRepository;
import pl.koszela.nowoczesnebud.Service.ProjectService;

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
    private final ProjectService projectService;

    public OfferPdfService(OfferTemplateRepository templateRepository,
                          @Qualifier("stringTemplateEngine") SpringTemplateEngine templateEngine,
                          ProjectService projectService) {
        this.templateRepository = templateRepository;
        this.templateEngine = templateEngine;
        this.projectService = projectService;
    }

    /**
     * Generuje PDF z szablonu dla projektu
     * 
     * @param project Projekt do wygenerowania oferty
     * @param templateId ID szablonu (opcjonalne - jeśli null, użyje domyślnego)
     * @return PDF jako byte array
     */
    public byte[] generatePdfFromTemplate(Project project, Long templateId) throws IOException {
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
        // Przygotuj dane dla Thymeleaf
        Context context = new Context();
        context.setVariable("project", project);
        context.setVariable("client", project.getClient());
        
        // Dane klienta do prostych placeholderów
        String clientName = (project.getClient() != null && project.getClient().getName() != null)
            ? project.getClient().getName() + " " + (project.getClient().getSurname() != null ? project.getClient().getSurname() : "")
            : "Nie wybrano klienta";
        String clientAddress = (project.getClient() != null && project.getClient().getAddress() != null && project.getClient().getAddress().getAddress() != null)
            ? project.getClient().getAddress().getAddress()
            : "Brak adresu";
        String clientPhone = (project.getClient() != null && project.getClient().getTelephoneNumber() != null)
            ? project.getClient().getTelephoneNumber()
            : "Brak telefonu";
        String clientEmail = (project.getClient() != null && project.getClient().getEmail() != null)
            ? project.getClient().getEmail()
            : "Brak email";
        
        context.setVariable("clientName", clientName.trim());
        context.setVariable("clientAddress", clientAddress);
        context.setVariable("clientPhone", clientPhone);
        context.setVariable("clientEmail", clientEmail);
        
        // ⚠️ WAŻNE: Użyj tego samego mechanizmu co frontend - getProductComparison()
        // To zapewnia, że placeholdery w PDF będą pokazywać dokładnie te same dane co tabele w UI
        List<Product> allProducts = getProductsFromProductComparison(project);
        
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
        // Dla Dachówek i Rynien: tylko produkty oznaczone jako "Główna" (true) lub "Opcjonalna" (false)
        // Dla Akcesoriów: wszystkie produkty (nie filtruj po isMainOption)
        
        // ⚠️ WAŻNE: Dla Dachówek i Rynien filtrujemy tylko produkty z isMainOption != null
        // Jeśli nie ma żadnych produktów z isMainOption, użyj wszystkich produktów (fallback)
        
        List<Product> mainTiles = allTiles.stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == GroupOption.MAIN)
                .collect(Collectors.toList());
        List<Product> optionalTiles = allTiles.stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == GroupOption.OPTIONAL)
                .collect(Collectors.toList());
        
        long tilesWithoutOption = allTiles.stream().filter(p -> p.getIsMainOption() == null || p.getIsMainOption() == GroupOption.NONE).count();
        logger.info("📦 Dachówki - Główne: {}, Opcjonalne: {}, Bez opcji: {}", 
            mainTiles.size(), optionalTiles.size(), tilesWithoutOption);
        
        // Dla Dachówek: połącz główne i opcjonalne (dla tabeli)
        // Jeśli nie ma żadnych produktów z opcją, użyj wszystkich (fallback)
        List<Product> allTilesForTable = new ArrayList<>(mainTiles);
        allTilesForTable.addAll(optionalTiles);
        if (allTilesForTable.isEmpty() && !allTiles.isEmpty()) {
            logger.warn("⚠️ Brak dachówek z opcją (Główna/Opcjonalna) - używam wszystkich dachówek jako fallback");
            allTilesForTable = new ArrayList<>(allTiles);
        }
        
        List<Product> mainGutters = allGutters.stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == GroupOption.MAIN)
                .collect(Collectors.toList());
        List<Product> optionalGutters = allGutters.stream()
                .filter(p -> p.getIsMainOption() != null && p.getIsMainOption() == GroupOption.OPTIONAL)
                .collect(Collectors.toList());
        
        long guttersWithoutOption = allGutters.stream().filter(p -> p.getIsMainOption() == null || p.getIsMainOption() == GroupOption.NONE).count();
        logger.info("📦 Rynny - Główne: {}, Opcjonalne: {}, Bez opcji: {}", 
            mainGutters.size(), optionalGutters.size(), guttersWithoutOption);
        
        // Dla Rynien: połącz główne i opcjonalne (dla tabeli)
        // Jeśli nie ma żadnych produktów z opcją, użyj wszystkich (fallback)
        List<Product> allGuttersForTable = new ArrayList<>(mainGutters);
        allGuttersForTable.addAll(optionalGutters);
        if (allGuttersForTable.isEmpty() && !allGutters.isEmpty()) {
            logger.warn("⚠️ Brak rynien z opcją (Główna/Opcjonalna) - używam wszystkich rynien jako fallback");
            allGuttersForTable = new ArrayList<>(allGutters);
        }
        
        // Dla Akcesoriów: wszystkie produkty (nie filtruj po isMainOption)
        List<Product> mainAccessories = allAccessories; // Wszystkie akcesoria
        List<Product> optionalAccessories = new ArrayList<>(); // Pusta lista dla akcesoriów (nie używamy opcjonalnych)
        
        logger.info("📦 Akcesoria: {} produktów", mainAccessories.size());
        logger.info("📦 Tabele - Dachówki: {}, Rynny: {}, Akcesoria: {}", 
            allTilesForTable.size(), allGuttersForTable.size(), mainAccessories.size());
        
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
        String currentDate = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        if (project.getCreatedAt() != null) {
            formattedDate = project.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        context.setVariable("formattedDate", formattedDate);
        context.setVariable("projectDate", formattedDate);
        context.setVariable("currentDate", currentDate);
        // Nazwa projektu - używamy ID projektu lub nazwy klienta
        String projectName = "Projekt #" + project.getId();
        if (project.getClient() != null && project.getClient().getName() != null) {
            projectName = "Projekt dla " + project.getClient().getName() + 
                         (project.getClient().getSurname() != null ? " " + project.getClient().getSurname() : "");
        }
        context.setVariable("projectName", projectName);
        
        // Dane firmy (TODO: pobrać z konfiguracji)
        context.setVariable("companyName", "Twoja Firma Sp. z o.o.");
        context.setVariable("companyAddress", "ul. Przykładowa 123, 00-000 Warszawa");
        context.setVariable("companyNIP", "123-456-78-90");
        context.setVariable("companyPhone", "+48 123 456 789");
        context.setVariable("companyEmail", "kontakt@twojafirma.pl");
        context.setVariable("companyWebsite", "www.twojafirma.pl");
        context.setVariable("companyLogo", "<img src=\"/assets/logo.png\" alt=\"Logo\" style=\"max-width: 200px; height: auto;\" />");
        
        // Oblicz ceny dla każdej kategorii
        // Dla Dachówek i Rynien: uwzględnij główne i opcjonalne
        double tilesPrice = calculateTotal(allTilesForTable, new ArrayList<>(), new ArrayList<>());
        double guttersPrice = calculateTotal(new ArrayList<>(), allGuttersForTable, new ArrayList<>());
        double accessoriesPrice = calculateTotal(new ArrayList<>(), new ArrayList<>(), mainAccessories);
        
        // Dodaj ceny do kontekstu
        context.setVariable("totalPrice", String.format("%.2f", totalAll));
        context.setVariable("tilesPrice", String.format("%.2f", tilesPrice));
        context.setVariable("guttersPrice", String.format("%.2f", guttersPrice));
        context.setVariable("accessoriesPrice", String.format("%.2f", accessoriesPrice));
        context.setVariable("windowsPrice", "0.00"); // TODO: Dodać obsługę okien
        
        // Generuj tabele produktów jako HTML
        logger.info("🔨 Generowanie tabel produktów...");
        String allProductsTable = generateAllProductsTable(allTilesForTable, allGuttersForTable, mainAccessories, new ArrayList<>());
        
        // Tabele dla wszystkich produktów (główne + opcjonalne)
        String tilesTable = generateCategoryTable(allTilesForTable, "Dachówki");
        String guttersTable = generateCategoryTable(allGuttersForTable, "Rynny");
        String accessoriesTable = generateCategoryTable(mainAccessories, "Akcesoria");
        String windowsTable = "<p>Brak okien w ofercie</p>"; // TODO: Dodać obsługę okien
        
        // Tabele dla produktów głównych (tylko isMainOption = MAIN)
        logger.info("🔨 Generowanie tabel głównych - mainTiles: {} produktów, mainGutters: {} produktów", 
            mainTiles.size(), mainGutters.size());
        String tilesMainTable = generateCategoryTable(mainTiles, "Dachówki - Główne");
        String guttersMainTable = generateCategoryTable(mainGutters, "Rynny - Główne");
        String windowsMainTable = "<p>Brak okien głównych w ofercie</p>"; // TODO: Dodać obsługę okien
        
        // Jeśli tabela główna jest pusta, dodaj komunikat
        if (tilesMainTable.isEmpty() && !mainTiles.isEmpty()) {
            logger.warn("⚠️ tilesMainTable jest puste mimo {} produktów głównych", mainTiles.size());
            tilesMainTable = "<p style=\"color: #999; font-style: italic;\">Brak dachówek głównych do wyświetlenia</p>";
        } else if (tilesMainTable.isEmpty()) {
            logger.warn("⚠️ tilesMainTable jest puste - brak produktów głównych dla dachówek");
            tilesMainTable = "<p style=\"color: #999; font-style: italic;\">Brak dachówek głównych w ofercie</p>";
        }
        
        if (guttersMainTable.isEmpty() && !mainGutters.isEmpty()) {
            logger.warn("⚠️ guttersMainTable jest puste mimo {} produktów głównych", mainGutters.size());
            guttersMainTable = "<p style=\"color: #999; font-style: italic;\">Brak rynien głównych do wyświetlenia</p>";
        } else if (guttersMainTable.isEmpty()) {
            logger.warn("⚠️ guttersMainTable jest puste - brak produktów głównych dla rynien");
            guttersMainTable = "<p style=\"color: #999; font-style: italic;\">Brak rynien głównych w ofercie</p>";
        }
        
        // Tabele dla produktów opcjonalnych (tylko isMainOption = OPTIONAL) - TYLKO SUMY dla każdej grupy
        logger.info("🔨 Generowanie tabel opcjonalnych - optionalTiles: {} produktów, optionalGutters: {} produktów", 
            optionalTiles.size(), optionalGutters.size());
        String tilesOptionalTable = generateOptionalGroupsSummaryTable(optionalTiles, "Dachówki");
        String guttersOptionalTable = generateOptionalGroupsSummaryTable(optionalGutters, "Rynny");
        String windowsOptionalTable = "<p>Brak okien opcjonalnych w ofercie</p>"; // TODO: Dodać obsługę okien
        
        // Jeśli tabela opcjonalna jest pusta, dodaj komunikat
        if (tilesOptionalTable.isEmpty() && !optionalTiles.isEmpty()) {
            logger.warn("⚠️ tilesOptionalTable jest puste mimo {} produktów opcjonalnych", optionalTiles.size());
            tilesOptionalTable = "<p style=\"color: #999; font-style: italic;\">Brak dachówek opcjonalnych do wyświetlenia</p>";
        } else if (tilesOptionalTable.isEmpty()) {
            logger.warn("⚠️ tilesOptionalTable jest puste - brak produktów opcjonalnych dla dachówek");
            tilesOptionalTable = "<p style=\"color: #999; font-style: italic;\">Brak dachówek opcjonalnych w ofercie</p>";
        }
        
        if (guttersOptionalTable.isEmpty() && !optionalGutters.isEmpty()) {
            logger.warn("⚠️ guttersOptionalTable jest puste mimo {} produktów opcjonalnych", optionalGutters.size());
            guttersOptionalTable = "<p style=\"color: #999; font-style: italic;\">Brak rynien opcjonalnych do wyświetlenia</p>";
        } else if (guttersOptionalTable.isEmpty()) {
            logger.warn("⚠️ guttersOptionalTable jest puste - brak produktów opcjonalnych dla rynien");
            guttersOptionalTable = "<p style=\"color: #999; font-style: italic;\">Brak rynien opcjonalnych w ofercie</p>";
        }
        
        logger.info("🔨 Wygenerowane tabele - tilesTable: {} znaków, guttersTable: {} znaków, accessoriesTable: {} znaków", 
            tilesTable.length(), guttersTable.length(), accessoriesTable.length());
        logger.info("🔨 Tabele główne - tilesMainTable: {} znaków, guttersMainTable: {} znaków", 
            tilesMainTable.length(), guttersMainTable.length());
        logger.info("🔨 Tabele opcjonalne - tilesOptionalTable: {} znaków, guttersOptionalTable: {} znaków", 
            tilesOptionalTable.length(), guttersOptionalTable.length());
        
        // Dodaj wszystkie tabele do kontekstu
        context.setVariable("productsTable", allProductsTable);
        context.setVariable("tilesTable", tilesTable);
        context.setVariable("tilesMainTable", tilesMainTable);
        context.setVariable("tilesOptionalTable", tilesOptionalTable);
        context.setVariable("guttersTable", guttersTable);
        context.setVariable("guttersMainTable", guttersMainTable);
        context.setVariable("guttersOptionalTable", guttersOptionalTable);
        context.setVariable("windowsTable", windowsTable);
        context.setVariable("windowsMainTable", windowsMainTable);
        context.setVariable("windowsOptionalTable", windowsOptionalTable);
        context.setVariable("accessoriesTable", accessoriesTable);
        
        // Jeśli szablon ma HTML content, użyj go
        String htmlTemplate = template.getHtmlContent();
        if (htmlTemplate == null || htmlTemplate.isEmpty()) {
            htmlTemplate = "<html><body><p>Szablon nie ma zawartości HTML</p></body></html>";
        }
        
        // ⚠️ WAŻNE: Najpierw zastąp placeholdery BEZPOŚREDNIO wartościami
        // To zapewnia, że HTML z TinyMCE (z inline styles) będzie poprawnie renderowany
        String renderedHtml = replacePlaceholdersDirectly(htmlTemplate, context);
        
        // Pobierz CSS - z cssContent lub wyodrębnij z HTML
        String css = template.getCssContent();
        if (css == null || css.trim().isEmpty()) {
            // Wyodrębnij CSS z HTML jeśli istnieje (TinyMCE może dodać <style> tag)
            java.util.regex.Pattern stylePattern = java.util.regex.Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher styleMatcher = stylePattern.matcher(renderedHtml);
            if (styleMatcher.find()) {
                css = styleMatcher.group(1).trim();
                // Usuń <style> tag z HTML (będzie dodany później do <head>)
                renderedHtml = styleMatcher.replaceAll("");
            }
        }
        
        // ⚠️ WAŻNE: TinyMCE używa inline styles - one są już w HTML i będą działać w PDF
        // Dodatkowy CSS z cssContent będzie dodany do <head> dla globalnych stylów
        // NIE konwertuj CSS - użyj dokładnie takiego samego CSS jak w podglądzie TinyMCE
        
        // Sprawdź, czy HTML ma już pełną strukturę (DOCTYPE, html, head, body)
        boolean hasFullStructure = renderedHtml.contains("<!DOCTYPE") || renderedHtml.contains("<!doctype") ||
                                   (renderedHtml.contains("<html") && renderedHtml.contains("<head") && renderedHtml.contains("<body"));
        
        String fullHtml;
        
        if (hasFullStructure) {
            // HTML ma już pełną strukturę - dodaj/zastąp CSS w <head>
            fullHtml = renderedHtml;
            
            // ⚠️ WAŻNE: Zawsze dodaj białe tło do body (inline style)
            if (fullHtml.contains("<body>")) {
                fullHtml = fullHtml.replace("<body>", "<body style=\"background-color: #ffffff;\">");
            } else if (fullHtml.contains("<body ")) {
                // Jeśli body ma już style, dodaj background-color
                if (fullHtml.matches(".*<body[^>]*style\\s*=\\s*[\"'][^\"']*[\"'][^>]*>.*")) {
                    // Body ma już style - dodaj background-color jeśli nie ma
                    if (!fullHtml.matches(".*<body[^>]*style\\s*=\\s*[\"'][^\"']*background[^\"']*[\"'][^>]*>.*")) {
                        fullHtml = fullHtml.replaceFirst("(<body[^>]*style\\s*=\\s*[\"'])([^\"']*)([\"'][^>]*>)", "$1$2; background-color: #ffffff !important;$3");
                    }
                } else {
                    // Body nie ma style - dodaj
                    fullHtml = fullHtml.replaceFirst("<body([^>]*)>", "<body$1 style=\"background-color: #ffffff;\">");
                }
            }
            
            if (css != null && !css.trim().isEmpty()) {
                // Sprawdź, czy HTML ma już tag <style>
                java.util.regex.Pattern styleTagPattern = java.util.regex.Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher styleTagMatcher = styleTagPattern.matcher(fullHtml);
                
                if (styleTagMatcher.find()) {
                    // Zastąp istniejący <style> - dodaj białe tło i fonty jeśli nie ma
                    String styleContent = css;
                    if (!css.contains("background-color") && !css.contains("background:")) {
                        styleContent = "body { background-color: #ffffff !important; margin: 0; padding: 20px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }\n" + css;
                    }
                    if (!css.contains("font-family")) {
                        styleContent = "* { font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }\n" + styleContent;
                    }
                    fullHtml = styleTagMatcher.replaceFirst("<style>" + styleContent + "</style>");
                } else {
                    // Dodaj <style> do <head> - zawsze z białym tłem i fontami
                    String styleContent = css;
                    if (!css.contains("background-color") && !css.contains("background:")) {
                        styleContent = "body { background-color: #ffffff !important; margin: 0; padding: 20px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }\n" + css;
                    }
                    if (!css.contains("font-family")) {
                        styleContent = "* { font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }\n" + styleContent;
                    }
                    if (fullHtml.contains("</head>")) {
                        fullHtml = fullHtml.replace("</head>", "<style>" + styleContent + "</style></head>");
                    } else if (fullHtml.contains("<head>")) {
                        fullHtml = fullHtml.replace("<head>", "<head><style>" + styleContent + "</style>");
                    } else if (fullHtml.contains("<head ")) {
                        // <head> z atrybutami
                        fullHtml = fullHtml.replaceFirst("<head[^>]*>", "$0<style>" + styleContent + "</style>");
                    }
                }
            } else {
                // Brak CSS - dodaj białe tło i fonty
                if (!fullHtml.contains("body") || !fullHtml.matches(".*<style[^>]*>[\\s\\S]*?body[\\s\\S]*?</style>.*")) {
                    if (fullHtml.contains("</head>")) {
                        fullHtml = fullHtml.replace("</head>", "<style>body { background-color: #ffffff !important; margin: 0; padding: 20px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; } * { font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }</style></head>");
                    } else if (fullHtml.contains("<head>")) {
                        fullHtml = fullHtml.replace("<head>", "<head><style>body { background-color: #ffffff !important; margin: 0; padding: 20px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; } * { font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }</style>");
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
            
            // ⚠️ WAŻNE: Dodaj domyślne style dla białego tła i fontów obsługujących polskie znaki
            htmlBuilder.append("<style>\n");
            htmlBuilder.append("body { background-color: #ffffff !important; margin: 0; padding: 20px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }\n");
            htmlBuilder.append("* { font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; }\n");
            htmlBuilder.append("</style>\n");
            
            htmlBuilder.append("</head>\n");
            htmlBuilder.append("<body style=\"background-color: #ffffff;\">\n").append(renderedHtml).append("\n</body>\n");
            htmlBuilder.append("</html>");
            
            fullHtml = htmlBuilder.toString();
        }
        
        logger.debug("Wygenerowany HTML (pierwsze 500 znaków): {}", 
            fullHtml.length() > 500 ? fullHtml.substring(0, 500) : fullHtml);
        
        return fullHtml;
    }

    /**
     * Pobiera produkty używając tego samego mechanizmu co frontend - getProductComparison()
     * To zapewnia, że placeholdery w PDF będą pokazywać dokładnie te same dane co tabele w UI
     * 
     * Logika:
     * - Pobiera wszystkie produkty z cennika
     * - Łączy z zapisanymi danymi z ProjectProduct
     * - Uwzględnia draft changes (jeśli istnieją)
     * - Używa priorytetów: draft > saved > current
     * - Używa isMainOption z draft lub ProjectProductGroup
     */
    private List<Product> getProductsFromProductComparison(Project project) {
        logger.debug("Pobieranie produktów używając getProductComparison() dla projektu ID: {}", project.getId());
        
        List<Product> allProducts = new ArrayList<>();
        
        // Pobierz produkty dla każdej kategorii używając getProductComparison() (ten sam mechanizm co frontend)
        for (ProductCategory category : ProductCategory.values()) {
            List<ProductComparisonDTO> comparison = projectService.getProductComparison(project.getId(), category);
            logger.info("🔍 Pobrano {} produktów z getProductComparison() dla kategorii {}", comparison.size(), category);
            
            for (ProductComparisonDTO dto : comparison) {
                // ⚠️ WAŻNE: Użyj priorytetów tak jak w UI:
                // 1. draftQuantity (jeśli istnieje) - tymczasowe, niezapisane zmiany
                // 2. savedQuantity (jeśli istnieje) - zapisane dane z projektu
                // 3. 0 (domyślnie) - produkt nie jest w projekcie
                Double quantity = null;
                if (dto.getDraftQuantity() != null && dto.getDraftQuantity() > 0) {
                    quantity = dto.getDraftQuantity();
                } else if (dto.getSavedQuantity() != null && dto.getSavedQuantity() > 0) {
                    quantity = dto.getSavedQuantity();
                }
                
                // Pomiń produkty z ilością 0 lub null
                if (quantity == null || quantity <= 0) {
                    continue;
                }
                
                // ⚠️ WAŻNE: Użyj priorytetów dla cen (tak jak w UI):
                // 1. draftSellingPrice (jeśli istnieje) - tymczasowe, niezapisane zmiany
                // 2. savedSellingPrice (jeśli istnieje) - zapisane dane z projektu
                // 3. currentSellingPrice - aktualna cena z cennika
                Double sellingPrice = dto.getDraftSellingPrice() != null 
                    ? dto.getDraftSellingPrice() 
                    : (dto.getSavedSellingPrice() != null 
                        ? dto.getSavedSellingPrice() 
                        : dto.getCurrentSellingPrice());
                
                Double retailPrice = dto.getDraftRetailPrice() != null 
                    ? dto.getDraftRetailPrice() 
                    : (dto.getSavedRetailPrice() != null 
                        ? dto.getSavedRetailPrice() 
                        : dto.getCurrentRetailPrice());
                
                Double purchasePrice = dto.getDraftPurchasePrice() != null 
                    ? dto.getDraftPurchasePrice() 
                    : (dto.getSavedPurchasePrice() != null 
                        ? dto.getSavedPurchasePrice() 
                        : dto.getCurrentPurchasePrice());
                
                // Utwórz Product z danymi z ProductComparisonDTO
                Product product = new Product();
                product.setId(dto.getProductId());
                product.setName(dto.getName());
                product.setManufacturer(dto.getManufacturer());
                product.setGroupName(dto.getGroupName());
                product.setCategory(dto.getCategory());
                product.setMapperName(dto.getMapperName());
                product.setUnit(dto.getUnit());
                product.setQuantityConverter(dto.getQuantityConverter() != null ? dto.getQuantityConverter() : 1.0);
                
                // Użyj cen z priorytetami (draft > saved > current)
                product.setRetailPrice(retailPrice);
                product.setPurchasePrice(purchasePrice);
                product.setSellingPrice(sellingPrice);
                
                // Użyj ilości z priorytetami (draft > saved)
                product.setQuantity(quantity);
                
                // ⚠️ WAŻNE: Użyj isMainOption z ProductComparisonDTO (już ma priorytety: draft > saved > null)
                product.setIsMainOption(dto.getIsMainOption());
                
                logger.debug("Produkt '{}' - isMainOption: {}, quantity: {}, category: {}", 
                    product.getName(), dto.getIsMainOption(), quantity, dto.getCategory());
                
                allProducts.add(product);
            }
        }
        
        logger.info("✅ Przetworzono {} produktów z getProductComparison() (ten sam mechanizm co frontend)", allProducts.size());
        
        // Loguj statystyki isMainOption
        long withMain = allProducts.stream().filter(p -> p.getIsMainOption() == GroupOption.MAIN).count();
        long withOptional = allProducts.stream().filter(p -> p.getIsMainOption() == GroupOption.OPTIONAL).count();
        long withNone = allProducts.stream().filter(p -> p.getIsMainOption() == null || p.getIsMainOption() == GroupOption.NONE).count();
        long withOption = allProducts.stream().filter(p -> p.getIsMainOption() != null && p.getIsMainOption() != GroupOption.NONE).count();
        logger.info("📊 Statystyki isMainOption - MAIN: {}, OPTIONAL: {}, NONE: {}, z opcją: {}", 
                   withMain, withOptional, withNone, withOption);
        
        return allProducts;
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
     * Usuwa również wszystkie atrybuty Thymeleaf (th:*), które nie są obsługiwane przez Flying Saucer
     */
    private String convertHtmlToXhtml(String html) {
        try {
            // Parsuj HTML używając jsoup (obsługuje HTML5)
            // Używamy htmlParser() zamiast xmlParser(), bo HTML5 może mieć niepoprawny XML
            Document doc = Jsoup.parse(html);
            
            // ⚠️ WAŻNE: Usuń wszystkie atrybuty Thymeleaf (th:*) - Flying Saucer ich nie rozumie
            doc.select("*").forEach(element -> {
                // Pobierz wszystkie atrybuty
                org.jsoup.nodes.Attributes attributes = element.attributes();
                // Utwórz listę atrybutów do usunięcia (nie można modyfikować podczas iteracji)
                List<String> attributesToRemove = new ArrayList<>();
                for (org.jsoup.nodes.Attribute attr : attributes) {
                    // Jeśli atrybut zaczyna się od "th:", usuń go
                    if (attr.getKey().startsWith("th:")) {
                        attributesToRemove.add(attr.getKey());
                    }
                }
                // Usuń atrybuty
                for (String attrKey : attributesToRemove) {
                    element.removeAttr(attrKey);
                }
            });
            
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
            
            // ⚠️ WAŻNE: Usuń wszystkie atrybuty Thymeleaf (th:*) - Flying Saucer ich nie rozumie
            // Używamy regex do usunięcia wszystkich atrybutów th:* z tagów
            html = html.replaceAll("\\s+th:[^\\s=]+(=\"[^\"]*\")?", "");
            
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
            
            // ⚠️ WAŻNE: Konfiguruj fonty obsługujące polskie znaki
            configureFontsForPolishCharacters(renderer);
            
            // Upewnij się, że XHTML ma poprawne kodowanie UTF-8
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
    
    /**
     * Konfiguruje fonty obsługujące polskie znaki dla ITextRenderer
     * Używa fontów systemowych Windows (Arial, Times New Roman) lub standardowych fontów z obsługą Unicode
     */
    private void configureFontsForPolishCharacters(ITextRenderer renderer) {
        try {
            ITextFontResolver fontResolver = renderer.getFontResolver();
            
            // Próbuj użyć fontów systemowych Windows (obsługują polskie znaki)
            boolean fontsLoaded = false;
            
            // Próbuj załadować fonty systemowe Windows (obsługują polskie znaki)
            try {
                java.io.File fontsDir = new java.io.File("C:/Windows/Fonts");
                if (fontsDir.exists() && fontsDir.isDirectory()) {
                    // Dodaj katalog z fontami - Flying Saucer automatycznie załaduje wszystkie fonty
                    // i użyje kodowania Unicode dla polskich znaków
                    // Parametr true oznacza, że fonty będą osadzone w PDF (embedded)
                    fontResolver.addFontDirectory("C:/Windows/Fonts", true);
                    fontsLoaded = true;
                    logger.info("✅ Załadowano katalog fontów systemowych Windows dla polskich znaków");
                } else {
                    logger.warn("Katalog fontów systemowych Windows nie znaleziony: C:/Windows/Fonts");
                }
            } catch (Exception e) {
                logger.warn("Nie udało się załadować katalogu fontów systemowych Windows: {}", e.getMessage());
            }
            
            // Jeśli fonty systemowe nie są dostępne, loguj ostrzeżenie
            // Flying Saucer użyje domyślnych fontów, które mogą nie obsługiwać wszystkich polskich znaków
            if (!fontsLoaded) {
                logger.warn("⚠️ Fonty systemowe Windows nie są dostępne - polskie znaki mogą nie być poprawnie wyświetlane w PDF");
                logger.warn("Upewnij się, że HTML używa kodowania UTF-8 i fontów obsługujących polskie znaki w CSS");
            }
            
        } catch (Exception e) {
            logger.error("Błąd podczas konfiguracji fontów dla polskich znaków: {}", e.getMessage(), e);
            // Kontynuuj bez konfiguracji fontów - może działać z domyślnymi
        }
    }
    
    /**
     * Zastępuje placeholdery {{variable}} i wyrażenia Thymeleaf [[${...}]] bezpośrednio wartościami z kontekstu
     * To zapewnia, że HTML z TinyMCE (z inline styles) będzie poprawnie renderowany
     */
    private String replacePlaceholdersDirectly(String html, Context context) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        
        String result = html;
        
        // ⚠️ WAŻNE: Obsługujemy zarówno {{placeholder}} jak i [[${...}]] (Thymeleaf)
        
        // Dane klienta - obsługa obu formatów
        String clientName = getVariableAsString(context, "clientName", "");
        String clientSurname = "";
        if (context.getVariable("client") != null) {
            User client = (User) context.getVariable("client");
            if (client.getSurname() != null) {
                clientSurname = client.getSurname();
            }
            // Jeśli clientName zawiera już imię i nazwisko, wyodrębnij tylko imię
            if (client.getName() != null && clientName.contains(client.getName())) {
                clientName = client.getName();
            }
        }
        String fullClientName = (clientName + " " + clientSurname).trim();
        
        result = replacePlaceholder(result, "clientName", fullClientName);
        // Obsługa wyrażeń Thymeleaf: [[${project.client.name}]] [[${project.client.surname}]]
        result = replaceThymeleafExpression(result, "project\\.client\\.name", clientName);
        result = replaceThymeleafExpression(result, "project\\.client\\.surname", clientSurname);
        result = replaceThymeleafExpression(result, "client\\.name", clientName);
        result = replaceThymeleafExpression(result, "client\\.surname", clientSurname);
        result = replacePlaceholder(result, "clientAddress", getVariableAsString(context, "clientAddress", ""));
        result = replaceThymeleafExpression(result, "client.address", getVariableAsString(context, "clientAddress", ""));
        
        String clientPhone = getVariableAsString(context, "clientPhone", "");
        result = replacePlaceholder(result, "clientPhone", clientPhone);
        result = replaceThymeleafExpression(result, "client.phone", clientPhone);
        result = replaceThymeleafExpression(result, "client.telephoneNumber", clientPhone);
        
        String clientEmail = getVariableAsString(context, "clientEmail", "");
        result = replacePlaceholder(result, "clientEmail", clientEmail);
        result = replaceThymeleafExpression(result, "client.email", clientEmail);
        
        // Dane projektu
        String projectName = getVariableAsString(context, "projectName", "");
        result = replacePlaceholder(result, "projectName", projectName);
        result = replaceThymeleafExpression(result, "project.projectName", projectName);
        
        String formattedDate = getVariableAsString(context, "formattedDate", "");
        String projectDate = getVariableAsString(context, "projectDate", formattedDate);
        String currentDate = getVariableAsString(context, "currentDate", "");
        result = replacePlaceholder(result, "projectDate", projectDate);
        result = replacePlaceholder(result, "currentDate", currentDate);
        result = replaceThymeleafExpression(result, "formattedDate", formattedDate);
        result = replaceThymeleafExpression(result, "projectDate", projectDate);
        result = replaceThymeleafExpression(result, "currentDate", currentDate);
        
        // Dane firmy
        result = replacePlaceholder(result, "companyName", getVariableAsString(context, "companyName", ""));
        result = replacePlaceholder(result, "companyAddress", getVariableAsString(context, "companyAddress", ""));
        result = replacePlaceholder(result, "companyNIP", getVariableAsString(context, "companyNIP", ""));
        result = replacePlaceholder(result, "companyPhone", getVariableAsString(context, "companyPhone", ""));
        result = replacePlaceholder(result, "companyEmail", getVariableAsString(context, "companyEmail", ""));
        result = replacePlaceholder(result, "companyWebsite", getVariableAsString(context, "companyWebsite", ""));
        result = replacePlaceholder(result, "companyLogo", getVariableAsString(context, "companyLogo", ""));
        
        // Tabele produktów - wszystkie (główne + opcjonalne)
        result = replacePlaceholder(result, "productsTable", getVariableAsString(context, "productsTable", ""));
        result = replacePlaceholder(result, "tilesTable", getVariableAsString(context, "tilesTable", ""));
        result = replacePlaceholder(result, "guttersTable", getVariableAsString(context, "guttersTable", ""));
        result = replacePlaceholder(result, "windowsTable", getVariableAsString(context, "windowsTable", ""));
        result = replacePlaceholder(result, "accessoriesTable", getVariableAsString(context, "accessoriesTable", ""));
        
        // Tabele produktów głównych (tylko isMainOption = true)
        result = replacePlaceholder(result, "tilesMainTable", getVariableAsString(context, "tilesMainTable", ""));
        result = replacePlaceholder(result, "guttersMainTable", getVariableAsString(context, "guttersMainTable", ""));
        result = replacePlaceholder(result, "windowsMainTable", getVariableAsString(context, "windowsMainTable", ""));
        
        // Tabele produktów opcjonalnych (tylko isMainOption = false)
        result = replacePlaceholder(result, "tilesOptionalTable", getVariableAsString(context, "tilesOptionalTable", ""));
        result = replacePlaceholder(result, "guttersOptionalTable", getVariableAsString(context, "guttersOptionalTable", ""));
        result = replacePlaceholder(result, "windowsOptionalTable", getVariableAsString(context, "windowsOptionalTable", ""));
        
        // Ceny - obsługa formatowania liczb
        String totalPrice = getVariableAsString(context, "totalPrice", "0.00");
        String tilesPrice = getVariableAsString(context, "tilesPrice", "0.00");
        String guttersPrice = getVariableAsString(context, "guttersPrice", "0.00");
        String windowsPrice = getVariableAsString(context, "windowsPrice", "0.00");
        String accessoriesPrice = getVariableAsString(context, "accessoriesPrice", "0.00");
        
        result = replacePlaceholder(result, "totalPrice", totalPrice);
        result = replacePlaceholder(result, "tilesPrice", tilesPrice);
        result = replacePlaceholder(result, "guttersPrice", guttersPrice);
        result = replacePlaceholder(result, "windowsPrice", windowsPrice);
        result = replacePlaceholder(result, "accessoriesPrice", accessoriesPrice);
        
        // Obsługa wyrażeń Thymeleaf z formatowaniem liczb: [[${#numbers.formatDecimal(mainTotal, 0, 2)}]]
        Object mainTotalObj = context.getVariable("mainTotal");
        if (mainTotalObj != null) {
            double mainTotal = mainTotalObj instanceof Number ? ((Number) mainTotalObj).doubleValue() : 0.0;
            String mainTotalFormatted = String.format("%.2f", mainTotal);
            // Obsługa różnych wariantów formatowania
            result = replaceThymeleafExpressionWithFormat(result, "mainTotal", mainTotalFormatted);
            result = replaceThymeleafExpression(result, "mainTotal", mainTotalFormatted);
        }
        
        // Obsługa wyrażeń z produktami w pętlach - uproszczona wersja
        // Zamiast parsować pełne wyrażenia Thymeleaf, używamy prostych placeholderów
        // Użytkownik powinien używać {{productsTable}} zamiast pętli
        
        logger.debug("Zastąpiono placeholdery w HTML (długość przed: {}, po: {})", html.length(), result.length());
        
        return result;
    }
    
    /**
     * Zastępuje wyrażenie Thymeleaf [[${expression}]] wartością
     */
    private String replaceThymeleafExpression(String html, String expression, String value) {
        if (html == null || expression == null || value == null) {
            return html;
        }
        // Escapuj specjalne znaki w wyrażeniu dla regex (ale zachowaj regex metaznaki jak .)
        String escapedExpression = expression.replaceAll("([\\[\\](){}*+?.^$|\\\\])", "\\\\$1");
        // Zamień [[${expression}]] na wartość
        String pattern = "\\[\\[\\$\\{" + escapedExpression + "\\}\\]\\]";
        return html.replaceAll(pattern, java.util.regex.Matcher.quoteReplacement(value));
    }
    
    /**
     * Zastępuje wyrażenie Thymeleaf z formatowaniem liczb [[${#numbers.formatDecimal(var, 0, 2)}]]
     */
    private String replaceThymeleafExpressionWithFormat(String html, String varName, String formattedValue) {
        if (html == null || varName == null || formattedValue == null) {
            return html;
        }
        // Obsługa różnych wariantów formatowania
        String[] patterns = {
            "\\[\\[\\$\\{#numbers\\.formatDecimal\\(" + varName + ",\\s*0,\\s*2\\)\\}\\]\\]",
            "\\[\\[\\$\\{#numbers\\.formatDecimal\\(" + varName + ",\\s*1,\\s*2\\)\\}\\]\\]",
            "\\[\\[\\$\\{" + varName + "\\}\\]\\]"
        };
        for (String pattern : patterns) {
            html = html.replaceAll(pattern, java.util.regex.Matcher.quoteReplacement(formattedValue));
        }
        return html;
    }
    
    /**
     * Zastępuje pojedynczy placeholder w HTML
     */
    private String replacePlaceholder(String html, String placeholderName, String value) {
        if (html == null || placeholderName == null || value == null) {
            return html;
        }
        String placeholder = "{{" + placeholderName + "}}";
        // Zamień wszystkie wystąpienia (nie tylko pierwsze)
        return html.replace(placeholder, value);
    }
    
    /**
     * Pobiera zmienną z kontekstu Thymeleaf jako String
     */
    private String getVariableAsString(Context context, String key, String defaultValue) {
        Object value = context.getVariable(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
    
    /**
     * Generuje tabelę HTML dla wszystkich produktów
     */
    private String generateAllProductsTable(List<Product> tiles, List<Product> gutters, List<Product> accessories, List<Product> windows) {
        StringBuilder html = new StringBuilder();
        
        if (!tiles.isEmpty()) {
            html.append(generateCategoryTable(tiles, "Dachówki"));
        }
        if (!gutters.isEmpty()) {
            html.append(generateCategoryTable(gutters, "Rynny"));
        }
        if (!windows.isEmpty()) {
            html.append(generateCategoryTable(windows, "Okna"));
        }
        if (!accessories.isEmpty()) {
            html.append(generateCategoryTable(accessories, "Akcesoria"));
        }
        
        if (html.length() == 0) {
            html.append("<p>Brak produktów w ofercie</p>");
        }
        
        return html.toString();
    }
    
    /**
     * Generuje tabelę HTML dla produktów opcjonalnych - tylko sumy dla każdej grupy
     * Grupuje produkty po manufacturer + groupName i pokazuje tylko sumę ceny sprzedaży
     * 
     * @param products Lista produktów opcjonalnych (isMainOption = false)
     * @param categoryName Nazwa kategorii (np. "Dachówki", "Rynny")
     * @return HTML z tabelą sum dla każdej grupy opcjonalnej
     */
    private String generateOptionalGroupsSummaryTable(List<Product> products, String categoryName) {
        logger.debug("🔨 generateOptionalGroupsSummaryTable - kategoria: {}, produkty: {}", categoryName, products != null ? products.size() : 0);
        
        if (products == null || products.isEmpty()) {
            logger.debug("🔨 generateOptionalGroupsSummaryTable - lista produktów jest pusta");
            return "";
        }
        
        // Grupuj produkty po manufacturer + groupName
        Map<String, List<Product>> groupsMap = products.stream()
            .filter(p -> p.getManufacturer() != null && p.getGroupName() != null && 
                         p.getQuantity() != null && p.getQuantity() > 0)
            .collect(Collectors.groupingBy(
                p -> p.getManufacturer() + " - " + p.getGroupName()
            ));
        
        logger.debug("🔨 generateOptionalGroupsSummaryTable - zmapowano {} grup produktowych", groupsMap.size());
        
        if (groupsMap.isEmpty()) {
            logger.warn("⚠️ generateOptionalGroupsSummaryTable - brak grup produktowych po filtrowaniu (manufacturer/groupName/quantity)");
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<h3 style=\"margin-top: 20px; color: #2A2A2A;\">").append(categoryName).append(" - Opcjonalne</h3>\n");
        html.append("<table style=\"width: 100%; border-collapse: collapse; margin: 10px 0;\">\n");
        html.append("  <thead>\n");
        html.append("    <tr style=\"background-color: #FFD700; color: #2A2A2A;\">\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: left;\">Grupa produktów</th>\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right;\">Suma ceny sprzedaży (PLN)</th>\n");
        html.append("    </tr>\n");
        html.append("  </thead>\n");
        html.append("  <tbody>\n");
        
        double totalOptional = 0.0;
        
        for (Map.Entry<String, List<Product>> entry : groupsMap.entrySet()) {
            String groupName = entry.getKey();
            List<Product> groupProducts = entry.getValue();
            
            // Oblicz sumę ceny sprzedaży dla tej grupy
            double groupTotal = 0.0;
            for (Product product : groupProducts) {
                double quantity = product.getQuantity() != null ? product.getQuantity() : 0.0;
                double sellingPrice = product.getSellingPrice() != null ? product.getSellingPrice() 
                    : (product.getRetailPrice() != null ? product.getRetailPrice() 
                    : (product.getPurchasePrice() != null ? product.getPurchasePrice() : 0.0));
                groupTotal += quantity * sellingPrice;
            }
            
            totalOptional += groupTotal;
            
            html.append("    <tr>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">").append(groupName).append("</td>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right; font-weight: 600;\">")
                .append(String.format("%.2f", groupTotal)).append(" PLN</td>\n");
            html.append("    </tr>\n");
        }
        
        html.append("  </tbody>\n");
        html.append("  <tfoot>\n");
        html.append("    <tr style=\"background-color: #f8f9fa; font-weight: 700;\">\n");
        html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right;\">RAZEM ").append(categoryName).append(" - Opcjonalne:</td>\n");
        html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right; color: #2A2A2A;\">")
            .append(String.format("%.2f", totalOptional)).append(" PLN</td>\n");
        html.append("    </tr>\n");
        html.append("  </tfoot>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
    
    /**
     * Generuje tabelę HTML dla danej kategorii produktów
     */
    private String generateCategoryTable(List<Product> products, String categoryName) {
        logger.debug("🔨 generateCategoryTable - kategoria: {}, produkty: {}", categoryName, products != null ? products.size() : 0);
        
        if (products == null || products.isEmpty()) {
            logger.debug("🔨 generateCategoryTable - lista produktów jest pusta dla kategorii: {}", categoryName);
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<h3 style=\"margin-top: 20px; color: #2A2A2A;\">").append(categoryName).append("</h3>\n");
        html.append("<table style=\"width: 100%; border-collapse: collapse; margin: 10px 0;\">\n");
        html.append("  <thead>\n");
        html.append("    <tr style=\"background-color: #FFD700; color: #2A2A2A;\">\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: left;\">Lp.</th>\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: left;\">Nazwa produktu</th>\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: center;\">Ilość</th>\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right;\">Cena jedn. (PLN)</th>\n");
        html.append("      <th style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right;\">Wartość (PLN)</th>\n");
        html.append("    </tr>\n");
        html.append("  </thead>\n");
        html.append("  <tbody>\n");
        
        int index = 1;
        double categoryTotal = 0.0;
        
        for (Product product : products) {
            double quantity = product.getQuantity() != null ? product.getQuantity() : 0.0;
            double unitPrice = product.getSellingPrice() != null ? product.getSellingPrice() 
                            : (product.getRetailPrice() != null ? product.getRetailPrice() 
                            : (product.getPurchasePrice() != null ? product.getPurchasePrice() : 0.0));
            double totalValue = quantity * unitPrice;
            categoryTotal += totalValue;
            
            html.append("    <tr>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">").append(index++).append("</td>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px;\">").append(product.getName() != null ? product.getName() : "Bez nazwy").append("</td>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: center;\">").append(String.format("%.2f", quantity)).append("</td>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right;\">").append(String.format("%.2f", unitPrice)).append("</td>\n");
            html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right; font-weight: 600;\">").append(String.format("%.2f", totalValue)).append("</td>\n");
            html.append("    </tr>\n");
        }
        
        html.append("  </tbody>\n");
        html.append("  <tfoot>\n");
        html.append("    <tr style=\"background-color: #f8f9fa; font-weight: 700;\">\n");
        html.append("      <td colspan=\"4\" style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right;\">RAZEM ").append(categoryName).append(":</td>\n");
        html.append("      <td style=\"border: 1px solid #dee2e6; padding: 8px; text-align: right; color: #2A2A2A;\">").append(String.format("%.2f", categoryTotal)).append(" PLN</td>\n");
        html.append("    </tr>\n");
        html.append("  </tfoot>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
}

