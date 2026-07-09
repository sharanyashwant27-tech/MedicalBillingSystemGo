package com.medicalbilling.repository;

import com.medicalbilling.entity.OnlineOrder;
import com.medicalbilling.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnlineOrderRepository extends JpaRepository<OnlineOrder, Long> {
    Optional<OnlineOrder> findByOrderNumber(String orderNumber);
    List<OnlineOrder> findByStatus(OrderStatus status);
    List<OnlineOrder> findByBranchId(Long branchId);

    @Query("SELECT DISTINCT o FROM OnlineOrder o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.medicine ORDER BY o.orderDate DESC")
    List<OnlineOrder> findAllWithDetails();

    @Query("SELECT DISTINCT o FROM OnlineOrder o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.medicine WHERE o.status = :status ORDER BY o.orderDate DESC")
    List<OnlineOrder> findByStatusWithDetails(@Param("status") OrderStatus status);
}
