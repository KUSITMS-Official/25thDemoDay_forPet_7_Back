package com.kusitms.forpet.repository;

import com.kusitms.forpet.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRep extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    User findByUserId(Long user_id);
    User findByNickname(String nickname);
    Boolean existsByEmail(String email);
}
