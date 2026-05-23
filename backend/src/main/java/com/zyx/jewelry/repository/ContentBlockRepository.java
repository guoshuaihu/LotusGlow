package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.ContentBlock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {

    Optional<ContentBlock> findByBlockKey(String blockKey);

    List<ContentBlock> findAllByOrderByIdAsc();
}
