package au.edu.ardc.igsn.Repository;

import org.springframework.stereotype.Repository;

import au.edu.ardc.igsn.entity.Prefix;
import au.edu.ardc.igsn.entity.Registrant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RegistrantRepository extends JpaRepository<Registrant, Long>{

}
