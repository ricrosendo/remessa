package br.com.inter.service.impl;

import br.com.inter.dto.CreateUserRequest;
import br.com.inter.dto.UpdateUserRequest;
import br.com.inter.enums.UserType;
import br.com.inter.exception.InvalidUserDocumentException;
import br.com.inter.exception.UserAlreadyExistsException;
import br.com.inter.exception.UserNotFoundException;
import br.com.inter.model.User;
import br.com.inter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private AutoCloseable mocks;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void shouldCreateIndividualUser() {
        CreateUserRequest request = createIndividualRequest();
        User savedUser = userFromCreateRequest(request);
        savedUser.setId(UUID.randomUUID());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User user = userService.create(request);

        assertEquals(savedUser.getId(), user.getId());
        assertEquals(request.fullName(), user.getFullName());
        assertEquals(request.email(), user.getEmail());
        assertEquals(request.cpf(), user.getCpf());
        assertNull(user.getCnpj());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldCreateCompanyUser() {
        CreateUserRequest request = createCompanyRequest();
        User savedUser = userFromCreateRequest(request);
        savedUser.setId(UUID.randomUUID());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User user = userService.create(request);

        assertEquals(request.cnpj(), user.getCnpj());
        assertNull(user.getCpf());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldNotCreateUserWhenEmailAlreadyExists() {
        CreateUserRequest request = createIndividualRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

        assertEquals("User already exists with email", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateIndividualUserWithoutCpf() {
        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                "john.doe@email.com",
                "password",
                UserType.INDIVIDUAL,
                null,
                null,
                BigDecimal.TEN,
                BigDecimal.ONE
        );

        InvalidUserDocumentException exception = assertThrows(InvalidUserDocumentException.class, () -> userService.create(request));

        assertEquals("CPF is required for individual users", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateIndividualUserWithBlankCpf() {
        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                "john.doe@email.com",
                "password",
                UserType.INDIVIDUAL,
                " ",
                null,
                BigDecimal.TEN,
                BigDecimal.ONE
        );

        InvalidUserDocumentException exception = assertThrows(InvalidUserDocumentException.class, () -> userService.create(request));

        assertEquals("CPF is required for individual users", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateCompanyUserWithoutCnpj() {
        CreateUserRequest request = new CreateUserRequest(
                "Acme Inc",
                "contact@acme.com",
                "password",
                UserType.COMPANY,
                null,
                null,
                BigDecimal.TEN,
                BigDecimal.ONE
        );

        InvalidUserDocumentException exception = assertThrows(InvalidUserDocumentException.class, () -> userService.create(request));

        assertEquals("CNPJ is required for company users", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateIndividualUserWhenCpfAlreadyExists() {
        CreateUserRequest request = createIndividualRequest();

        when(userRepository.existsByCpf(request.cpf())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

        assertEquals("User already exists with cpf", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateCompanyUserWhenCnpjAlreadyExists() {
        CreateUserRequest request = createCompanyRequest();

        when(userRepository.existsByCnpj(request.cnpj())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

        assertEquals("User already exists with cnpj", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFindAllUsers() {
        User user = userFromCreateRequest(createIndividualRequest());

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> users = userService.findAll();

        assertEquals(1, users.size());
        assertEquals(user.getEmail(), users.getFirst().getEmail());
    }

    @Test
    void shouldFindUserById() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createIndividualRequest());
        user.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User foundUser = userService.findById(id);

        assertEquals(id, foundUser.getId());
    }

    @Test
    void shouldThrowWhenUserIsNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.findById(id));

        assertTrue(exception.getMessage().contains(id.toString()));
    }

    @Test
    void shouldUpdateUser() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createIndividualRequest());
        user.setId(id);
        UpdateUserRequest request = updateCompanyRequest();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.update(user)).thenReturn(user);

        User updatedUser = userService.update(id, request);

        assertEquals(request.fullName(), updatedUser.getFullName());
        assertEquals(request.email(), updatedUser.getEmail());
        assertEquals(UserType.COMPANY, updatedUser.getType());
        assertEquals(request.cnpj(), updatedUser.getCnpj());
        assertNull(updatedUser.getCpf());
        verify(userRepository).update(user);
    }

    @Test
    void shouldNotUpdateUserWhenCpfAlreadyExists() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createIndividualRequest());
        user.setId(id);
        UpdateUserRequest request = updateIndividualRequest();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByCpfAndIdNot(request.cpf(), id)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.update(id, request));

        assertEquals("User already exists with cpf", exception.getMessage());
        verify(userRepository, never()).update(any(User.class));
    }

    @Test
    void shouldNotUpdateUserWhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createIndividualRequest());
        user.setId(id);
        UpdateUserRequest request = updateIndividualRequest();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(request.email(), id)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.update(id, request));

        assertEquals("User already exists with email", exception.getMessage());
        verify(userRepository, never()).update(any(User.class));
    }

    @Test
    void shouldNotUpdateUserWhenCnpjAlreadyExists() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createCompanyRequest());
        user.setId(id);
        UpdateUserRequest request = updateCompanyRequest();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByCnpjAndIdNot(request.cnpj(), id)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.update(id, request));

        assertEquals("User already exists with cnpj", exception.getMessage());
        verify(userRepository, never()).update(any(User.class));
    }

    @Test
    void shouldNotUpdateCompanyUserWithoutCnpj() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createCompanyRequest());
        user.setId(id);
        UpdateUserRequest request = new UpdateUserRequest(
                "New Company",
                "new@company.com",
                "new-password",
                UserType.COMPANY,
                null,
                " ",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(2)
        );

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        InvalidUserDocumentException exception = assertThrows(InvalidUserDocumentException.class, () -> userService.update(id, request));

        assertEquals("CNPJ is required for company users", exception.getMessage());
        verify(userRepository, never()).update(any(User.class));
    }

    @Test
    void shouldDeleteUser() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createIndividualRequest());
        user.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.delete(id);

        verify(userRepository).delete(user);
    }

    @Test
    void shouldExposeUserExceptionMessage() {
        UserNotFoundException exception = new UserNotFoundException(UUID.randomUUID());

        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(exception.getMessage(), exception.message());
    }

    private CreateUserRequest createIndividualRequest() {
        return new CreateUserRequest(
                "John Doe",
                "john.doe@email.com",
                "password",
                UserType.INDIVIDUAL,
                "12345678901",
                null,
                BigDecimal.TEN,
                BigDecimal.ONE
        );
    }

    private CreateUserRequest createCompanyRequest() {
        return new CreateUserRequest(
                "Acme Inc",
                "contact@acme.com",
                "password",
                UserType.COMPANY,
                null,
                "12345678000199",
                BigDecimal.TEN,
                BigDecimal.ONE
        );
    }

    private UpdateUserRequest updateIndividualRequest() {
        return new UpdateUserRequest(
                "Jane Doe",
                "jane.doe@email.com",
                "new-password",
                UserType.INDIVIDUAL,
                "98765432100",
                null,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(2)
        );
    }

    private UpdateUserRequest updateCompanyRequest() {
        return new UpdateUserRequest(
                "New Company",
                "new@company.com",
                "new-password",
                UserType.COMPANY,
                null,
                "99887766000155",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(2)
        );
    }

    private User userFromCreateRequest(CreateUserRequest request) {
        return User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(request.password())
                .type(request.type())
                .cpf(request.cpf())
                .cnpj(request.cnpj())
                .brlBalance(request.brlBalance())
                .usdBalance(request.usdBalance())
                .build();
    }
}
