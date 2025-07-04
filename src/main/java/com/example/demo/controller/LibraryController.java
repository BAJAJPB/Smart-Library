package com.example.demo.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Book;
import com.example.demo.entity.BorrowRecord;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.BorrowRecordRepository;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class LibraryController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    // ✅ Get all books
    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        books.forEach(book ->
                System.out.println(book.getTitle() + " - Quantity: " + book.getQuantity())
        );
        return ResponseEntity.ok(books);
    }

    @GetMapping("/borrowed-books")
    public List<BorrowRecord> getAllBorrowedBooks() {
        return borrowRecordRepository.findAll();  // Ensure BorrowRecord includes issueDate
    }

    // ✅ Get all books borrowed by a student
    @GetMapping("/borrow/student/{studentId}")
    public ResponseEntity<List<BorrowRecord>> getBooksBorrowedByStudent(@PathVariable String studentId) {
        List<BorrowRecord> records = borrowRecordRepository.findByStudentId(studentId);
        return ResponseEntity.ok(records);
    }

    //  Borrow a book
    @PostMapping("/borrow")
    public ResponseEntity<String> borrowBook(@RequestParam Long bookId,
                                             @RequestParam String name,
                                             @RequestParam String email,
                                             @RequestParam String studentId) {
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (!optionalBook.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found.");
        }

        Book book = optionalBook.get();
        if (book.getQuantity() <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Book cannot be issued. Minimum quantity reached.");
        }

        // Validate student info
        List<BorrowRecord> existingRecords = borrowRecordRepository.findByStudentId(studentId);
        if (!existingRecords.isEmpty()) {
            BorrowRecord previous = existingRecords.get(0);
            if (!previous.getBorrowerName().equalsIgnoreCase(name) || !previous.getBorrowerEmail().equalsIgnoreCase(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Student ID already exists with different name or email. Please enter correct credentials.");
            }
        }

        // Save new borrow record
        BorrowRecord record = new BorrowRecord();
        record.setBook(book);
        record.setStudentId(studentId);
        record.setBorrowerName(name);
        record.setBorrowerEmail(email);
        record.setBorrowDate(LocalDate.now());
        borrowRecordRepository.save(record);

        // Reduce book quantity
        book.setQuantity(book.getQuantity() - 1);
        bookRepository.save(book);

        // Optional: log to file
        try (FileWriter writer = new FileWriter("issued_books.txt", true)) {
            writer.write("Issued to ID: " + studentId + ", Name: " + name + ", Email: " + email + ", Book: " + book.getTitle() + ", Date: " + LocalDate.now() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok("Book issued successfully.");
    }


    
    @PostMapping("/books")
    public ResponseEntity<String> addOrUpdateBook(@RequestParam String title,
                                                  @RequestParam String author,
                                                  @RequestParam int quantity) {
        // Check if book already exists
        Book existingBook = bookRepository.findByTitleAndAuthor(title, author);

        if (existingBook != null) {
            // Book exists → just increase quantity
            existingBook.setQuantity(existingBook.getQuantity() + quantity);
            bookRepository.save(existingBook);
            return ResponseEntity.ok("Book quantity updated successfully.");
        } else {
            // New book → create and save
            Book newBook = new Book();
            newBook.setTitle(title);
            newBook.setAuthor(author);
            newBook.setQuantity(quantity);
            newBook.setAvailable(true);
            bookRepository.save(newBook);
            return ResponseEntity.ok("New book added successfully.");
        }
    }

    
    @PostMapping("/return")
    public ResponseEntity<String> returnBook(@RequestParam String studentId,
                                             @RequestParam String name) {
        List<BorrowRecord> borrowRecords = borrowRecordRepository.findByStudentId(studentId);

        if (borrowRecords.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No borrowed books found.");
        }

        BorrowRecord recordToReturn = borrowRecords.get(0);

        if (!recordToReturn.getBorrowerName().equalsIgnoreCase(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Student name does not match with ID.");
        }

        Book book = recordToReturn.getBook();
        book.setQuantity(book.getQuantity() + 1);
        bookRepository.save(book);
        borrowRecordRepository.delete(recordToReturn);

        // ✅ Append return info to borrow-log.txt
        try (FileWriter writer = new FileWriter("book_return_data.txt", true)) {
        	logToFile("RETURNED by ID: " + studentId + ", Name: " + name + ", Book: " + book.getTitle() + ", Date: " + LocalDate.now());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!recordToReturn.getBorrowerName().equalsIgnoreCase(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Student name does not match with ID.");
        }



        return ResponseEntity.ok("Book returned successfully.");
    }



    @PostMapping("/books/delete")
    public ResponseEntity<String> deleteBookPartially(@RequestParam Long bookId, @RequestParam int count) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (!bookOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found.");
        }

        Book book = bookOpt.get();

        if (count > book.getQuantity()) {
            return ResponseEntity.badRequest().body("Cannot delete more copies than available.");
        }

        // Check if all copies are to be deleted
        if (count == book.getQuantity()) {
            if (!borrowRecordRepository.findByBook(book).isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete. Some copies are still issued.");
            }

            bookRepository.delete(book);
            return ResponseEntity.ok("All copies deleted.");
        } else {
            book.setQuantity(book.getQuantity() - count);
            bookRepository.save(book);
            return ResponseEntity.ok(count + " copies removed. Remaining: " + book.getQuantity());
        }
    }

    @DeleteMapping("/return/{borrowRecordId}")
    public ResponseEntity<String> returnBookById(@PathVariable Long borrowRecordId) {
        Optional<BorrowRecord> recordOpt = borrowRecordRepository.findById(borrowRecordId);
        if (recordOpt.isPresent()) {
            BorrowRecord record = recordOpt.get();

            // Get the book and increase its count
            Book book = record.getBook();
            book.setQuantity(book.getQuantity() + 1);
            bookRepository.save(book);

            // Delete the borrow record
            borrowRecordRepository.delete(record);

            return ResponseEntity.ok("Book returned successfully.");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Borrow record not found.");
    }
    
    



    // ✅ Logging utility
    private void logToFile(String logMessage) {
        try {
            File file = new File("book_return_data.txt");

            if (!file.exists()) {
                file.createNewFile(); // create the file if it doesn't exist
            }

            file.setWritable(true); // allow writing (just in case it's read-only)

            FileWriter fw = new FileWriter(file, true); // open in append mode
            fw.write(logMessage + System.lineSeparator()); // write the log
            fw.close(); // close writer

            file.setReadOnly(); // make the file read-only after writing

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}