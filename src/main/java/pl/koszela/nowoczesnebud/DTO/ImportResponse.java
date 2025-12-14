package pl.koszela.nowoczesnebud.DTO;

import java.util.List;

/**
 * DTO z odpowiedzią importu zawierającą informacje o każdym przetworzonym pliku
 */
public class ImportResponse {
    private int totalFiles;
    private int successfulFiles;
    private int failedFiles;
    private List<ImportFileResult> fileResults;
    private List<pl.koszela.nowoczesnebud.Model.Product> allProducts;

    public ImportResponse() {
    }

    public ImportResponse(int totalFiles, int successfulFiles, int failedFiles, 
                         List<ImportFileResult> fileResults) {
        this.totalFiles = totalFiles;
        this.successfulFiles = successfulFiles;
        this.failedFiles = failedFiles;
        this.fileResults = fileResults;
    }

    // Getters and Setters
    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getSuccessfulFiles() {
        return successfulFiles;
    }

    public void setSuccessfulFiles(int successfulFiles) {
        this.successfulFiles = successfulFiles;
    }

    public int getFailedFiles() {
        return failedFiles;
    }

    public void setFailedFiles(int failedFiles) {
        this.failedFiles = failedFiles;
    }

    public List<ImportFileResult> getFileResults() {
        return fileResults;
    }

    public void setFileResults(List<ImportFileResult> fileResults) {
        this.fileResults = fileResults;
    }

    public List<pl.koszela.nowoczesnebud.Model.Product> getAllProducts() {
        return allProducts;
    }

    public void setAllProducts(List<pl.koszela.nowoczesnebud.Model.Product> allProducts) {
        this.allProducts = allProducts;
    }
}

