package pl.servicedesk.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.clients.domain.Client;

public interface AppointmentPricer {

    AppointmentPrice price(Client client, List<LineItem> items, Instant slotStart);

    record LineItem(ServiceOffering service, int quantity) {
    }

    record AppointmentPrice(
            BigDecimal netAmount,
            BigDecimal surchargeAmount,
            BigDecimal discountPercentage,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String loyaltyTier) {
    }
}
