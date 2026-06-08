package br.com.inter.service;

import br.com.inter.dto.CreateUserRequest;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UpdateUserRequest;
import br.com.inter.model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User create(CreateUserRequest request);

    List<User> findAll();

    User findById(UUID id);

    User update(UUID id, UpdateUserRequest request);

    User updateBalance(UUID id, UpdateUserBalanceRequest request);

    void delete(UUID id);
}
