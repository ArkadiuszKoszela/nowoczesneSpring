package pl.koszela.nowoczesnebud.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CsvImporter<C> {

    List<C> readAndSaveTiles (List<MultipartFile> list) throws IOException;
}
