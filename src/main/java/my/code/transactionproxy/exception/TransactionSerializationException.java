package my.code.transactionproxy.exception;

public class TransactionSerializationException extends RuntimeException {
    public TransactionSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
