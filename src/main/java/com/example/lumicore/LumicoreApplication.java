package com.example.lumicore;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
		info = @Info(
				title = "LumiDiary Core API",
				version = "v1",
				description = "LumiDiary Core 서비스 API 명세"
		)
)
@SpringBootApplication
public class LumicoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(LumicoreApplication.class, args);
	}

}
