package pl.koszela.nowoczesnebud.Exception;

public class UnauthorizedRefreshTokenException extends RuntimeException {

    public UnauthorizedRefreshTokenException(String message) {
        super(message);
    }
}
