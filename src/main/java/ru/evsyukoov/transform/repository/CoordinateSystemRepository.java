package ru.evsyukoov.transform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.evsyukoov.transform.model.CoordinateSystem;
import java.util.List;

public interface CoordinateSystemRepository extends JpaRepository<CoordinateSystem, Long> {

    List<CoordinateSystem> findCoordinateSystemByDescriptionLikeIgnoreCase(String description);

    List<CoordinateSystem> findCoordinateSystemByDescriptionLike(String description);

    CoordinateSystem findFirstByDescription(String description);

}
