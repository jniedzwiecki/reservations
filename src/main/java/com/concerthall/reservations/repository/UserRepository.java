package com.concerthall.reservations.repository;

import com.concerthall.reservations.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedVenues")
    List<User> findAllWithVenues();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.assignedVenues WHERE u.id = :id")
    Optional<User> findByIdWithVenues(@Param("id") UUID id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.assignedVenues WHERE u.email = :email")
    Optional<User> findByEmailWithVenues(@Param("email") String email);
}
