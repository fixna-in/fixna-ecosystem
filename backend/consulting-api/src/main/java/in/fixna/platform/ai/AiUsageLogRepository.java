package in.fixna.platform.ai;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {}
