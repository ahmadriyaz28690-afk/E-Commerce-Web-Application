package sb_ecom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sb_ecom.model.AppRole;
import sb_ecom.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role , Long> {
    Optional<Role> findByRoleName(AppRole appRole);
}
