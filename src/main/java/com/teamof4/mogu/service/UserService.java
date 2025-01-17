package com.teamof4.mogu.service;

import com.teamof4.mogu.constants.CategoryNames;
import com.teamof4.mogu.dto.PostDto;
import com.teamof4.mogu.dto.UserDto;
import com.teamof4.mogu.dto.UserDto.*;
import com.teamof4.mogu.entity.Category;
import com.teamof4.mogu.entity.Post;
import com.teamof4.mogu.entity.User;
import com.teamof4.mogu.entity.UserSkill;
import com.teamof4.mogu.exception.image.ImageNotFoundException;
import com.teamof4.mogu.exception.post.CategoryNotFoundException;
import com.teamof4.mogu.exception.user.*;
import com.teamof4.mogu.repository.*;
import com.teamof4.mogu.security.TokenProvider;
import com.teamof4.mogu.util.certification.EmailService;
import com.teamof4.mogu.util.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

import static com.teamof4.mogu.constants.DefaultImageConstants.DEFAULT_PROFILE_IMAGE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ImageService imageService;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;
    private final EncryptionService encryptionService;
    private final EmailService emailService;
    private final TokenProvider tokenProvider;

    @Transactional
    public void create(SaveRequest requestDto) {
        checkDuplicatedForCreate(requestDto);
        requestDto.encryptPassword(encryptionService);

        User user = requestDto.toEntity();

        user.setImage(imageRepository.findById(DEFAULT_PROFILE_IMAGE_ID)
                .orElseThrow(() -> new ImageNotFoundException("기본 프로필 이미지를 찾지 못했습니다.")));
        userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public void checkDuplicatedForCreate(SaveRequest requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicatedEmailException();
        }
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            throw new DuplicatedNicknameException();
        }
        if (userRepository.existsByPhone(requestDto.getPhone())) {
            throw new DuplicatedPhoneException();
        }
    }

    @Transactional
    public String login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자 입니다."));

        if (user.getIsDeleted()) {
            throw new UserDeletedException();
        }
        if (!loginRequest.checkPassword(encryptionService, user.getPassword())) {
            throw new UserNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        String token = tokenProvider.create(user);

        return token;
    }


    @Transactional(readOnly = true)
    public MyInfoResponse getMyPageInformation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자 입니다."));
        MyInfoResponse myInfoResponse = user.toUserInfoResponse();

        return myInfoResponse;
    }

    @Transactional(readOnly = true)
    public LoginInfoResponse getLoginInformation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자 입니다."));
        LoginInfoResponse loginInfoResponse = user.toLoginInfoResponse();

        return loginInfoResponse;
    }

    @Transactional
    public void update(UpdateRequest updateRequest, MultipartFile profileImage, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자 입니다."));

        checkDuplicatedForUpdate(user, updateRequest);
        user.updateUser(updateRequest);
        updateSkills(user, updateRequest);
        //profileImage가 null이 아니면 새 이미지 저장하고 기존 이미지 삭제
        if (!profileImage.isEmpty()) {
            user.setImage(imageService.updateProfileImage(profileImage));
            imageService.deleteProfileImage(user.getImage());
        }
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void checkDuplicatedForUpdate(User user, UpdateRequest requestDto) {
        if (!user.getNickname().equals(requestDto.getNickname())
                && userRepository.existsByNickname(requestDto.getNickname())) {
            throw new DuplicatedNicknameException();
        }
        if (!user.getPhone().equals(requestDto.getPhone())
                && userRepository.existsByPhone(requestDto.getPhone())) {
            throw new DuplicatedPhoneException();
        }
    }


    @Transactional
    public void updateSkills(User user, UpdateRequest updateRequest) {
        List<UserSkill> originalUserSkills = user.getUserSkills();
        List<String> updatingSkillNames = updateRequest.getSkills();

        //기존 스킬들 중에서 삭제될 스킬들 filter
        originalUserSkills
                .stream()
                .filter(original -> updatingSkillNames
                        .stream()
                        .noneMatch(updating -> original.getSkill().getSkillName().equals(updating)))
                .forEach(userSkill -> userSkillRepository.delete(userSkill));
        //기존 스킬외에 추가될 스킬들 filter
        updatingSkillNames
                .stream()
                .filter(updating -> user.getUserSkillNames()
                        .stream()
                        .noneMatch(original -> updating.equals(original)))
                .map(skillName -> skillRepository.findBySkillName(skillName))
                .forEach(skill -> userSkillRepository.save(
                        UserSkill.of(user, skill.orElseThrow(() -> new UserSkillNotFoundException()))));
    }

    @Transactional
    public void delete(DeleteRequest requestDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자 입니다."));

        if (!requestDto.checkPassword(encryptionService, user.getPassword())) {
            throw new WrongPasswordException();
        }
        user.deleteUser();
        userRepository.save(user);
    }

    public String certificateByEmail(EmailCertificationRequest requestDto) {
        return emailService.sendCertificationEmail(requestDto.getEmail());
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest updatePasswordRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        if (!updatePasswordRequest.checkPassword(encryptionService, user.getPassword())) {
            throw new WrongPasswordException();
        }
        if (updatePasswordRequest.isAlreadyMyPassword()) {
            throw new AlreadyMyPasswordException();
        }
        updatePasswordRequest.encryptPassword(encryptionService);
        user.updatePassword(updatePasswordRequest.getNewPassword());
        userRepository.save(user);
    }

    @Transactional
    public void createNewPassword(CreatePasswordRequest requestDto) {
        User user = userRepository.findByEmailAndName(requestDto.getEmail(), requestDto.getName())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
        String newPassword = emailService.sendNewPasswordEmail(requestDto.getEmail());

        user.updatePassword(UserDto.encryptPassword(encryptionService, newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<PostDto.MyPageResponse> getPostsILiked(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
        Page<Post> posts = postRepository.findPostsILiked(pageable, user);

        return new PageImpl<>(toMyPageResponse(posts, user), pageable, posts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<PostDto.MyPageResponse> getPostsIReplied(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
        Page<Post> posts = postRepository.findPostsIReplied(pageable, user);

        return new PageImpl<>(toMyPageResponse(posts, user), pageable, posts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<PostDto.MyPageResponse> getMyParticipatingPosts(Long userId, Pageable pageable, CategoryNames categoryName) {
        Category category = categoryRepository.findByCategoryName(categoryName.getKorName())
                .orElseThrow(() -> new CategoryNotFoundException("해당 카테고리가 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
        Page<Post> posts = postRepository.findMyPostsByUserAndCategory(pageable, user, category);

        return new PageImpl<>(toMyPageResponse(posts, user), pageable, posts.getTotalElements());
    }

    public List<PostDto.MyPageResponse> toMyPageResponse(Page<Post> posts, User user) {
        return posts.stream()
                .map(post -> post.toMyPageResponse(user))
                .collect(Collectors.toList());
    }
}