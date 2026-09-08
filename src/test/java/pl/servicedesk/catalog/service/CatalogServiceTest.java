package pl.servicedesk.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceCategoryRepository;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.catalog.service.CatalogService.NewService;
import pl.servicedesk.catalog.service.CatalogService.ServiceUpdate;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.resources.domain.ResourceType;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ServiceOfferingRepository serviceRepository;

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void createsServiceWithNormalizedPriceAndActiveByDefault() {
        ServiceCategory category = new ServiceCategory("Diagnostics", null, 1);
        given(serviceRepository.existsByCodeIgnoreCase("DIAG-STD")).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(serviceRepository.save(any(ServiceOffering.class))).willAnswer(call -> call.getArgument(0));

        catalogService.createService(new NewService(
                1L, "DIAG-STD", "Standard diagnostics", "  ", new BigDecimal("120.5"), 45, 15, ResourceType.STATION));

        ArgumentCaptor<ServiceOffering> captor = ArgumentCaptor.forClass(ServiceOffering.class);
        verify(serviceRepository).save(captor.capture());
        ServiceOffering saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("DIAG-STD");
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getBasePrice()).isEqualByComparingTo("120.50");
        assertThat(saved.getBasePrice().scale()).isEqualTo(2);
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getRequiredResourceType()).isEqualTo(ResourceType.STATION);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.slotLength()).hasMinutes(60);
    }

    @Test
    void rejectsServiceWithDuplicateCode() {
        given(serviceRepository.existsByCodeIgnoreCase("DIAG-STD")).willReturn(true);

        assertThatThrownBy(() -> catalogService.createService(new NewService(
                1L, "DIAG-STD", "x", null, BigDecimal.TEN, 30, 0, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(serviceRepository, never()).save(any());
    }

    @Test
    void rejectsServiceWhenCategoryIsUnknown() {
        given(serviceRepository.existsByCodeIgnoreCase("DIAG-STD")).willReturn(false);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.createService(new NewService(
                99L, "DIAG-STD", "x", null, BigDecimal.TEN, 30, 0, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsCategoryWithDuplicateName() {
        given(categoryRepository.existsByNameIgnoreCase("Diagnostics")).willReturn(true);

        assertThatThrownBy(() -> catalogService.createCategory("Diagnostics", null, 0))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void reassignsCategoryOnlyWhenItActuallyChanges() {
        ServiceCategory current = categoryWithId(1L);
        ServiceCategory target = categoryWithId(2L);
        ServiceOffering service = new ServiceOffering(
                current, "DIAG-STD", "Standard diagnostics", null, new BigDecimal("100.00"), 30, 0, null);
        given(serviceRepository.findByCode("DIAG-STD")).willReturn(Optional.of(service));
        given(categoryRepository.findById(2L)).willReturn(Optional.of(target));

        catalogService.updateService("DIAG-STD", new ServiceUpdate(
                2L, "Extended diagnostics", "full check", new BigDecimal("150"), 60, 10, ResourceType.BAY));

        assertThat(service.getCategory()).isSameAs(target);
        assertThat(service.getName()).isEqualTo("Extended diagnostics");
        assertThat(service.getBasePrice()).isEqualByComparingTo("150.00");
        assertThat(service.getRequiredResourceType()).isEqualTo(ResourceType.BAY);
    }

    @Test
    void deactivatesServiceByCode() {
        ServiceOffering service = new ServiceOffering(
                new ServiceCategory("Diagnostics", null, 0), "DIAG-STD", "x", null, BigDecimal.TEN, 30, 0, null);
        given(serviceRepository.findByCode("DIAG-STD")).willReturn(Optional.of(service));

        catalogService.changeServiceActivation("DIAG-STD", false);

        assertThat(service.isActive()).isFalse();
    }

    private static ServiceCategory categoryWithId(long id) {
        ServiceCategory category = mock(ServiceCategory.class);
        lenient().when(category.getId()).thenReturn(id);
        return category;
    }
}
