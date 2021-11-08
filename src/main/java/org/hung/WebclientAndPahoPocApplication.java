package org.hung;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebclientAndPahoPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebclientAndPahoPocApplication.class, args);
	}

}
