package com.behl.overseer.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "plans")
public class Plan {

	@Id
	@Column(name = "id", nullable = false, unique = true)
	// Primary key stored in the plans table.
	private UUID id;

	@Column(name = "name", nullable = false, unique = true)
	// Human-readable plan name.
	private String name;

	@Column(name = "limit_per_hour", nullable = false, unique = true)
	// Hourly quota used to create a user's Bucket4j bucket.
	private Integer limitPerHour;

	@Column(name = "created_at", nullable = false)
	// UTC creation timestamp assigned before the entity is first inserted.
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	// UTC timestamp refreshed whenever this entity is updated.
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		// JPA calls this lifecycle hook immediately before INSERT.
		this.id = UUID.randomUUID();
		this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
		this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
	}

	@PreUpdate
	void onUpdate() {
		// JPA calls this lifecycle hook immediately before UPDATE.
		this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
	}

}
