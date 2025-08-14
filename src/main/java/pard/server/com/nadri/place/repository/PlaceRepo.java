package pard.server.com.nadri.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pard.server.com.nadri.place.entity.Place;

@Repository
public interface PlaceRepo extends JpaRepository<Long, Place> {
}
