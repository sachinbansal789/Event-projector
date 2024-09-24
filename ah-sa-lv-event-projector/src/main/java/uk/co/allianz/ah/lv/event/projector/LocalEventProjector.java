package uk.co.allianz.ah.lv.event.projector;

import static org.springframework.boot.SpringApplication.run;

public class LocalEventProjector {

    public static void main(String[] args) throws Exception {
        run(EventProjector.class,
                "--spring.profiles.active=local"
        );
    }
}
