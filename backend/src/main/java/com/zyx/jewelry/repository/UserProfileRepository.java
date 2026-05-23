package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByOpenId(String openId);

    Optional<UserProfile> findByUnionId(String unionId);
}
