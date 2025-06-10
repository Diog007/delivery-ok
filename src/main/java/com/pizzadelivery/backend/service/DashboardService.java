package com.pizzadelivery.backend.service;

import com.pizzadelivery.backend.dto.DashboardDtos;
import com.pizzadelivery.backend.entity.Order;
import com.pizzadelivery.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;

    public DashboardDtos.DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();

        // Time ranges
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime monthStart = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();

        // Orders count
        long todayOrders = orderRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long weeklyOrders = orderRepository.countByCreatedAtBetween(weekStart, todayEnd);
        long monthlyOrders = orderRepository.countByCreatedAtBetween(monthStart, todayEnd);

        // Pending orders
        List<Order> allOrders = orderRepository.findAll();
        long pendingOrders = allOrders.stream()
                .filter(o -> List.of("received", "preparing", "out_for_delivery").contains(o.getStatus()))
                .count();

        // Revenue calculation
        double todayRevenue = getRevenueForPeriod(todayStart, todayEnd);
        double weeklyRevenue = getRevenueForPeriod(weekStart, todayEnd);
        double monthlyRevenue = getRevenueForPeriod(monthStart, todayEnd);

        DashboardDtos.Revenue revenue = new DashboardDtos.Revenue(todayRevenue, weeklyRevenue, monthlyRevenue);
        return new DashboardDtos.DashboardStats(todayOrders, weeklyOrders, monthlyOrders, pendingOrders, revenue);
    }

    private double getRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByCreatedAtBetween(start, end).stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }
}
