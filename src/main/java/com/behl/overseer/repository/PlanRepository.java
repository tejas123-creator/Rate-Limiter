package com.behl.overseer.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.behl.overseer.entity.Plan;

/**
 * Spring Data repository for plan records. Inheriting JpaRepository provides
 * standard create, read, update, and delete methods without writing SQL.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

}
