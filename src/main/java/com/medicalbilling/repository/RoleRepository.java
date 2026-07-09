package com.medicalbilling.repository;

import com.medicalbilling.entity.Role;
import com.medicalbilling.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}
