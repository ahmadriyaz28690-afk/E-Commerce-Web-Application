package sb_ecom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sb_ecom.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
