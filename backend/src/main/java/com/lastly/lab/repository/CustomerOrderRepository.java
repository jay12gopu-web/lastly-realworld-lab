package com.lastly.lab.repository;

import com.lastly.lab.domain.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {
    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);
}
