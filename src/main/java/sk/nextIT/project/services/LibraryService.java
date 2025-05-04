package sk.nextIT.project.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sk.nextIT.project.models.Book;
import sk.nextIT.project.models.Borrowed;
import sk.nextIT.project.models.Library;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryService {
    @Getter
    public static class Wrapper {
        @JsonProperty("Library")
        private Library Library;
    }
    private Library libraryCache;
    private final ObjectMapper objectMapper;

    @Autowired
    public LibraryService(ObjectMapper objectMapper) {
        // Register the JavaTimeModule for LocalDate and LocalDateTime support
        this.objectMapper = objectMapper.registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    public Library loadLibrary() throws IOException {
        if (libraryCache != null) return libraryCache;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("library.json")) {
            Wrapper wrapper = objectMapper.readValue(is, Wrapper.class);
            libraryCache = wrapper.getLibrary();
            return libraryCache;
        }
    }

    public List<Book> getBooks() throws IOException {
        return loadLibrary().getBook();
    }

    public void addBook(Book newBook) throws IOException {
        long maxId = getBooks().stream()
                .mapToLong(Book::getId)
                .max()
                .orElse(0);

        newBook.setId(maxId + 1);

        setBorrowed(newBook);
        getBooks().add(newBook);
    }

    public boolean updateBook(Long bookId, Book updatedBook) throws IOException {
        List<Book> books = getBooks();
        return books.stream()
                .filter(book -> book.getId().equals(bookId))
                .findFirst()
                .map(existingBook -> {
                    int index = books.indexOf(existingBook);
                    updatedBook.setId(bookId);
                    setBorrowed(updatedBook);
                    books.set(index, updatedBook);
                    return true;
                })
                .orElse(false);
    }

    public boolean deleteBook(Long bookId) throws IOException {
        return getBooks().removeIf(book -> book.getId().equals(bookId));
    }

    public boolean borrowBook(Long id, Borrowed borrowed) throws IOException {
        return getBooks().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .map(book -> {
                    book.setBorrowed(borrowed);
                    return true;
                }).orElse(false);
    }

    public Boolean returnBook(Long id) throws IOException {
        return getBooks().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .map(book -> {
                    if(book.getBorrowed().getFrom() == null){
                        return false;
                    }
                    book.setBorrowed(new Borrowed());
                    return true;
                }).orElse(false);
    }

    public List<Book> getPaginatedBooks(int page, int size, Boolean available) throws IOException {
        List<Book> allBooks = getBooks();
        if (available != null) {
            allBooks = allBooks.stream()
                    .filter(book -> available == (book.getBorrowed() != null && book.getBorrowed().getFrom() == null))
                    .collect(Collectors.toList());
        }
        int start = page * size;
        int end = Math.min(start + size, allBooks.size());
        if (start > allBooks.size()) return List.of();
        return allBooks.subList(start, end);
    }

    private void setBorrowed(Book newBook){
        if(newBook.getBorrowed() == null){
            newBook.setBorrowed(new Borrowed());
        }
    }

}
