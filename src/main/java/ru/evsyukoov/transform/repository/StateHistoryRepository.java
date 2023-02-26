package ru.evsyukoov.transform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.evsyukoov.transform.model.StateHistory;

@Repository
public interface StateHistoryRepository extends JpaRepository<StateHistory, Long> {
}
