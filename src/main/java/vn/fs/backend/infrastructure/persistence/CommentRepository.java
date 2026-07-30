package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.fs.backend.domain.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>{

}
