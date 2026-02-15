package com.jackforbes.paymentscore;

import org.springframework.boot.SpringApplication;

public class TestPaymentscoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentscoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
