package pl.servicedesk.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.servicedesk.clients.domain.Client;

@Component
class FlatRateAppointmentPricer implements AppointmentPricer {

    @Override
    public AppointmentPrice price(Client client, List<LineItem> items, Instant slotStart) {
        BigDecimal net = items.stream()
                .map(item -> item.service().getBasePrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new AppointmentPrice(net, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), net, null);
    }
}
