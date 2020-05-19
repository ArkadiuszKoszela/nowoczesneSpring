package pl.koszela.nowoczesnebud.Service;

import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Tiles;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static pl.koszela.nowoczesnebud.EndPoint.*;

@Service
public class ServiceCsv {

    private List<Tiles> tilesList;

    public ServiceCsv() {
        tilesList = new ArrayList<>();
    }

    List<Tiles> saveTiles() {
        try {
            readFromCSV(FILE_BOGEN_INNOVO_10_CZERWONA_ANGOBA_URL);
            readFromCSV(FILE_TITANIA_BRAZ_GLAZ_NOBLESSE_URL);
            readFromCSV(FILE_TITANIA_CZARNA_GLAZ_FINESSE_URL);
            readFromCSV(FILE_BOGEN_INNOVO_10_MIEDZIANO_BRAZOWA_ANGOBA_URL);
            readFromCSV(FILE_BOGEN_INNOVO_12_CZERWONA_ANGOBA_URL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tilesList;
    }

    private void readFromCSV(String filePath) throws IOException {
        List<Tiles> tilesPathList = new CsvToBeanBuilder<Tiles>(new FileReader(filePath))
                .withSeparator(';').withType(Tiles.class).build().parse();
        tilesPathList.forEach(tile -> tile.setManufacturer(getManufacturer(filePath)));
        tilesList.addAll(tilesPathList);
    }

    private String getManufacturer(String url) {
        String trim = StringUtils.substringAfterLast(url, "/");
        return StringUtils.substringBeforeLast(trim, ".");
    }
}
