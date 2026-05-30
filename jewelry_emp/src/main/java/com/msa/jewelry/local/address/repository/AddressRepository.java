package com.msa.jewelry.local.address.repository;

import com.msa.jewelry.local.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
