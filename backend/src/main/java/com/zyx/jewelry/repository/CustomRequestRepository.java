package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.CustomRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomRequestRepository extends JpaRepository<CustomRequest, Long> {

    List<CustomRequest> findByUserIdOrderByIdDesc(Long userId);
}
