package com.behl.overseer.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@ToString
@Table(name = "user_plan_mappings")
public class UserPlanMapping {

	@Id
	@Setter(AccessLevel.NONE)
	@Column(name = "id", nullable = false, unique = true)
	// Primary key for this plan-assignment history record.
	private UUID id;

	@Column(name = "user_id", nullable = true)
	// Foreign-key value identifying the assigned user.
	private UUID userId;

	@Setter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "user_id", nullable = true, insertable = false, updatable = false)
	// Read-only JPA relationship used to access user details from this mapping.
	private User user;

	@Column(name = "plan_id", nullable = true)
	// Foreign-key value identifying the selected plan.
	private UUID planId;

	@Setter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "plan_id", nullable = true, insertable = false, updatable = false)
	// Read-only JPA relationship used to obtain the plan's hourly limit.
	private Plan plan;

	@Column(name = "is_active", nullable = false)
	// Only one mapping per user should be true: their currently active plan.
	private Boolean isActive;

	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false)
	// UTC time when this assignment was created.
	private LocalDateTime createdAt;

	@Setter(AccessLevel.NONE)
	@Column(name = "updated_at", nullable = false)
	// UTC time when this assignment was last changed.
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		// Initialise IDs, active state, and audit times before INSERT.
		this.id = UUID.randomUUID();
		this.isActive = Boolean.TRUE;
		this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
		this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
	}

	@PreUpdate
	void onUpdate() {
		// Refresh the audit time before UPDATE.
		this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
	}

}
