package com.example.practica.service;

import com.example.practica.entity.Book;
import com.example.practica.entity.User;
import com.example.practica.repository.BookRepository;
import com.example.practica.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
// ДОБАВИЛИ: implements UserDetailsService
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public UserService(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    // ЭТОТ МЕТОД НУЖЕН ДЛЯ SPRING SECURITY (чтобы убрать 401 ошибку)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // Благодаря NoOpPasswordEncoder сработает обычный текст
                .authorities(user.getRole())  // Передаем ADMIN или CLIENT
                .build();
    }

    // Твои старые методы оставляем без изменений...
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email уже занят!");
        }
        user.setRole("CLIENT");
        return userRepository.save(user);
    }

    public User login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password))
                .orElseThrow(() -> new RuntimeException("Неверный логин или пароль"));
    }

    @Transactional
    public void addBookToFavorites(Long userId, Long bookId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Юзер не найден"));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Книга не найдена"));
        user.getFavorites().add(book);
        userRepository.save(user);
    }

    @Transactional
    public void removeBookFromFavorites(Long userId, Long bookId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Юзер не найден"));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Книга не найдена"));
        user.getFavorites().remove(book);
        userRepository.save(user);
    }
}