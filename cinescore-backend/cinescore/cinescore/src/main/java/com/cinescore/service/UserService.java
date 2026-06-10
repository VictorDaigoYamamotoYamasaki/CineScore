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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final String DELETED_USER_NAME    = "Usuário Deletado";
    private static final String DELETED_EMAIL_DOMAIN = "@removed.invalid";

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

    @Transactional(readOnly = true)
    public List<UserResponseDTO> buscarPorNome(String nome) {
        return userRepository.findByNameContainingIgnoreCaseOrderByNameAsc(nome)
                .stream()
                .map(UserResponseDTO::fromUser)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO buscarPorId(String userId) {
        return UserResponseDTO.fromUser(findUserOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> listarTodos() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::fromUser)
                .toList();
    }

    @Transactional
    public UserResponseDTO atualizar(String userId, UserRequestDTO dto) {
        User user = findUserOrThrow(userId);
        validateEmailNotInUse(dto.getEmail(), userId);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        atualizarSenhaSeInformada(user, dto.getPassword());
        return UserResponseDTO.fromUser(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO atualizarPerfil(String userId, ProfileUpdateDTO dto) {
        User user = findUserOrThrow(userId);
        validateEmailNotInUse(dto.getEmail(), userId);
        validateNameNotInUse(dto.getName(), userId);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        atualizarSenhaSeInformada(user, dto.getPassword());
        log.info("Perfil atualizado: userId={}", userId);
        return UserResponseDTO.fromUser(userRepository.save(user));
    }

    @Transactional
    public void deletarConta(String userId, boolean deletarReviews) {
        User user = findUserOrThrow(userId);
        if (deletarReviews) {
            excluirContaComReviews(userId);
            log.info("Conta excluída com reviews: userId={}", userId);
        } else {
            anonimizarDadosPessoais(user);
            log.info("Conta anonimizada: userId={}", userId);
        }
    }

    @Transactional
    public void deletar(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuário", userId);
        }
        userRepository.deleteById(userId);
    }


    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateEmailNotInUse(String email, String excludeUserId) {
        boolean emailEmUso = excludeUserId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeUserId);
        if (emailEmUso) throw new DuplicateResourceException("Usuário", "e-mail", email);
    }

    /** Package-private: chamado por {@link AuthService} durante o registro. */
    void validateNameNotInUse(String name, String excludeUserId) {
        boolean nameEmUso = excludeUserId == null
                ? userRepository.existsByNameIgnoreCase(name)
                : userRepository.existsByNameIgnoreCaseAndIdNot(name, excludeUserId);
        if (nameEmUso) throw new DuplicateResourceException("Usuário", "nome", name);
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

    private void excluirContaComReviews(String userId) {
        reviewRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    private void anonimizarDadosPessoais(User user) {
        user.setName(DELETED_USER_NAME);
        user.setEmail("deleted_" + UUID.randomUUID() + DELETED_EMAIL_DOMAIN);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        userRepository.save(user);
    }
}
