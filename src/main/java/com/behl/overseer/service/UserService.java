package com.behl.overseer.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.behl.overseer.dto.TokenSuccessResponseDto;
import com.behl.overseer.dto.UserCreationRequestDto;
import com.behl.overseer.dto.UserLoginRequestDto;
import com.behl.overseer.entity.User;
import com.behl.overseer.entity.UserPlanMapping;
import com.behl.overseer.exception.AccountAlreadyExistsException;
import com.behl.overseer.exception.InvalidLoginCredentialsException;
import com.behl.overseer.exception.InvalidPlanException;
import com.behl.overseer.repository.PlanRepository;
import com.behl.overseer.repository.UserPlanMappingRepository;
import com.behl.overseer.repository.UserRepository;
import com.behl.overseer.utility.JwtUtility;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	// Generates the signed JWT returned after a successful login.
	private final JwtUtility jwtUtility;
	// Persists user accounts and searches for users by email.
	private final UserRepository userRepository;
	// Checks that a requested subscription plan exists.
	private final PlanRepository planRepository;
	// Hashes new passwords and safely compares login passwords with stored hashes.
	private final PasswordEncoder passwordEncoder;
	// Persists the relationship between a user and their chosen plan.
	private final UserPlanMappingRepository userPlanMappingRepository;

	/**
	 * Creates a new user account in the system corresponding to provided
	 * subscription plan in the request.
	 *
	 * @param userCreationRequest containing user account details.
	 * @throws IllegalArgumentException if provided argument is <code>null</code>.
	 * @throws AccountAlreadyExistsException If an account with the provided email-id already exists.
	 * @throws InvalidPlanException If the provided plan ID is invalid.
	 */
	@Transactional
	public void create(@NonNull final UserCreationRequestDto userCreationRequest) {
		// An email is the account's unique login identifier.
		final var emailId = userCreationRequest.getEmailId();
		// Reject duplicates before attempting to create the account.
		final var userAccountExistsWithEmailId = userRepository.existsByEmailId(emailId);
		if (Boolean.TRUE.equals(userAccountExistsWithEmailId)) {
			throw new AccountAlreadyExistsException("Account with provided email-id already exists");
		}

		// A user can only be attached to one of the plans already stored in MySQL.
		final var planId = userCreationRequest.getPlanId();
		final var isPlanIdValid = planRepository.existsById(planId);
		if (Boolean.FALSE.equals(isPlanIdValid)) {
			throw new InvalidPlanException("No plan exists in the system with provided-id");
		}

		// Create the user entity; its UUID and creation time are set automatically by @PrePersist.
		final var user = new User();
		// Never store the raw password in the database.
		final var encodedPassword = passwordEncoder.encode(userCreationRequest.getPassword());
		user.setEmailId(emailId);
		user.setPassword(encodedPassword);
		// Save first so the generated user ID is available for the plan mapping.
		final var savedUser = userRepository.save(user);

		// Store the user's selected plan separately, allowing plan changes to retain history.
		final var userPlanMapping = new UserPlanMapping();
		userPlanMapping.setUserId(savedUser.getId());
		userPlanMapping.setPlanId(planId);
		userPlanMappingRepository.save(userPlanMapping);
	}

	/**
	 * Validates user login credentials and generates an access token on successful
	 * authentication.
	 *
	 * @param userLoginRequest The request object containing user login credentials.
	 * @return The access token response containing the generated access token.
	 * @throws IllegalArgumentException if provided argument is <code>null</code>.
	 * @throws InvalidLoginCredentialsException If the provided login credentials are invalid.
	 */
	public TokenSuccessResponseDto login(@NonNull final UserLoginRequestDto userLoginRequest) {
		// Look up the account by its login email; return 401 if it does not exist.
		final var user = userRepository.findByEmailId(userLoginRequest.getEmailId())
				.orElseThrow(InvalidLoginCredentialsException::new);
		
		// BCrypt compares a raw login password with the one-way hash stored in MySQL.
		final var encodedPassword = user.getPassword();
		final var plainTextPassword = userLoginRequest.getPassword();
		final var isCorrectPassword = passwordEncoder.matches(plainTextPassword, encodedPassword);
		if (Boolean.FALSE.equals(isCorrectPassword)) {
			throw new InvalidLoginCredentialsException();
		}
		
		// The token contains the user's ID and is used instead of a server-side session.
		final var accessToken = jwtUtility.generateAccessToken(user.getId());
		return TokenSuccessResponseDto.builder().accessToken(accessToken).build();
	}

}
