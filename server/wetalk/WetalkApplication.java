package com.wetalk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication
@MapperScan("com.wetalk.mapper")
public class WetalkApplication {

	public static void main(String[] args) {
		SpringApplication.run(WetalkApplication.class, args);
	}

}
