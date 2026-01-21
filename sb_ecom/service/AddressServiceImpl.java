package sb_ecom.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sb_ecom.exceptions.ResourceNotFoundException;
import sb_ecom.model.Address;
import sb_ecom.model.User;
import sb_ecom.payload.AddressDTO;
import sb_ecom.repositories.AddressRepository;
import sb_ecom.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AddressServiceImpl implements AddressService{

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);
        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addresses = addressRepository.findAll();
        List<AddressDTO>addressDTOs =addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO getAddressesById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address", "addressId", addressId));
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addresses = user.getAddresses();
        return addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDatabase=addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        addressFromDatabase.setCity(addressDTO.getCity());
        addressFromDatabase.setPincode(addressDTO.getPincode());
        addressFromDatabase.setState(addressDTO.getState());
        addressFromDatabase.setCountry(addressDTO.getCountry());
        addressFromDatabase.setBuildingName(addressDTO.getBuildingName());
        addressFromDatabase.setStreet(addressDTO.getStreet());

        Address updateAddress = addressRepository.save(addressFromDatabase);

        User user = addressFromDatabase.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        user.getAddresses().add(updateAddress);
        userRepository.save(user);

        return modelMapper.map(updateAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {
        Address addressFromDatabase = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address", "addressId", addressId));

        User user = addressFromDatabase.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(addressFromDatabase);

        return "Address deleted successfully with addressId "+ addressId;
    }
}
