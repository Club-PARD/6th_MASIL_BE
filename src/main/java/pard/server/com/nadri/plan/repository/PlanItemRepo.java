package pard.server.com.nadri.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pard.server.com.nadri.plan.entity.Plan;
import pard.server.com.nadri.plan.entity.PlanItem;

import java.util.List;

@Repository
public interface PlanItemRepo extends JpaRepository<PlanItem, Long> {
    List<PlanItem> findAllByPlan(Plan plan);
}
