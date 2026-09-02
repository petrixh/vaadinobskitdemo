package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByStatus(String status);

    List<CustomerOrder> findByCustomerEmail(String email);
}
