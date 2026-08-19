package com.lp.ligaportugalzone.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need a real database.
 *
 * <p>Testcontainers starts a throwaway PostgreSQL in Docker and {@code @ServiceConnection} points
 * Spring's DataSource at it, so no JDBC URL has to be configured by hand. The container is a
 * static singleton started once per JVM and shared by every subclass — starting one container per
 * test class would dominate the build time.
 *
 * <p>Requires a running Docker daemon. GitHub Actions' ubuntu runners have one out of the box.
 */
public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static {
        POSTGRES.start();
    }
}
