package pard.server.com.nadri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NadriApplication {

    public static void main(String[] args) {
        SpringApplication.run(NadriApplication.class, args);
    }

}
