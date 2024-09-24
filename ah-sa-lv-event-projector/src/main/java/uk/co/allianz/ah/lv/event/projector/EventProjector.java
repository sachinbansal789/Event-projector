package uk.co.allianz.ah.lv.event.projector;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan({"uk.co.allianz.health", "uk.co.allianz.ah.lv.event.projector",
        "uk.co.ah.encryption", "uk.co.ah.messaging.camel.security", "uk.co.ah.security.service"})

public class EventProjector implements CommandLineRunner {

    public static void main(final String[] args) throws Exception {
        SpringApplication.run(EventProjector.class, args);
    }


    @Override
    public void run(final String... strings) throws Exception { }
}
