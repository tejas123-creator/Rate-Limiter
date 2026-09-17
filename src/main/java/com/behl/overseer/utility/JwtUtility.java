package com.behl.overseer.utility;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.behl.overseer.configuration.TokenConfigurationProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;

/**
 * Utility class for JWT (JSON Web Token) operations, responsible for handling
 * JWT generation, signature verification, and extracting required claims from
 * JWT tokens. It interacts with the application's token configuration
 * properties to ensure correct token creation and validation.
 * 
 * @see com.behl.overseer.configuration.TokenConfigurationProperties
 * @see com.behl.overseer.filter.JwtAuthenticationFilter
 */
@Component
@EnableConfigurationProperties(TokenConfigurationProperties.class)
public class JwtUtility {
	
	// Prefix removed when callers pass a complete Authorization-header value.
	private static final String BEARER_PREFIX = "Bearer ";

	// Token issuer; validation rejects tokens created for another application.
	private final String issuer;
	// Holds the Base64 signing key and token lifespan from application.yml.
	private final TokenConfigurationProperties tokenConfigurationProperties;
	
	public JwtUtility(@Value("${spring.application.name}") final String issuer,
			final TokenConfigurationProperties tokenConfigurationProperties) {
		this.issuer = issuer;
		this.tokenConfigurationProperties = tokenConfigurationProperties;
	}

	/**
	 * Generates an access token corresponding to provided user-id based on
	 * configured settings. The generated access token can be used to perform tasks
	 * on behalf of the user on subsequent HTTP calls to the application until it
	 * expires.
	 * 
	 * @param userId The userId against which an access token is to be generated.
	 * @throws IllegalArgumentException if provided argument is <code>null</code>.
	 * @return The generated JWT access token.
	 */
	public String generateAccessToken(@NonNull final UUID userId) {
		// Store the user UUID as the token audience so the filter can identify the caller later.
		final var audience = String.valueOf(userId);
		
		final var accessTokenValidity = tokenConfigurationProperties.getValidity();
		// Convert configuration in minutes to the milliseconds expected by java.util.Date.
		final var expiration = TimeUnit.MINUTES.toMillis(accessTokenValidity);
		final var currentTimestamp = new Date(System.currentTimeMillis());
		final var expirationTimestamp = new Date(System.currentTimeMillis() + expiration);
		
		final var encodedSecretKey = tokenConfigurationProperties.getSecretKey();
		final var secretKey = getSecretKey(encodedSecretKey);
		
		// Sign an HS256 token containing issuer, issue time, expiry, and authenticated user's UUID.
		return Jwts.builder()
				.issuer(issuer)
				.issuedAt(currentTimestamp)
				.expiration(expirationTimestamp)
				.audience().add(audience)
				.and()
				.signWith(secretKey, Jwts.SIG.HS256)
				.compact();
	}
	
	/**
	 * Extracts user's ID from a given JWT token signifying an authenticated
	 * user.
	 * 
	 * @param token The JWT token from which to extract the user's ID.
	 * @throws IllegalArgumentException if provided argument is <code>null</code>.
	 * @return The authenticated user's unique identifier (ID) in UUID format.
	 */
	public UUID getUserId(@NonNull final String token) {
		// Extract the audience claim and convert the stored UUID text back to a UUID object.
		final var audience = extractClaim(token, Claims::getAudience).iterator().next();
		return UUID.fromString(audience);
	}

	/**
	 * Extracts a specific claim from the provided JWT token. This method verifies
	 * the token's issuer and signature before extracting the claim.
	 * 
	 * @param token JWT token from which the desired claim is to be extracted.
	 * @param claimsResolver function of {@link Claims} to execute. example: {@code Claims::getId}.
	 * @throws IllegalArgumentException if any provided argument is <code>null</code>
	 * @return The extracted claim value from the JWT token.
	 */
	private <T> T extractClaim(@NonNull final String token, @NonNull final Function<Claims, T> claimsResolver) {
		// Recreate the signing key and remove an optional Bearer prefix before validation.
		final var encodedSecretKey = tokenConfigurationProperties.getSecretKey();
		final var secretKey = getSecretKey(encodedSecretKey);
		final var sanitizedToken = token.replace(BEARER_PREFIX, StringUtils.EMPTY);
		// Verify issuer, signature, and expiration before exposing any token data.
		final var claims = Jwts.parser()
				.requireIssuer(issuer)
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(sanitizedToken)
				.getPayload();
		return claimsResolver.apply(claims);
	}

	/**
	 * Constructs an instance of {@link SecretKey} from the provided Base64-encoded
	 * secret key string.
	 * 
	 * @param encodedKey The Base64-encoded secret key string.
	 * @throws IllegalArgumentException if encodedKey is <code>null</code>
	 * @return A {@link SecretKey} instance for JWT signing and verification.
	 */
	private SecretKey getSecretKey(@NonNull final String encodedKey) {
		// Decode the Base64 configuration value into key material suitable for HS256 signing.
		final var decodedKey = Decoders.BASE64.decode(encodedKey);
		return Keys.hmacShaKeyFor(decodedKey);
	}

}
