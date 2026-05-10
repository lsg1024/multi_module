package com.msa.jewelry.account.internal.global.domain.repository;

import com.msa.jewelry.account.internal.global.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
