package pl.servicedesk.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.domain.ResourceType;
import pl.servicedesk.resources.repository.BookableResourceRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private BookableResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void createsActiveResourceWhenNameIsAvailable() {
        given(resourceRepository.existsByNameIgnoreCase("Room 1")).willReturn(false);
        given(resourceRepository.save(any(BookableResource.class))).willAnswer(call -> call.getArgument(0));

        resourceService.create("  Room 1  ", ResourceType.ROOM);

        ArgumentCaptor<BookableResource> captor = ArgumentCaptor.forClass(BookableResource.class);
        verify(resourceRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Room 1");
        assertThat(captor.getValue().getType()).isEqualTo(ResourceType.ROOM);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void rejectsCreationWhenNameAlreadyExists() {
        given(resourceRepository.existsByNameIgnoreCase("Room 1")).willReturn(true);

        assertThatThrownBy(() -> resourceService.create("Room 1", ResourceType.ROOM))
                .isInstanceOf(DuplicateResourceException.class);

        verify(resourceRepository, never()).save(any());
    }

    @Test
    void rejectsRenameToNameOwnedByAnotherResource() {
        BookableResource resource = new BookableResource("Room 1", ResourceType.ROOM);
        given(resourceRepository.findById(7L)).willReturn(Optional.of(resource));
        given(resourceRepository.existsByNameIgnoreCase("Room 2")).willReturn(true);

        assertThatThrownBy(() -> resourceService.update(7L, "Room 2", ResourceType.ROOM))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void allowsUpdateThatKeepsTheSameName() {
        BookableResource resource = new BookableResource("Room 1", ResourceType.ROOM);
        given(resourceRepository.findById(7L)).willReturn(Optional.of(resource));

        resourceService.update(7L, "Room 1", ResourceType.STATION);

        assertThat(resource.getType()).isEqualTo(ResourceType.STATION);
    }

    @Test
    void throwsWhenResourceDoesNotExist() {
        given(resourceRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatesResource() {
        BookableResource resource = new BookableResource("Room 1", ResourceType.ROOM);
        given(resourceRepository.findById(7L)).willReturn(Optional.of(resource));

        resourceService.changeActivation(7L, false);

        assertThat(resource.isActive()).isFalse();
    }
}
