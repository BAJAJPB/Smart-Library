package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Book;
import com.example.demo.entity.BorrowRecord;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    // Existing method
    List<BorrowRecord> findByStudentId(String studentId);

    // Add this new method to check if a book is borrowed
    List<BorrowRecord> findByBook(Book book);
}