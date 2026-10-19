package com.capstore.service;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.entity.Address;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.AddressRepository;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addresses;
    private final AuthFacade auth;

    @Transactional(readOnly = true)
    public List<AddressResponse> list() {
        return addresses.findByUserIdOrderByDefaultAddressDescIdDesc(auth.currentUser().getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse create(AddressRequest r) {
        User u = auth.currentUser();
        Address a = new Address();
        a.setUser(u);
        boolean first = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId()).isEmpty();
        apply(a, r);
        a.setDefaultAddress(r.defaultAddress() || first);
        if (a.isDefaultAddress()) clearDefaults(u.getId());
        return toResponse(addresses.save(a));
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest r) {
        User u = auth.currentUser();
        Address a = addresses.findByIdAndUserId(id, u.getId()).orElseThrow(() -> ApiException.notFound("Address not found"));
        apply(a, r);
        if (r.defaultAddress()) {
            clearDefaults(u.getId());
            a.setDefaultAddress(true);
        }
        return toResponse(a);
    }

    @Transactional
    public void delete(Long id) {
        User u = auth.currentUser();
        Address a = addresses.findByIdAndUserId(id, u.getId()).orElseThrow(() -> ApiException.notFound("Address not found"));
        addresses.delete(a);
    }

    private void clearDefaults(Long userId) {
        addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).forEach(x -> x.setDefaultAddress(false));
    }

    private void apply(Address a, AddressRequest r) {
        a.setLabel(r.label());
        a.setName(r.name().trim());
        a.setPhone(r.phone());
        a.setLine1(r.line1().trim());
        a.setLine2(r.line2());
        a.setCity(r.city().trim());
        a.setState(r.state().trim());
        a.setPincode(r.pincode());
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getName(), a.getPhone(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPincode(), a.isDefaultAddress());
    }
}
