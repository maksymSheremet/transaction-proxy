package my.code.transactionproxy.exception;

public class TransactionDeserializationException extends RuntimeException {
    public TransactionDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
