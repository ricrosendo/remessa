package br.com.inter.service.impl;

import br.com.inter.exception.InvalidUserDocumentException;
import br.com.inter.exception.UserAlreadyExistsException;
import br.com.inter.exception.UserNotFoundException;
import br.com.inter.dto.CreateUserRequest;
import br.com.inter.dto.UpdateUserRequest;
import br.com.inter.model.User;
import br.com.inter.enums.UserType;
import br.com.inter.repository.UserRepository;
import br.com.inter.service.UserService;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Singleton
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public User create(CreateUserRequest request) {
        validateDocument(request.type(), request.cpf(), request.cnpj());
        validateUniqueFieldsForCreate(request.email(), request.cpf(), request.cnpj());

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(request.password())
                .type(request.type())
                .cpf(documentForIndividual(request.type(), request.cpf()))
                .cnpj(documentForCompany(request.type(), request.cnpj()))
                .brlBalance(request.brlBalance())
                .usdBalance(request.usdBalance())
                .build();

        return userRepository.save(user);
    }

    @Override
    public List<User> findAll() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false).toList();
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    @Override
    public User update(UUID id, UpdateUserRequest request) {
        User user = findById(id);

        validateDocument(request.type(), request.cpf(), request.cnpj());
        validateUniqueFieldsForUpdate(id, request.email(), request.cpf(), request.cnpj());

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setType(request.type());
        user.setCpf(documentForIndividual(request.type(), request.cpf()));
        user.setCnpj(documentForCompany(request.type(), request.cnpj()));
        user.setBrlBalance(request.brlBalance());
        user.setUsdBalance(request.usdBalance());

        return userRepository.update(user);
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    private void validateUniqueFieldsForCreate(String email, String cpf, String cnpj) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email");
        }

        if (hasText(cpf) && userRepository.existsByCpf(cpf)) {
            throw new UserAlreadyExistsException("cpf");
        }

        if (hasText(cnpj) && userRepository.existsByCnpj(cnpj)) {
            throw new UserAlreadyExistsException("cnpj");
        }
    }

    private void validateUniqueFieldsForUpdate(UUID id, String email, String cpf, String cnpj) {
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new UserAlreadyExistsException("email");
        }

        if (hasText(cpf) && userRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new UserAlreadyExistsException("cpf");
        }

        if (hasText(cnpj) && userRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new UserAlreadyExistsException("cnpj");
        }
    }

    private void validateDocument(UserType type, String cpf, String cnpj) {
        switch (type) {
            case INDIVIDUAL -> {
                if (!hasText(cpf)) {
                    throw new InvalidUserDocumentException("CPF is required for individual users");
                }
            }
            case COMPANY -> {
                if (!hasText(cnpj)) {
                    throw new InvalidUserDocumentException("CNPJ is required for company users");
                }
            }
        }
    }

    private String documentForIndividual(UserType type, String cpf) {
        return type == UserType.INDIVIDUAL ? cpf : null;
    }

    private String documentForCompany(UserType type, String cnpj) {
        return type == UserType.COMPANY ? cnpj : null;
    }

    private boolean hasText(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }
}
