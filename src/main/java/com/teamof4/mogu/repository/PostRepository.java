package com.teamof4.mogu.repository;

import com.teamof4.mogu.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAll(Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY size(p.likes) DESC, p.createdAt DESC ")
    Page<Post> findAllLikesDesc(Pageable pageable);
}
