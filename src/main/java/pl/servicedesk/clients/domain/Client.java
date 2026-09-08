package pl.servicedesk.clients.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;
import pl.servicedesk.identity.domain.User;

@Entity
@Getter
@Table(name = "clients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "loyalty_tier", length = 20)
    private String loyaltyTier;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;

    public Client(User user, boolean marketingConsent) {
        this.user = user;
        this.marketingConsent = marketingConsent;
    }

    public void assignLoyaltyTier(String tierName) {
        this.loyaltyTier = tierName;
    }

    public void updateMarketingConsent(boolean consent) {
        this.marketingConsent = consent;
    }
}
