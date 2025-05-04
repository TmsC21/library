package sk.nextIT.project.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sk.nextIT.project.models.Book;
import sk.nextIT.project.models.Borrowed;
import sk.nextIT.project.services.LibraryService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/library")
public class LibraryController {
    @Autowired
    private LibraryService libraryService;
    private static final Logger logger = LoggerFactory.getLogger(LibraryController.class);


    /**
     * GET /library/books
     * Retrieves a paginated list of books. Optionally filters by availability.
     *
     * @param page      The page number (default is 0).
     * @param size      The number of items per page (default is 3).
     * @param available Optional filter for book availability (true/false).
     * @return 200 OK with list of books if successful, 500 Internal Server Error on failure.
     */
    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "3") int size,
                                                  @RequestParam(required = false) Boolean available) {
        try {
            List<Book> books = libraryService.getPaginatedBooks(page, size, available);
            return new ResponseEntity<>(books, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * POST /library/book
     * Adds a new book to the library.
     *
     * Request Body Requirements:
     * - "Name": Required (max 15 characters). Example: "Môj Príbeh"
     * - "Author": Required. Example: "Jozef Mak"
     * - "Borrowed": Optional, but if present, must include:
     *      - "From": Required date in d.M.yyyy format, must not be in the future.
     *      - "FirstName"/"LastName": Optional.
     *
     * Validation:
     * - Returns 400 Bad Request with error messages if any required field is invalid or missing.
     * - Example error response:
     *   {
     *     "name": "Názov knihy je povinný",
     *     "name": "Názov knihy môže mať maximálne 15 znakov",
     *     "author": "Autor je povinný",
     *     "borrowed.from": "Dátum výpožičky nemôže byť v budúcnosti" | "Dátum výpožičky je povinný"
     *   }
     *
     * @param book A valid Book object in JSON format.
     * @return
     * - 201 Created if successfully added.
     * - 400 Bad Request if validation fails.
     * - 500 Internal Server Error on failure.
     */
    @PostMapping("/book")
    public ResponseEntity<Void> createBook(@RequestBody @Valid Book book) {
        logger.info("Request to add a new book: {}", book);
        try {
            libraryService.addBook(book);
            logger.info("Book added successfully: {}", book);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Error while adding book: {}", book, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /library/book/{id}
     * Updates an existing book by its ID.
     *
     * Request Body Requirements:
     * - "Name": Required (max 15 characters). Example: "Nový Názov"
     * - "Author": Required. Example: "Anna Nováková"
     * - "Borrowed": Optional, but if present, must include:
     *      - "From": Required date in d.M.yyyy format, must not be in the future.
     *      - "FirstName"/"LastName": Optional.
     *
     * Validation:
     * - Returns 400 Bad Request with error messages if any field is invalid or missing.
     * - Example validation error response:
     *   {
     *     "name": "Názov knihy je povinný",
     *     "name": "Názov knihy môže mať maximálne 15 znakov",
     *     "borrowed.from": "Dátum výpožičky nemôže byť v budúcnosti" | "Dátum výpožičky je povinný"
     *   }
     *
     * @param id   The ID of the book to update.
     * @param book A valid Book object with updated details.
     * @return
     * - 200 OK if updated successfully.
     * - 400 Bad Request if validation fails.
     * - 404 Not Found if the book with given ID doesn't exist.
     * - 500 Internal Server Error on unexpected failure.
     */
    @PutMapping("/book/{id}")
    public ResponseEntity<Void> updateBook(@PathVariable Long id, @RequestBody @Valid Book book) {
        logger.info("Request to update book with ID {}: {}", id, book);
        try {
            boolean success = libraryService.updateBook(id, book);
            if (success) {
                logger.info("Book with ID {} updated successfully: {}", id, book);
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Book with ID {} not found for update", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error while updating book with ID {}: {}", id, book, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DELETE /library/book/{id}
     * Deletes a book by its ID.
     *
     * @param id The ID of the book to delete.
     * @return 200 OK if deleted, 404 Not Found if book doesn't exist, 500 Internal Server Error on failure.
     */
    @DeleteMapping("/book/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        logger.info("Request to delete book with ID {}", id);
        try {
            boolean success = libraryService.deleteBook(id);
            if (success) {
                logger.info("Book with ID {} deleted successfully", id);
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Book with ID {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error while deleting book with ID {}: ", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PATCH /library/book/{id}/borrow
     * Marks a book as borrowed.
     *
     * Request Body Requirements:
     * - "FirstName": Optional. The first name of the borrower.
     * - "LastName": Optional. The last name of the borrower.
     * - "From": Required. The date of borrowing in d.M.yyyy format.
     *   - Must not be a future date. Example: "2.5.2025"
     *
     * Validation:
     * - Returns 400 Bad Request if "From" date is in the future or missing.
     * - Example validation error response:
     *   {
     *     "from": "Dátum výpožičky nemôže byť v budúcnosti"
     *   }
     *
     * @param id       The ID of the book to borrow.
     * @param borrowed A valid Borrowed object with borrowing details.
     * @return
     * - 200 OK if the book was successfully marked as borrowed.
     * - 400 Bad Request if validation fails (e.g., future date in "From").
     * - 404 Not Found if the book with the given ID doesn't exist.
     * - 500 Internal Server Error on unexpected failure.
     */
    @PatchMapping("/book/{id}/borrow")
    public ResponseEntity<Void> borrowBook(@PathVariable Long id, @RequestBody @Valid Borrowed borrowed) {
        logger.info("Request to borrow book with ID {}: {}", id, borrowed);
        try {
            boolean success = libraryService.borrowBook(id, borrowed);
            if (success) {
                logger.info("Book with ID {} borrowed successfully: {}", id, borrowed);
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Book with ID {} not found for borrowing", id);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error while borrowing book with ID {}: {}", id, borrowed, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PATCH /library/book/{id}/return
     * Marks a book as returned.
     *
     * @param id The ID of the book to return.
     * @return 200 OK if returned successfully, 404 Not Found if book doesn't exist, 500 Internal Server Error on failure.
     */
    @PatchMapping("/book/{id}/return")
    public ResponseEntity<Void> returnBook(@PathVariable Long id) {
        logger.info("Request to return book with ID {}", id);
        try {
            Boolean success = libraryService.returnBook(id);
            if (success) {
                logger.info("Book with ID {} successfully marked as returned", id);
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Book with ID {} not found for return", id);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error while returning book with ID {}: ", id, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
