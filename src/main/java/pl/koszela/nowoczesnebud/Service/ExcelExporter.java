package pl.koszela.nowoczesnebud.Service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.GroupOfTile;
import pl.koszela.nowoczesnebud.Model.Tile;
import pl.koszela.nowoczesnebud.Model.TypeOfTile;
import pl.koszela.nowoczesnebud.Repository.TileRepository;

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

    private XSSFWorkbook writeDataLines(Tile tile, GroupOfTile groupOfTile) {
        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        int columnCount = 0;
        for (TypeOfTile typeOfTile: groupOfTile.getTypeOfTileList()) {
            Row row = sheet.createRow(rowCount++);
            createCell(row, columnCount++, typeOfTile.getName(), style);
            createCell(row, columnCount++, typeOfTile.getUnitDetalPrice(), style);
            createCell(row, columnCount++, typeOfTile.getQuantityConverter(), style);
            createCell(row, columnCount++, tile.getBasicDiscount(), style);
            createCell(row, columnCount++, tile.getAdditionalDiscount(), style);
            createCell(row, columnCount++, tile.getPromotionDiscount(), style);
            createCell(row, columnCount++, tile.getSkontoDiscount(), style);
            createCell(row, columnCount++, typeOfTile.getMapperName(), style);
            createCell(row, columnCount++, "", style);
            columnCount = 0;
        }

        return workbook;
    }

    public File export(Tile tile, GroupOfTile groupOfTile) throws IOException {
        writeHeaderLine();

        writeDataLines(tile, groupOfTile);
        File file = new File(tile.getManufacturer() + "-" + groupOfTile.getTypeOfTileName() + ".xlsx");
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
            for (GroupOfTile groupOfTile : tile.getGroupOfTileList()) {
                    fileList.add(export(tile, groupOfTile));
            }
        }
        return fileList;
    }
}
