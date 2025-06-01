package my.code.transactionproxy.exception;

import lombok.Getter;

@Getter
public class TransactionFetchException extends RuntimeException {
    private final String errorBody;

    public TransactionFetchException(String message, String errorBody) {
        super(message);
        this.errorBody = errorBody;
    }

}