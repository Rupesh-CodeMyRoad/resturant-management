package com.rupjava.restaurantmanagement.scheduler;

import com.rupjava.restaurantmanagement.model.Reservation;
import com.rupjava.restaurantmanagement.model.ReservationLog;
import com.rupjava.restaurantmanagement.model.RestaurantTable;
import com.rupjava.restaurantmanagement.repository.ReservationLogRepository;
import com.rupjava.restaurantmanagement.repository.ReservationRepository;
import com.rupjava.restaurantmanagement.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationLogRepository reservationLogRepository;
    private final RestaurantTableRepository tableRepository;

    @Scheduled(fixedRate = 60000) // Runs every minute
    public void cleanUpExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findByEndTimeBefore(now);

        for (Reservation reservation : expiredReservations) {
            // Log the deletion
            logReservationDeletion(reservation);

            // Free the table
            RestaurantTable table = reservation.getRestaurantTable();
            table.setOccupied(false);
            tableRepository.save(table);

            // Remove the reservation
            reservationRepository.delete(reservation);
        }
    }

    private void logReservationDeletion(Reservation reservation) {
        ReservationLog log = ReservationLog.builder()
                .customerName(reservation.getCustomerName())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .numberOfPeople(reservation.getNumberOfPeople())
                .restaurantTable(reservation.getRestaurantTable())
                .status("DELETED_BY_SYSTEM")
                .statusChangedTime(LocalDateTime.now())
                .build();
        reservationLogRepository.save(log);
    }
}
