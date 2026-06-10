package com.cinescore.service;

import com.cinescore.dto.FollowUserDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.exception.SelfFollowException;
import com.cinescore.model.Follower;
import com.cinescore.model.User;
import com.cinescore.repository.FollowerRepository;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowerService - Testes Unitários")
class FollowerServiceTest {

    @Mock private FollowerRepository followerRepository;
    @Mock private UserRepository     userRepository;
    @Mock private ReviewRepository   reviewRepository;

    @InjectMocks private FollowerService followerService;

    private static final String FOLLOWER_ID = "uuid-ana-001";
    private static final String FOLLOWED_ID = "uuid-joao-002";

    private User followerUser;
    private User followedUser;

    @BeforeEach
    void configurar() {
        followerUser = User.builder().id(FOLLOWER_ID).name("Ana").email("ana@test.com").role("USER").build();
        followedUser = User.builder().id(FOLLOWED_ID).name("João").email("joao@test.com").role("USER").build();
    }

    @Test
    @DisplayName("Deve seguir usuário com sucesso quando ainda não segue")
    void deveSeguirUsuarioComSucesso() {
        when(followerRepository.existsByFollowerIdAndFollowedId(FOLLOWER_ID, FOLLOWED_ID)).thenReturn(false);
        when(userRepository.findById(FOLLOWER_ID)).thenReturn(Optional.of(followerUser));
        when(userRepository.findById(FOLLOWED_ID)).thenReturn(Optional.of(followedUser));

        followerService.seguir(FOLLOWER_ID, FOLLOWED_ID);

        verify(followerRepository).save(any(Follower.class));
    }

    @Test
    @DisplayName("Não deve duplicar follow quando já segue")
    void naoDeveDuplicarFollowSeJaSegue() {
        when(followerRepository.existsByFollowerIdAndFollowedId(FOLLOWER_ID, FOLLOWED_ID)).thenReturn(true);

        followerService.seguir(FOLLOWER_ID, FOLLOWED_ID);

        verify(followerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar SelfFollowException ao tentar seguir a si mesmo")
    void deveLancarExcecaoAoSeguirASiMesmo() {
        assertThatThrownBy(() -> followerService.seguir(FOLLOWER_ID, FOLLOWER_ID))
                .isInstanceOf(SelfFollowException.class);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao seguir usuário inexistente")
    void deveLancarExcecaoAoSeguirUsuarioInexistente() {
        when(followerRepository.existsByFollowerIdAndFollowedId(FOLLOWER_ID, "uuid-inexistente")).thenReturn(false);
        when(userRepository.findById(FOLLOWER_ID)).thenReturn(Optional.of(followerUser));
        when(userRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followerService.seguir(FOLLOWER_ID, "uuid-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve deixar de seguir usuário com sucesso")
    void deveDeixarDeSeguirComSucesso() {
        followerService.deixarDeSeguir(FOLLOWER_ID, FOLLOWED_ID);

        verify(followerRepository).deleteByFollowerIdAndFollowedId(FOLLOWER_ID, FOLLOWED_ID);
    }

    @Test
    @DisplayName("Deve retornar true quando usuário já segue outro")
    void deveRetornarTrueQuandoJaSegue() {
        when(followerRepository.existsByFollowerIdAndFollowedId(FOLLOWER_ID, FOLLOWED_ID)).thenReturn(true);

        boolean resultado = followerService.verificarSeguindo(FOLLOWER_ID, FOLLOWED_ID);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando usuário não segue outro")
    void deveRetornarFalseQuandoNaoSegue() {
        when(followerRepository.existsByFollowerIdAndFollowedId(FOLLOWER_ID, FOLLOWED_ID)).thenReturn(false);

        boolean resultado = followerService.verificarSeguindo(FOLLOWER_ID, FOLLOWED_ID);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve contar seguidores corretamente")
    void deveContarSeguidoresCorretamente() {
        when(followerRepository.countByFollowedId(FOLLOWED_ID)).thenReturn(15L);

        long resultado = followerService.contarSeguidores(FOLLOWED_ID);

        assertThat(resultado).isEqualTo(15L);
    }

    @Test
    @DisplayName("Deve contar seguindo corretamente")
    void deveContarSeguindoCorretamente() {
        when(followerRepository.countByFollowerId(FOLLOWER_ID)).thenReturn(7L);

        long resultado = followerService.contarSeguindo(FOLLOWER_ID);

        assertThat(resultado).isEqualTo(7L);
    }

    @Test
    @DisplayName("Deve listar seguidores do usuário")
    void deveListarSeguidores() {
        Follower follower = Follower.builder().follower(followerUser).followed(followedUser).build();
        when(followerRepository.findByFollowedId(FOLLOWED_ID)).thenReturn(List.of(follower));
        when(reviewRepository.countByUserId(FOLLOWER_ID)).thenReturn(3L);

        List<FollowUserDTO> resultado = followerService.listarSeguidores(FOLLOWED_ID, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getName()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Deve listar usuários que está seguindo")
    void deveListarSeguindo() {
        Follower follower = Follower.builder().follower(followerUser).followed(followedUser).build();
        when(followerRepository.findByFollowerId(FOLLOWER_ID)).thenReturn(List.of(follower));
        when(reviewRepository.countByUserId(FOLLOWED_ID)).thenReturn(5L);

        List<FollowUserDTO> resultado = followerService.listarSeguindo(FOLLOWER_ID, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getName()).isEqualTo("João");
    }
}
