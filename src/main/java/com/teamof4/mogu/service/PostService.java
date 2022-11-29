package com.teamof4.mogu.service;

import com.amazonaws.util.CollectionUtils;
import com.teamof4.mogu.dto.PostDto;
import com.teamof4.mogu.entity.*;
import com.teamof4.mogu.exception.category.CategoryNotFoundException;
import com.teamof4.mogu.exception.post.PostNotFoundException;
import com.teamof4.mogu.exception.user.UserNotFoundException;
import com.teamof4.mogu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class PostService {


    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ImagePostRepository imagePostRepository;
    private final ImageService imageService;

    public List<PostDto.Response> getPostList(Long categoryId) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(post -> !post.isDeleted())
                .filter(post -> post.getCategory().getId().equals(categoryId)).collect(Collectors.toList());

        List<PostDto.Response> responseList = new ArrayList<>();

        for (Post post : posts) {
            PostDto.Response response = PostDto.Response.builder()
                    .post(post)
                    .images(getImages(post.getId())).build();
            responseList.add(response);
        }
        return responseList;
    }

    public PostDto.Response getPostDetails(Long postId, Long currentUserId) {

        Post post = getPost(postId);

        if (!post.getUser().getId().equals(currentUserId)) {
            post.addViewCount(post.getView());
            postRepository.save(post);
        }

        List<Image> images = getImages(postId);

        return PostDto.Response.builder()
                .post(post)
                .images(images).build();

    }

    public List<Image> getImages(Long postId) {
        List<ImagePost> imagePosts = imagePostRepository.findAllByPostId(postId);

        List<Image> images = new ArrayList<>();

        if (!CollectionUtils.isNullOrEmpty(imagePosts)) {
            images = imagePosts.stream().map(ImagePost::getImage).collect(Collectors.toList());

        }
        return images;
    }

    @Transactional
    public Long savePost(PostDto.SaveRequest requestDTO) {

        Post post = requestDTO.toEntity(getUser(requestDTO.getUserId()), getCategory(requestDTO.getCategoryId()));

        postRepository.save(post);

        if (!CollectionUtils.isNullOrEmpty(requestDTO.getMultipartFiles())) {
            saveImage(requestDTO.getMultipartFiles(), post);
        }

        return post.getId();

    }

    @Transactional
    public Long updatePost(Long postId, PostDto.UpdateRequest requestDTO) {

        Post post = getPost(postId);
        post.updatePost(requestDTO.getTitle(), requestDTO.getContent());

        List<ImagePost> imagePosts = imagePostRepository.findAllByPostId(postId);

        List<MultipartFile> multipartFiles = requestDTO.getMultipartFiles();

        updatePostImageProcess(post, multipartFiles, imagePosts);


        postRepository.save(post);

        return post.getId();
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = getPost(postId);
        post.changeStatus();

        postRepository.save(post);
    }

    private void updatePostImageProcess(Post post, List<MultipartFile> multipartFiles, List<ImagePost> imagePosts) {

        if (CollectionUtils.isNullOrEmpty(imagePosts)) {
            if (!CollectionUtils.isNullOrEmpty(multipartFiles)) {
                saveImage(multipartFiles, post);
            }
        } else {

            for (ImagePost imagePost : imagePosts) {
                imagePostRepository.deleteById(imagePost.getId());
            }

            List<Image> images = imagePosts.stream().map(ImagePost::getImage).collect(Collectors.toList());

            for (Image image : images) {
                imageService.deleteImage(image);
            }

            if (!CollectionUtils.isNullOrEmpty(multipartFiles)) {
                saveImage(multipartFiles, post);
            }
        }
    }

    @Transactional
    public void saveImage(List<MultipartFile> images, Post post) {
        for (MultipartFile image : images) {
            Image imageEntity = imageService.savePostImage(image);
            imagePostRepository.save(ImagePost.createImagePost(imageEntity, post));
        }
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("존재하지 않는 카테고리입니다."));
    }

    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("존재하지 않는 게시글입니다."));
    }
}