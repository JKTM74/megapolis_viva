package com.megapolis.viva.jpa.repositories;

import com.megapolis.viva.jpa.models.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    @Query(value = "Select max(date_time) from orders WHERE city = :city", nativeQuery = true)
    LocalDateTime findMaximumDateByCityNative(@Param("city") String city);

    @Query(value = "select s from Orders s where s.taskId is null")
    List<Orders> getOrdersWithTaskIdIsNull();
}
