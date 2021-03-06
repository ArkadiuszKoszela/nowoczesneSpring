package pl.koszela.nowoczesnebud.CreateOffer;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Offer;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Model.Tile;
import pl.koszela.nowoczesnebud.Repository.ProductGroupRepository;
import pl.koszela.nowoczesnebud.Repository.ProductTypeRepository;
import pl.koszela.nowoczesnebud.Service.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CreateOffer {

    private final ProductGroupService productGroupService;

    public CreateOffer(@Lazy ProductGroupService productGroupService) {
        this.productGroupService = productGroupService;
    }

    private Chunk getUrlToWebSite() throws IOException, DocumentException {
        Chunk chunk = new Chunk(StaticValuesForOffer.URL + "\n", font());
        chunk.setAnchor(StaticValuesForOffer.URL);
        return chunk;
    }

    public void createOffer(Offer offer) {
        Document document = new Document();

        try {

            PdfWriter.getInstance(document, new FileOutputStream(new File(StaticValuesForOffer.URL_TO_PDF)));

            document.open();
            document.add(createDate());
            document.add(getUrlToWebSite());
            document.add(new Paragraph(StaticValuesForOffer.INFO, font()));
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph(StaticValuesForOffer.INFORMATION_FOR, font()));
            document.add(new Paragraph(
                    "\n" + getTab(
                            40) + "Imię nazwisko: " + offer.getUser().getName() + " " + offer.getUser().getSurname(),
                    font()));
            document.add(new Paragraph("\n" + getTab(40) + "Adres: " + offer.getUser().getAddress().getAddress(),
                    font()));
            document.add(new Paragraph("\n" + getTab(40) + "tel. " + offer.getUser().getTelephoneNumber(), font()));
            document.add(new Paragraph("\n\n" + StaticValuesForOffer.NELSKAMRUBP + "\n\n\n", font()));
            generateMainTile(document);
            document.close();
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    private String getTab(int value) {
        return Stream.generate(() -> " ").limit(value).collect(
                Collectors.joining());
    }

    private void generateMainTile(Document document) throws IOException, DocumentException {
        ProductGroup mainProductType = productGroupService.findMainProductGroup();
        PdfPTable table = new PdfPTable(17);
        table.setTotalWidth(Utilities.millimetersToPoints(180));
        table.setLockedWidth(true);

        table.addCell(getCell(8, mainProductType.getTypeName(), font(10), true));
        table.addCell(getCell(1, "Jm", font(10), true));
        table.addCell(getCell(2, "Ilość", font(10), true));
        table.addCell(getCell(2, "Cena detal", font(10), true));
        table.addCell(getCell(2, "Cena po rabacie", font(10), true));
        table.addCell(getCell(2, "Wartość netto po rabacie", font(10), true));


        for (ProductType productType : mainProductType.getProductTypeList()) {
            table.addCell(getCell(8, StringUtils.capitalize(productType.getName()), font(), false));
            table.addCell(getCell(1, "szt.", font(), false));
            table.addCell(getCell(2, String.valueOf(productType.getQuantity()), font(), false));
            table.addCell(getCell(2, String.valueOf(productType.getDetalPrice()), font(), false));
            table.addCell(getCell(2, String.valueOf(productType.getSellingPrice()), font(), false));
            table.addCell(getCell(2,
                    String.valueOf(
                            BigDecimal.valueOf(productType.getPurchasePrice() * productType.getQuantity()).setScale(2,
                                    RoundingMode.HALF_UP)),
                    font(), false));
        }
        document.add(table);
        document.add(new Paragraph(
                "\n\n" + getTab(100) + "Elementy dachówkowe: " + mainProductType.getTotalPriceAfterDiscount() + " " +
                        "zł\n\n",
                font(12, true)));
        document.add(new Paragraph("Oferta OPCJONALNIE w cenach netto od: \n\n", font(12)));
        document.add(new Paragraph("Najtańszy dach od: " + productGroupService.finCheapestOption(), font(12)));

    }

    private Paragraph createDate() throws IOException, DocumentException {
        Paragraph date = new Paragraph(StaticValuesForOffer.OFFER_DATE, font());
        date.setAlignment(Element.ALIGN_RIGHT);
        return date;
    }

    private PdfPCell getCell(int cm, String value, Font font, boolean doCenter) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(cm);
        cell.setUseAscender(true);
        cell.setUseDescender(true);
        if (doCenter)
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph p = new Paragraph(value, font);
        if (doCenter)
            p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    private Font font(float size, boolean doBold) throws IOException, DocumentException {
        Font font = font(doBold);
        font.setSize(size);
        return font;
    }

    private Font font(float size) throws IOException, DocumentException {
        Font font = font(false);
        font.setSize(size);
        return font;
    }

    private Font font(boolean doBold) throws IOException, DocumentException {
        final BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
        if (doBold)
            return new Font(baseFont, 8, Font.BOLD);
        return new Font(baseFont, 8);
    }

    private Font font() throws IOException, DocumentException {
        final BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
        return new Font(baseFont, 8);
    }
}
