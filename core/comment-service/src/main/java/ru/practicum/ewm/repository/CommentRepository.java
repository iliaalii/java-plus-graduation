package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<List<Comment>> findAllByEventIdAndDeletedFalseOrderByCreatedDesc(Long eventId);

    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.deleted = false")
    Optional<Comment> findByIdAndDeletedFalse(@Param("id") Long id);
}
