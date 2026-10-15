package in.fixna.platform.hospitality.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import in.fixna.platform.hospitality.auth.JwtService;
import in.fixna.platform.hospitality.tenant.MembershipRole;
import in.fixna.platform.hospitality.tenant.Tenant;
import in.fixna.platform.hospitality.tenant.TenantMembership;
import in.fixna.platform.hospitality.tenant.TenantMembershipRepository;
import in.fixna.platform.hospitality.tenant.TenantRepository;
import in.fixna.platform.hospitality.tenant.User;
import in.fixna.platform.hospitality.tenant.UserRepository;

/**
 * Seeds a deterministic local test tenant+owner when running with the 'local' profile.
 * Runs after Flyway migrations complete. Idempotent: skips if the fixture email exists.
 */
@Configuration
@Profile("local")
public class LocalTestDataSeeder {

    @Bean
    CommandLineRunner seedLocalTestData(
            UserRepository users,
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            JdbcTemplate jdbc,
            PasswordEncoder encoder) {
        return args -> {
            String email = "owner@example.com";
            if (users.existsByEmail(email)) {
                return; // already seeded
            }
            String passwordHash = encoder.encode("test-password-123");
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            jdbc.update("""
                    WITH new_user AS (
                        INSERT INTO users (id, email, password_hash, first_name, last_name)
                        VALUES (?, ?, ?, 'Test', 'Owner')
                        ON CONFLICT (email) DO NOTHING
                        RETURNING id
                    ), new_tenant AS (
                        INSERT INTO tenants (id, name, tenant_type)
                        VALUES (?, 'Fixna Demo Hospitality', 'HOSPITALITY')
                        RETURNING id
                    ), new_membership AS (
                        INSERT INTO tenant_memberships (id, tenant_id, user_id, role)
                        VALUES (?, (SELECT id FROM new_tenant), (SELECT id FROM new_user), ?)
                    )
                    SELECT 1
                    """, userId, email, passwordHash, tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER.name());
        };
    }
}