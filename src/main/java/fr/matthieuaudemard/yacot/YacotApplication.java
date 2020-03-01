package fr.matthieuaudemard.yacot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YacotApplication {

	public static void main(String[] args) {
		SpringApplication.run(YacotApplication.class, args);
	}

}
