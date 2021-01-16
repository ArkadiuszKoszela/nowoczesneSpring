package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ServiceCsv {

    public List<Tile> readAndSaveTiles (List<MultipartFile> list) throws IOException {
        List<Tile> tilePathList = new ArrayList<>();

        Tile tile = new Tile();
        List<MultipartFile> li = new ArrayList<>(list);
        Iterator<MultipartFile> i = li.iterator();

        while (i.hasNext()) {
            File file = convertMultiPartToFile (i.next());
            List<TypeOfTile> typeOfTileList = Poiji.fromExcel(file, TypeOfTile.class);
            List<Tile> tiles = Poiji.fromExcel(file, Tile.class);
            Optional<Tile> opt = tiles.stream().findFirst();
            if (!opt.isPresent())
                continue;

            tile.setManufacturer(getManufacturer(file.getName()));
            setDiscounts (opt.get(), tile);
            GroupOfTile groupOfTile = new GroupOfTile(getTypeOfTile(file.getName()), typeOfTileList);
            tile.getGroupOfTileList().add(groupOfTile);

            i.remove();
            boolean hasAnyMore = li.stream().anyMatch(e -> getManufacturer(e.getOriginalFilename()).equalsIgnoreCase(getManufacturer(file.getName())));
            if (!hasAnyMore) {
                tilePathList.add(tile);
                tile = new Tile();
            }
        }

        return tilePathList;
    }

    List<Tile> readAndSaveTiles(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Tile> tilePathList = new ArrayList<>();

        Tile tile = new Tile();
        List<File> list = new LinkedList<>(Arrays.asList(files));
        Iterator<File> i = list.iterator();

        while (i.hasNext()) {
            File file = i.next();
            List<TypeOfTile> typeOfTileList = Poiji.fromExcel(file, TypeOfTile.class);
            List<Tile> tiles = Poiji.fromExcel(file, Tile.class);
            Optional<Tile> opt = tiles.stream().findFirst();
            if (!opt.isPresent())
                continue;

            tile.setManufacturer(getManufacturer(file.getName()));
            setDiscounts (opt.get(), tile);
            GroupOfTile groupOfTile = new GroupOfTile(getTypeOfTile(file.getName()), typeOfTileList);
            tile.getGroupOfTileList().add(groupOfTile);

            i.remove();
            boolean hasAnyMore = list.stream().anyMatch(e -> getManufacturer(e.getName()).equalsIgnoreCase(getManufacturer(file.getName())));
            if (!hasAnyMore) {
                tilePathList.add(tile);
                tile = new Tile();
            }
        }

        return tilePathList;
    }

    private void setDiscounts(Tile fromTile, Tile toTile) {
        toTile.setBasicDiscount(fromTile.getBasicDiscount());
        toTile.setPromotionDiscount(fromTile.getPromotionDiscount());
        toTile.setAdditionalDiscount(fromTile.getAdditionalDiscount());
        toTile.setSkontoDiscount(fromTile.getSkontoDiscount());
    }

    private void setDiscounts(Gutter fromTile, Gutter toTile) {
        toTile.setBasicDiscount(fromTile.getBasicDiscount());
        toTile.setPromotionDiscount(fromTile.getPromotionDiscount());
        toTile.setAdditionalDiscount(fromTile.getAdditionalDiscount());
        toTile.setSkontoDiscount(fromTile.getSkontoDiscount());
    }


    List<Accessory> readAndSaveAccessories(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Accessory> accessoryPathList = new ArrayList<>();
        for (File file : files) {
            accessoryPathList = Poiji.fromExcel(file, Accessory.class);
            accessoryPathList.forEach(obj -> obj.setManufacturer(getManufacturer(file.getName())));
        }
        return accessoryPathList;
    }

    List<Gutter> readAndSaveGutters(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Gutter> tilePathList = new ArrayList<>();

        Gutter gutter = new Gutter();
        List<File> list = new LinkedList<>(Arrays.asList(files));
        Iterator<File> i = list.iterator();

        while (i.hasNext()) {
            File file = i.next();
            List<TypeOfTile> typeOfTileList = Poiji.fromExcel(file, TypeOfTile.class);
            List<Gutter> gutterList = Poiji.fromExcel(file, Gutter.class);
            Optional<Gutter> opt = gutterList.stream().findFirst();
            if (!opt.isPresent())
                continue;

            gutter.setManufacturer(getManufacturer(file.getName()));
            setDiscounts (opt.get(), gutter);
            GroupOfTile groupOfTile = new GroupOfTile(getTypeOfTile(file.getName()), typeOfTileList);
            gutter.getGroupOfTileList().add(groupOfTile);

            i.remove();
            boolean hasAnyMore = list.stream().anyMatch(e -> getManufacturer(e.getName()).equalsIgnoreCase(getManufacturer(file.getName())));
            if (!hasAnyMore) {
                tilePathList.add(gutter);
                gutter = new Gutter();
            }
        }

        return tilePathList;
    }

    private String getManufacturer(String url) {
        String fullName = StringUtils.substringBeforeLast(url, ".");
        return StringUtils.substringBeforeLast(fullName, "-").trim();
    }

    private String getTypeOfTile(String url) {
        String fullName = StringUtils.substringBeforeLast(url, ".");
        return StringUtils.substringAfterLast(fullName, "-").trim();
    }

    private File convertMultiPartToFile(MultipartFile file ) throws IOException
    {
        File convFile = new File( file.getOriginalFilename() );
        FileOutputStream fos = new FileOutputStream( convFile );
        fos.write( file.getBytes() );
        fos.close();
        return convFile;
    }
}
