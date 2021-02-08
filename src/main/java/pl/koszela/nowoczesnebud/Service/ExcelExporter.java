package pl.koszela.nowoczesnebud.Service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Model.Tile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelExporter {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private final TilesService tilesService;

    public ExcelExporter(TilesService tilesService) {
        this.tilesService = tilesService;
    }


    private void writeHeaderLine() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(dtf.format(LocalDateTime.now()));

        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);

        createCell(row, 0, "name", style);
        createCell(row, 1, "unitDetalPrice", style);
        createCell(row, 2, "quantityConverter", style);
        createCell(row, 3, "basicDiscount", style);
        createCell(row, 4, "additionalDiscount", style);
        createCell(row, 5, "promotionDiscount", style);
        createCell(row, 6, "skonto", style);
        createCell(row, 7, "mapperName", style);
        createCell(row, 8, "cena zakupu", style);

    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        sheet.autoSizeColumn(columnCount);
        Cell cell = row.createCell(columnCount);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }

    private XSSFWorkbook writeDataLines(Tile tile, ProductGroup productGroup) {
        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        int columnCount = 0;
        for (ProductType productType : productGroup.getProductTypeList()) {
            Row row = sheet.createRow(rowCount++);
            createCell(row, columnCount++, productType.getName(), style);
            createCell(row, columnCount++, productType.getDetalPrice(), style);
            createCell(row, columnCount++, productType.getQuantityConverter(), style);
            createCell(row, columnCount++, productType.getBasicDiscount(), style);
            createCell(row, columnCount++, productType.getAdditionalDiscount(), style);
            createCell(row, columnCount++, productType.getPromotionDiscount(), style);
            createCell(row, columnCount++, productType.getSkontoDiscount(), style);
            createCell(row, columnCount++, productType.getMapperName(), style);
            createCell(row, columnCount++, "", style);
            columnCount = 0;
        }

        return workbook;
    }

    public File export(Tile tile, ProductGroup productGroup) throws IOException {
        writeHeaderLine();

        writeDataLines(tile, productGroup);
        File file = new File(tile.getManufacturer() + "-" + productGroup.getTypeName() + ".xlsx");
        FileOutputStream outputStream = new FileOutputStream(file);

        workbook.write(outputStream);
        workbook.close();

        outputStream.close();

        return file;
    }

    public List<File> getAll() throws IOException {
        List<Tile> tileList = tilesService.getAllTilesOrCreate();
        List<File> fileList = new ArrayList<>();
        for (Tile tile : tileList) {
            for (ProductGroup productGroup : tile.getProductGroupList()) {
                    fileList.add(export(tile, productGroup));
            }
        }
        return fileList;
    }
}
