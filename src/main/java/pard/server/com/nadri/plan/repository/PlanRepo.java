package pard.server.com.nadri.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pard.server.com.nadri.plan.entity.Plan;

@Repository
public interface PlanRepo extends JpaRepository<Long, Plan> {
}
