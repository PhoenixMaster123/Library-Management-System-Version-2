package app.support;

import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.adapters.output.repositories.UserRepository;
import app.infrastructure.config.database.DatabaseSeeder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Puts the database and caches into a known state before each test method.
 *
 * <p>This replaces {@code @DirtiesContext(AFTER_EACH_TEST_METHOD)}, which every integration test
 * used to carry. That annotation rebuilt the whole Spring context between test methods - about a
 * hundred application start-ups per run, and most of a ten-minute suite. What it was quietly
 * providing, and what has to be provided some other way:
 *
 * <ul>
 *   <li><b>A fresh database.</b> Closing the context closed the connection pool, which drops an H2
 *       in-memory database. Without that the schema survives the whole run - and because classes
 *       using {@code @MockitoBean} get their own Spring context pointed at the same
 *       {@code jdbc:h2:mem:library_ms}, each context's seeder wrote another copy of the fixture
 *       into it.</li>
 *   <li><b>The dev fixture.</b> {@link DatabaseSeeder} is a {@code CommandLineRunner}, so a new
 *       context re-seeded the shelves. Tests that read seeded data need it back after the previous
 *       test's {@code @AfterEach} wiped it.</li>
 *   <li><b>Empty caches.</b> Entries outlive the rows they came from, so a later test could be
 *       answered from data an earlier one deleted.</li>
 * </ul>
 *
 * <p>The reset is unconditional rather than "only when empty": tests clean up to differing depths -
 * some delete only customers - and a partial fixture is what produced duplicate-title failures.
 * Deleting a few dozen rows and re-inserting them costs milliseconds against seconds for a context
 * restart. It runs before {@code @BeforeEach}, so anything a test sets up for itself survives.
 *
 * <p>Registered for every Spring test through {@code META-INF/spring.factories}.
 */
public class TestStateResetListener extends AbstractTestExecutionListener {

    /** After Spring's own listeners, so the context is ready to be read from. */
    @Override
    public int getOrder() {
        return 4000;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        ApplicationContext context = testContext.getApplicationContext();

        DatabaseSeeder seeder = context.getBeanProvider(DatabaseSeeder.class).getIfAvailable();
        if (seeder == null) {
            // Not an application context with the dev fixture in it; nothing to reset.
            clearCaches(context);
            return;
        }

        clearCaches(context);
        wipe(context);
        seeder.run();
    }

    private static void clearCaches(ApplicationContext context) {
        CacheManager cacheManager = context.getBeanProvider(CacheManager.class).getIfAvailable();
        if (cacheManager == null) {
            return;
        }

        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    /** Children before parents: a transaction points at a book and a customer. */
    private static void wipe(ApplicationContext context) {
        deleteAll(context, TransactionRepository.class);
        deleteAll(context, UserRepository.class);
        deleteAll(context, BookRepository.class);
        deleteAll(context, AuthorRepository.class);
        deleteAll(context, CustomerRepository.class);
    }

    private static void deleteAll(ApplicationContext context, Class<? extends JpaRepository<?, ?>> type) {
        JpaRepository<?, ?> repository = context.getBeanProvider(type).getIfAvailable();
        if (repository != null) {
            repository.deleteAll();
        }
    }
}
