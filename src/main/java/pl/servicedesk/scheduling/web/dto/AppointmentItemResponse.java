package pl.servicedesk.scheduling.web.dto;

import java.math.BigDecimal;

public record AppointmentItemResponse(
        String serviceCode,
        String serviceName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount) {
}
