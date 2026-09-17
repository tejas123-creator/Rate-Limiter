package com.behl.overseer.utility;

import org.springframework.stereotype.Component;

import com.behl.overseer.dto.JokeResponseDto;

import net.datafaker.Faker;
import net.datafaker.providers.entertainment.Joke;

/**
 * Utility class for generating random jokes.
 */
@Component
public class JokeGenerator {

	// Datafaker provider used only to create a harmless sample response.
	private final Joke joke;

	/** Constructs the reusable Datafaker joke provider once when Spring creates this component. */
	public JokeGenerator() {
		this.joke = new Faker().joke();
	}

	/**
	 * Generates a random joke.
	 * 
	 * @return JokeResponseDto containing the generated joke
	 */
	public JokeResponseDto generate() {
		// Ask Datafaker for a random pun, then expose it through the API response DTO.
		final var pun = joke.pun();
		return JokeResponseDto.builder().joke(pun).build();
	}

}
