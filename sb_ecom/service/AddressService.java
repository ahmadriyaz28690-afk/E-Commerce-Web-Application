package sb_ecom.service;

import jakarta.validation.Valid;
import sb_ecom.model.User;
import sb_ecom.payload.AddressDTO;

import java.util.List;

public interface AddressService {
    AddressDTO createAddress(@Valid AddressDTO addressDTO, User user);

    List<AddressDTO> getAddresses();

    AddressDTO getAddressesById(Long addressId);

    List<AddressDTO> getUserAddresses(User user);


    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);

    String deleteAddress(Long addressId);
}
