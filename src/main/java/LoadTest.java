import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

public class LoadTest {

    @Test
    public void testHighConcurrency() throws InterruptedException {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");

        int numThreads = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errorCount = new AtomicInteger(0);
        Random random = new Random();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        int operation = random.nextInt(4);

                        switch (operation) {
                            case 0:
                                String username = "user_" + threadId + "_" + j;
                                try {
                                    User user = User.create(username, "Test User", username + "@test.com");
                                    system.getUserManager().add(user);
                                } catch (IllegalArgumentException e) {

                                }
                                break;

                            case 1:
                                String roleName = "role_" + threadId + "_" + j;
                                try {
                                    Role role = new Role(roleName, "Test role");
                                    Permission perm = new Permission("READ", "test", "Test permission");
                                    role.addPermission(perm);
                                    system.getRoleManager().add(role);
                                } catch (IllegalArgumentException e) {

                                }
                                break;

                            case 2:
                                var users = system.getUserManager().findAll(null, null);
                                var roles = system.getRoleManager().findAll(null, null);
                                if (!users.isEmpty() && !roles.isEmpty()) {
                                    User user = users.get(random.nextInt(users.size()));
                                    Role role = roles.get(random.nextInt(roles.size()));
                                    try {
                                        if (!system.getAssignmentManager().userHasRole(user, role)) {
                                            AssignmentMetadata meta = AssignmentMetadata.now("admin", "Load test assignment");
                                            RoleAssignment assignment = new PermanentAssignment(user, role, meta);
                                            system.getAssignmentManager().add(assignment);
                                        }
                                    } catch (Exception e) {

                                    }
                                }
                                break;

                            case 3:
                                try {
                                    system.getUserManager().findByFilterParallel(u -> u.username().contains("user"), null);
                                    system.getRoleManager().findByFilterParallel(r -> r.name().contains("role"), null);
                                } catch (Exception e) {

                                }
                                break;
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Ошибка в потоке " + threadId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        int finalUserCount = system.getUserManager().count();
        int finalRoleCount = system.getRoleManager().count();
        int finalAssignmentCount = system.getAssignmentManager().count();

        System.out.println("Тест завершён:");
        System.out.println("Ошибок: " + errorCount.get());
        System.out.println("Пользователей: " + finalUserCount);
        System.out.println("Ролей: " + finalRoleCount);
        System.out.println("Назначений: " + finalAssignmentCount);
    }
}