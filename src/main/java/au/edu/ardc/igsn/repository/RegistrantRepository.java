package au.edu.ardc.igsn.repository;

import org.springframework.stereotype.Repository;

import au.edu.ardc.igsn.entity.Registrant;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RegistrantRepository extends JpaRepository<Registrant, Long>{

}
