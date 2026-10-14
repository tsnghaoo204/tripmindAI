package com.tripmind.repositories;

import com.tripmind.entities.BlogPostUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface BlogPostUpvoteRepository extends JpaRepository<BlogPostUpvoteEntity, Long> {

    Optional<BlogPostUpvoteEntity> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    /** Trả 1 khi vừa thêm, 0 khi đã có sẵn (không ném lỗi trùng khoá khi bấm đồng thời). */
    @Modifying
    @Query(value = "INSERT INTO blog_post_upvotes (user_id, post_id) VALUES (:userId, :postId) "
            + "ON CONFLICT (user_id, post_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM BlogPostUpvoteEntity u WHERE u.userId = :userId AND u.postId = :postId")
    int deleteVote(@Param("userId") Long userId, @Param("postId") Long postId);

    long countByPostId(Long postId);

    @Query("SELECT u.postId FROM BlogPostUpvoteEntity u WHERE u.userId = :userId AND u.postId IN :postIds")
    Set<Long> findUpvotedPostIds(@Param("userId") Long userId, @Param("postIds") Iterable<Long> postIds);
}
