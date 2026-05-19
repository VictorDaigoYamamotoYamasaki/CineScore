package com.cinescore.service;

import com.cinescore.dto.ProfileUpdateDTO;
import com.cinescore.dto.UserRequestDTO;
import com.cinescore.dto.UserResponseDTO;
import com.cinescore.exception.DuplicateResourceException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.User;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final String DELETED_USER_NAME  = "Usuário Deletado";
    private static final String DELETED_EMAIL_DOMAIN = "@removed.invalid";
    private static final String DELETED_PASSWORD_HASH = "[REMOVIDO]";

    private final UserRepository   userRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder  passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado com e-mail: " + email));
    }

    public UserResponseDTO criar(UserRequestDTO dto) {
        validateEmailNotInUse(dto.getEmail(), null);
        User user = buildNewUser(dto);
        return UserResponseDTO.fromUser(userRepository.save(user));
    }

    public UserResponseDTO buscarPorId(Long userId) {
        return UserResponseDTO.fromUser(findUserOrThrow(userId));
    }

    public List<UserResponseDTO> listarTodos() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::fromUser)
                .toList();
    }

    public UserResponseDTO atualizar(Long userId, UserRequestDTO dto) {
        User user = findUserOrThrow(userId);
        validateEmailNotInUse(dto.getEmail(), userId);

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        atualizarSenhaSeInformada(user, dto.getPassword());

        return UserResponseDTO.fromUser(userRepository.save(user));
    }


    @Transactional
    public UserResponseDTO atualizarPerfil(Long userId, ProfileUpdateDTO dto) {
        User user = findUserOrThrow(userId);
        validateEmailNotInUse(dto.getEmail(), userId);
        validateNameNotInUse(dto.getName(), userId);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        atualizarSenhaSeInformada(user, dto.getPassword());
        return UserResponseDTO.fromUser(userRepository.save(user));
    }

    @Transactional
    public void deletarConta(Long userId, boolean deletarReviews) {
        User user = findUserOrThrow(userId);

        if (deletarReviews) {
            excluirContaComReviews(userId);
        } else {
            anonimizarDadosPessoais(user);
        }
    }

    public void deletar(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuário", userId);
        }
        userRepository.deleteById(userId);
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateEmailNotInUse(String email, Long excludeUserId) {
        boolean emailEmUso = excludeUserId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeUserId);

        if (emailEmUso) {
            throw new DuplicateResourceException("Usuário", "e-mail", email);
        }
    }

    private void validateNameNotInUse(String name, Long excludeUserId) {
        boolean nameEmUso = excludeUserId == null
                ? userRepository.existsByNameIgnoreCase(name)
                : userRepository.existsByNameIgnoreCaseAndIdNot(name, excludeUserId);

        if (nameEmUso) {
            throw new DuplicateResourceException("Usuário", "nome", name);
        }
    }

    private User buildNewUser(UserRequestDTO dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role("USER")
                .build();
    }

    private void atualizarSenhaSeInformada(User user, String newPassword) {
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
    }

    private void excluirContaComReviews(Long userId) {
        reviewRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    private void anonimizarDadosPessoais(User user) {
        user.setName(DELETED_USER_NAME);
        user.setEmail("deleted_" + UUID.randomUUID() + DELETED_EMAIL_DOMAIN);
        user.setPasswordHash(DELETED_PASSWORD_HASH);
        userRepository.save(user);
    }
}
