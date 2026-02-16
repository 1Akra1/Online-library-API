package com.example.practica.controller;

import com.example.practica.entity.Book;
import com.example.practica.entity.User;
import com.example.practica.repository.BookRepository;
import com.example.practica.repository.UserRepository;
import com.example.practica.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LibraryController {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // Инъекция зависимостей через конструктор
    public LibraryController(BookRepository bookRepository,
                             UserRepository userRepository,
                             UserService userService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // --- КНИГИ ---

    // 1. Список всех доступных книг (не требует логина по ТЗ)
    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    // 2. Создание (Должно быть только для ADMIN)
    @PostMapping("/books")
    public Book addBook(@RequestBody Book book) {
        return bookRepository.save(book);
    }

    // 2. Получение информации по отдельной книге
    @GetMapping("/books/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- АВТОРИЗАЦИЯ (Упрощенно для начала) ---

    // Регистрация пользователя
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            // Вызываем логику из сервиса, а не напрямую репозиторий
            User savedUser = userService.registerUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (RuntimeException e) {
            // Если email занят, сервис выбросит ошибку, и мы вернем 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ИЗБРАННОЕ ---

    // Добавление книги в избранное (Требует ID пользователя и книги)
    @PostMapping("/users/{userId}/favorites/{bookId}")
    public ResponseEntity<String> addToFavorites(
            @PathVariable Long userId,
            @PathVariable Long bookId,
            java.security.Principal principal) {

        // Получаем email текущего залогиненного пользователя
        String currentEmail = principal.getName();

        // Находим пользователя в базе по этому email
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Юзер не найден"));

        // ПРОВЕРКА: совпадает ли ID из ссылки с ID того, кто залогинился?
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body("Вы не можете менять чужое избранное!");
        }

        userService.addBookToFavorites(userId, bookId);
        return ResponseEntity.ok("Книга добавлена в избранное");
    }
    @GetMapping("/books/export")
    public void exportBooksToCSV(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=all_books.csv");

        List<Book> books = bookRepository.findAll();

        java.io.PrintWriter writer = response.getWriter();

        // Добавляем BOM для корректного отображения кириллицы в Excel
        writer.write('\ufeff');

        writer.println("ID;Название;Описание");

        for (Book book : books) {
            // Убираем возможные точки с запятой из описания, чтобы не ломать столбцы
            String desc = book.getDescription() != null ? book.getDescription().replace(";", " ") : "";
            writer.println(book.getId() + ";" + book.getTitle() + ";" + desc);
        }
        writer.flush();
    }

    // --- АВТОРИЗАЦИЯ ---
    // 2. Логин (Пункт 2 ТЗ)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        try {
            User user = userService.login(email, password);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // --- УПРАВЛЕНИЕ КНИГАМИ (АДМИН) ---

    // 4. Удаление книги (Пункт 4 ТЗ)
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 6. Удаление книги из избранного (Пункт 6 ТЗ)
    @DeleteMapping("/users/{userId}/favorites/{bookId}")
    public ResponseEntity<String> removeFromFavorites(
            @PathVariable Long userId,
            @PathVariable Long bookId,
            java.security.Principal principal) {

        // 1. Проверяем, залогинен ли пользователь (безопасность на уровне кода)
        if (principal == null) {
            return ResponseEntity.status(401).body("Нужна авторизация");
        }

        // 2. Получаем email того, кто отправил запрос
        String currentEmail = principal.getName();

        // 3. Находим этого пользователя в базе
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 4. ГЛАВНАЯ ПРОВЕРКА: Тот ли это пользователь?
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body("Вы не можете удалять книги из чужого избранного!");
        }

        // 5. Если проверки прошли, удаляем
        userService.removeBookFromFavorites(userId, bookId);
        return ResponseEntity.ok("Книга удалена из избранного");
    }

}