package au.edu.ardc.igsn;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.idm.authorization.Permission;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class UserTest {

    @Test
    public void can_check_whether_a_user_has_permission() {
        User user = new User(UUID.randomUUID());
        // mock a user resources
        List<Permission> permissions = new ArrayList<>();
        Permission permission = new Permission();
        UUID res1 = UUID.randomUUID();
        permission.setResourceId(res1.toString());
        permission.setResourceName("Resource 1");
        permissions.add(permission);
        user.setAllocations(permissions);

        assertThat(user.hasPermission(res1.toString())).isTrue();
        assertThat(user.hasPermission(UUID.randomUUID().toString())).isFalse();
    }

    @Test
    public void can_check_whether_a_user_has_permission_scope() {
        User user = new User(UUID.randomUUID());

        // mock a user resources
        List<Permission> permissions = new ArrayList<>();
        Permission permission = new Permission();
        UUID res1 = UUID.randomUUID();
        permission.setResourceId(res1.toString());
        permission.setResourceName("Resource 1");
        permission.setScopes(Sets.newHashSet(Scope.UPDATE.getValue(), Scope.CREATE.getValue()));
        permissions.add(permission);
        user.setAllocations(permissions);

        assertThat(user.hasPermission(res1.toString(), Scope.UPDATE)).isTrue();
        assertThat(user.hasPermission(res1.toString(), Scope.IMPORT)).isFalse();
    }

}