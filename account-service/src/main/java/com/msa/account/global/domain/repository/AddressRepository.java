package com.msa.account.global.domain.repository;

import com.msa.account.global.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
