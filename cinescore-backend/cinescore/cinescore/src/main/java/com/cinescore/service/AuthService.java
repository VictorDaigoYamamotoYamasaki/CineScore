package com.cinescore.service;

import com.cinescore.dto.LoginRequestDTO;
import com.cinescore.dto.LoginResponseDTO;
import com.cinescore.dto.UserRequestDTO;
import com.cinescore.exception.DuplicateResourceException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.User;
import com.cinescore.repository.UserRepository;
import com.cinescore.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final ModerationService     moderationService;
    private final UserService           userService;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponseDTO login(LoginRequestDTO dto) {
        authenticateCredentials(dto.getEmail(), dto.getPassword());
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "e-mail", dto.getEmail()));
        log.info("Login bem-sucedido: email={}", dto.getEmail());
        return buildLoginResponse(user);
    }

    public LoginResponseDTO register(UserRequestDTO dto) {
        moderationService.verificar(dto.getName(), "nome de usuário");
        validateEmailNotInUse(dto.getEmail());
        userService.validateNameNotInUse(dto.getName(), null);

        User user = buildNewUser(dto);
        userRepository.save(user);
        log.info("Novo usuário registrado: email={}", dto.getEmail());
        return buildLoginResponse(user);
    }


    private void authenticateCredentials(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }

    private void validateEmailNotInUse(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Usuário", "e-mail", email);
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

    private LoginResponseDTO buildLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return LoginResponseDTO.builder()
                .token(token).type("Bearer")
                .userId(user.getId())
                .name(user.getName()).email(user.getEmail()).role(user.getRole())
                .build();
    }
}
