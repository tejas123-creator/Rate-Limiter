package com.behl.overseer.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

	@Id
	@Setter(AccessLevel.NONE)
	@Column(name = "id", nullable = false, unique = true)
	// Generated primary key; no public setter prevents callers from changing it.
	private UUID id;

	@Column(name = "email_id", nullable = false)
	// Unique account/login email stored in the users table.
	private String emailId;

	@Column(name = "password", nullable = false)
	// BCrypt hash, never the plain-text password.
	private String password;

	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false)
	// UTC timestamp assigned when the user is inserted.
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		// Generate values that must exist before JPA writes the new row.
		this.id = UUID.randomUUID();
		this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
	}

}
