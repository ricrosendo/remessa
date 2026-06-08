package br.com.inter.service.impl;

import br.com.inter.dto.CreateUserRequest;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UpdateUserRequest;
import br.com.inter.enums.UserType;
import br.com.inter.exception.InvalidUserDocumentException;
import br.com.inter.exception.UserAlreadyExistsException;
import br.com.inter.exception.UserNotFoundException;
import br.com.inter.model.User;
import br.com.inter.repository.UserRepository;
import br.com.inter.service.UserService;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Singleton
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public User create(CreateUserRequest request) {
        LOGGER.info("Starting user creation. type={}, email={}", request.type(), request.email());
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

        User savedUser = userRepository.save(user);
        LOGGER.info("User persisted successfully. userId={}, type={}", savedUser.getId(), savedUser.getType());
        return savedUser;
    }

    @Override
    public List<User> findAll() {
        LOGGER.debug("Fetching all users");
        List<User> users = StreamSupport.stream(userRepository.findAll().spliterator(), false).toList();
        LOGGER.debug("Users fetched successfully. count={}", users.size());
        return users;
    }

    @Override
    public User findById(UUID id) {
        LOGGER.debug("Fetching user by id. userId={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("User not found. userId={}", id);
                    return new UserNotFoundException(id);
                });
    }

    @Transactional
    @Override
    public User update(UUID id, UpdateUserRequest request) {
        LOGGER.info("Starting user update. userId={}, type={}, email={}", id, request.type(), request.email());
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

        User updatedUser = userRepository.update(user);
        LOGGER.info("User updated successfully. userId={}, type={}", updatedUser.getId(), updatedUser.getType());
        return updatedUser;
    }

    @Transactional
    @Override
    public User updateBalance(UUID id, UpdateUserBalanceRequest request) {
        LOGGER.info("Starting user balance update. userId={}", id);
        User user = findById(id);

        user.setBrlBalance(request.brlBalance());
        user.setUsdBalance(request.usdBalance());

        User updatedUser = userRepository.update(user);
        LOGGER.info("User balance persisted successfully. userId={}", updatedUser.getId());
        return updatedUser;
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        LOGGER.info("Starting user deletion. userId={}", id);
        User user = findById(id);
        userRepository.delete(user);
        LOGGER.info("User deleted from repository. userId={}", id);
    }

    private void validateUniqueFieldsForCreate(String email, String cpf, String cnpj) {
        if (userRepository.existsByEmail(email)) {
            LOGGER.warn("User creation rejected because email already exists. email={}", email);
            throw new UserAlreadyExistsException("email");
        }

        if (hasText(cpf) && userRepository.existsByCpf(cpf)) {
            LOGGER.warn("User creation rejected because CPF already exists");
            throw new UserAlreadyExistsException("cpf");
        }

        if (hasText(cnpj) && userRepository.existsByCnpj(cnpj)) {
            LOGGER.warn("User creation rejected because CNPJ already exists");
            throw new UserAlreadyExistsException("cnpj");
        }
    }

    private void validateUniqueFieldsForUpdate(UUID id, String email, String cpf, String cnpj) {
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            LOGGER.warn("User update rejected because email already exists. userId={}, email={}", id, email);
            throw new UserAlreadyExistsException("email");
        }

        if (hasText(cpf) && userRepository.existsByCpfAndIdNot(cpf, id)) {
            LOGGER.warn("User update rejected because CPF already exists. userId={}", id);
            throw new UserAlreadyExistsException("cpf");
        }

        if (hasText(cnpj) && userRepository.existsByCnpjAndIdNot(cnpj, id)) {
            LOGGER.warn("User update rejected because CNPJ already exists. userId={}", id);
            throw new UserAlreadyExistsException("cnpj");
        }
    }

    private void validateDocument(UserType type, String cpf, String cnpj) {
        switch (type) {
            case INDIVIDUAL -> {
                if (!hasText(cpf)) {
                    LOGGER.warn("User validation rejected because CPF is missing for individual user");
                    throw new InvalidUserDocumentException("CPF is required for individual users");
                }
            }
            case COMPANY -> {
                if (!hasText(cnpj)) {
                    LOGGER.warn("User validation rejected because CNPJ is missing for company user");
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