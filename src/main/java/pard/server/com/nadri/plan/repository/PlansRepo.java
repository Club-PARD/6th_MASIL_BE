package pard.server.com.nadri.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pard.server.com.nadri.plan.entity.Plans;

@Repository
public interface PlansRepo extends JpaRepository<Plans, Long> {
}
