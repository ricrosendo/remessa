package br.com.inter.repository;

import br.com.inter.model.Remittance;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

@Repository
public interface RemittanceRepository extends CrudRepository<Remittance, UUID> {
}
