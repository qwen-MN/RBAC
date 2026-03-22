import java.util.Comparator;

public class RoleSorters {

    public static Comparator<Role> byName() {
        return Comparator.comparing(Role::name);
    }

    public static Comparator<Role> byPermissionCount() {
        return (role1, role2) ->
                Integer.compare(role2.getPermissions().size(), role1.getPermissions().size());
    }
}
