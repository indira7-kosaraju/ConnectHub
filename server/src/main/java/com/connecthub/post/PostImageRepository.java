package com.connecthub.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

    @Query("select i from PostImage i where i.post.id in :postIds order by i.position asc")
    List<PostImage> findByPostIds(Collection<UUID> postIds);
}
