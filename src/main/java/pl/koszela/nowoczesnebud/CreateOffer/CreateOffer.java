package pl.koszela.nowoczesnebud.CreateOffer;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.TileToOffer;
import pl.koszela.nowoczesnebud.Repository.CommercialOfferRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class CreateOffer {

    private Chunk getUrlToWebSite() throws IOException, DocumentException {
        Chunk chunk = new Chunk(StaticValuesForOffer.URL +"\n", font());
        chunk.setAnchor(StaticValuesForOffer.URL);
        return chunk;
    }

    private CommercialOfferRepository commercialOfferRepository;


    public CreateOffer(CommercialOfferRepository commercialOfferRepository) {
        this.commercialOfferRepository = commercialOfferRepository;
    }

    public void createOffer(long user) {

        CommercialOffer commercialOfferByUserId = commercialOfferRepository.findByUserIdEquals(user);
        Document document = new Document();

        try {

            PdfWriter.getInstance(document, new FileOutputStream(new File(StaticValuesForOffer.URL_TO_PDF)));

            document.open();
            document.add(createDate ());
            document.add(getUrlToWebSite ());
            document.add(new Paragraph(StaticValuesForOffer.INFO, font()));
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph(StaticValuesForOffer.INFORMATION_FOR + commercialOfferByUserId.getUser().getName(), font()));
            document.add(new Paragraph("\n\n" + StaticValuesForOffer.NELSKAMRUBP + "\n\n\n", font()));
            document.add(generateTilesTable(commercialOfferByUserId));
            document.close();
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    private PdfPTable generateTilesTable(CommercialOffer commercialOfferByUserId) throws IOException, DocumentException {
        PdfPTable table = new PdfPTable(10);
        table.setTotalWidth(Utilities.millimetersToPoints(180));
        table.setLockedWidth(true);

        List<TileToOffer> tiles = commercialOfferByUserId.getTileToOffer();
        table.addCell(getCell(4, "Nazwa", font(11)));
        table.addCell(getCell(3,"Ilość", font(11)));
        table.addCell(getCell(3,"Cena detaliczna", font(11)));
        for (int i = 0; i < tiles.size(); i++) {
            String currentManufacturer = tiles.get(i).getManufacturer();
            if (i <= 0 || !currentManufacturer.equalsIgnoreCase(tiles.get(i -1).getManufacturer())) {
                PdfPCell cell = getCell(10, WordUtils.capitalizeFully(currentManufacturer), font(11));
                cell.setBackgroundColor(BaseColor.GREEN);
                table.addCell(cell);
            }
            generateManufacturer(table, tiles, i);
        }
        return table;
    }

    private Paragraph createDate () throws IOException, DocumentException {
        Paragraph date = new Paragraph(StaticValuesForOffer.OFFER_DATE, font());
        date.setAlignment(Element.ALIGN_RIGHT);
        return date;
    }

    private void generateManufacturer(PdfPTable table, List<TileToOffer> tileToOfferList, int i) throws IOException, DocumentException {
        table.addCell(getCell(4, tileToOfferList.get(i).getName(), font()));
        table.addCell(getCell(3, tileToOfferList.get(i).getQuantity() + " szt.", font()));
        table.addCell(getCell(3, tileToOfferList.get(i).getUnitDetalPrice() + " zł", font()));
    }

    private PdfPCell getCell(int cm, String value, Font font) throws IOException, DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(cm);
        cell.setUseAscender(true);
        cell.setUseDescender(true);
        Paragraph p = new Paragraph(value, font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    private Font font(float size) throws IOException, DocumentException {
        Font font = font();
        font.setSize(size);
        return font;
    }

    private Font font() throws IOException, DocumentException {
        final BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
        return new Font(baseFont, 8);
    }
}
