package com.msa.account.global.domain.repository;

import com.msa.account.global.domain.entity.CommonOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommonOptionRepository extends JpaRepository<CommonOption, Long> {
    @Query("SELECT co FROM CommonOption co JOIN FETCH co.goldHarry gh WHERE gh.goldHarryId = :goldHarryId")
    List<CommonOption> findAllWithGoldHarryByGoldHarryId(@Param("goldHarryId") Long goldHarryId);

}
