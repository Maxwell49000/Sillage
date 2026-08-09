package com.springai.chatspringai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * Point d'entrée de l'application Spring Boot.
 *
 * Classe minimale qui démarre le contexte Spring via `SpringApplication.run`.
 */
public class ChatspringaiApplication {

	// Méthode main lancée par la JVM pour démarrer l'application
	public static void main(String[] args) {
		SpringApplication.run(ChatspringaiApplication.class, args);
	}

}
