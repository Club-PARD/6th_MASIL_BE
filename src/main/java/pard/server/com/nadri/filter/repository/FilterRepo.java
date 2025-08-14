package pard.server.com.nadri.filter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilterRepo extends JpaRepository<Long, FilterRepo> {
}
