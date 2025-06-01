package my.code.transactionproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransactionProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionProxyApplication.class, args);
    }
}
