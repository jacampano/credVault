package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.dto.admin.GroupForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AdminGroupService adminGroupService;

    @Test
    void createGroupSavesWhenValid() {
        GroupForm form = new GroupForm();
        form.setName(" DEVOPS ");
        form.setDescription(" Grupo devops ");
        when(groupRepository.existsByNameIgnoreCase("DEVOPS")).thenReturn(false);

        adminGroupService.createGroup(form);

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("DEVOPS");
        assertThat(captor.getValue().getDescription()).isEqualTo("Grupo devops");
    }

    @Test
    void createGroupFailsWhenDuplicated() {
        GroupForm form = new GroupForm();
        form.setName("DEVOPS");
        when(groupRepository.existsByNameIgnoreCase("DEVOPS")).thenReturn(true);

        assertThatThrownBy(() -> adminGroupService.createGroup(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un grupo");
    }

    @Test
    void deleteGroupFailsWhenHasMembers() {
        Group group = new Group();
        group.setId(3L);
        group.setName("DEVOPS");
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(appUserRepository.countMembersByGroup("DEVOPS")).thenReturn(2L);

        assertThatThrownBy(() -> adminGroupService.deleteGroup(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tiene miembros");
    }

    @Test
    void deleteGroupRemovesWhenNoMembers() {
        Group group = new Group();
        group.setId(3L);
        group.setName("DEVOPS");
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(appUserRepository.countMembersByGroup("DEVOPS")).thenReturn(0L);

        adminGroupService.deleteGroup(3L);

        verify(groupRepository).delete(group);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminGroupService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAllGroupNamesReturnsOrderedSet() {
        Group a = new Group();
        a.setName("A");
        Group b = new Group();
        b.setName("B");
        when(groupRepository.findAllByOrderByNameAsc()).thenReturn(List.of(a, b));

        assertThat(adminGroupService.findAllGroupNames()).containsExactly("A", "B");
    }
}
