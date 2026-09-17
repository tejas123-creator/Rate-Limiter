package com.behl.overseer.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.behl.overseer.entity.User;

/**
 * Spring Data repository for user accounts. Spring derives SQL queries from
 * the method names declared below.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	/** @return whether an account already uses the provided email address. */
	Boolean existsByEmailId(final String emailId);

	/** @return the matching user when an account exists; empty otherwise. */
	Optional<User> findByEmailId(final String emailId);

}
