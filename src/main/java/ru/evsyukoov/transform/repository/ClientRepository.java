package ru.evsyukoov.transform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.evsyukoov.transform.model.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Client findClientById(long id);

}
