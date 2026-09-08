package pl.servicedesk.scheduling.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AppointmentResponse(
        Long id,
        String status,
        Instant scheduledStart,
        Instant scheduledEnd,
        Long clientId,
        String clientName,
        Long employeeId,
        String employeeName,
        String resourceName,
        BigDecimal netAmount,
        BigDecimal surchargeAmount,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String loyaltyTierAtBooking,
        String cancellationReason,
        List<AppointmentItemResponse> items) {
}
