package pl.servicedesk.resources.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;

@Entity
@Getter
@Table(name = "resources")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookableResource extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType type;

    @Column(nullable = false)
    private boolean active;

    public BookableResource(String name, ResourceType type) {
        this.name = name;
        this.type = type;
        this.active = true;
    }

    public void update(String name, ResourceType type) {
        this.name = name;
        this.type = type;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
