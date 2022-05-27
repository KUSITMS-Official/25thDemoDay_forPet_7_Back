package com.kusitms.forpet.controller;

import com.kusitms.forpet.domain.Community;
import com.kusitms.forpet.domain.User;
import com.kusitms.forpet.dto.response.ApiResponse;
import com.kusitms.forpet.dto.CommunityDto;
import com.kusitms.forpet.security.TokenProvider;
import com.kusitms.forpet.service.CommunityService;
import com.kusitms.forpet.service.S3Uploader;
import com.kusitms.forpet.service.UserService;
import com.kusitms.forpet.util.HeaderUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommunityController {
    private final TokenProvider tokenProvider;
    private final UserService userService;
    private final CommunityService communityService;
    private final S3Uploader s3Uploader;

    /**
     * 전체 게시글 조회 (동네에 해당하는 게시글만)
     */
    @GetMapping("")
    public ApiResponse getAllPostListByAddress(HttpServletRequest request) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        // 사용자 주소를 가져오기
        User user = userService.findByUserId(userId);
        String[] addressList = user.getAddress().split("#");

        // 인기 7개(id, 카테고리, 글 제목, 좋아요수, 댓글수)
        // 모임 7개(id, 글 제목, 좋아요수, 댓글수)
        // 나눔 9개 (id, 사진, 글 제목)
        // 자랑 9개 (id, 사진, 글 제목)

        Map<String, List<CommunityDto.CommunityResponse>> postList = new HashMap<>();

        List<Community> popularList = communityService.findOrderByThumbsUpAndAddress(addressList);
        popularList = getCommunityListBySize(popularList, popularList.size() < 7 ? popularList.size() : 7);

        List<Community> meetingList = communityService.findByCategoryAndAddress("meeting", addressList);
        meetingList = getCommunityListBySize(meetingList, meetingList.size() < 7 ? meetingList.size() : 7);

        List<Community> sharingList = communityService.findByCategoryAndAddress("sharing", addressList);
        sharingList = getCommunityListBySize(sharingList, sharingList.size() < 9 ? sharingList.size() : 9);

        List<Community> boastingList = communityService.findByCategoryAndAddress("boasting", addressList);
        boastingList = getCommunityListBySize(boastingList, boastingList.size() < 9 ? boastingList.size() : 9);

        // domain -> dto
        List<CommunityDto.CommunityResponse> popularResponseList = popularList.stream()
                .map(m -> new CommunityDto.CommunityResponse(m.getPostId(), m.getUser().getUserId(), m.getTitle(), m.getLikesCommList().size(), m.getBookmarkCommList().size(), m.getImageUrlList().split("#"), m.getCategory(), 2))
                .collect(Collectors.toList());

        List<CommunityDto.CommunityResponse> meetingResponseList = meetingList.stream()
                .map(m -> new CommunityDto.CommunityResponse(m.getPostId(), m.getUser().getUserId(), m.getTitle(), m.getLikesCommList().size(), m.getBookmarkCommList().size(), m.getImageUrlList().split("#"), m.getCategory(), 2))
                .collect(Collectors.toList());

        List<CommunityDto.CommunityResponse> sharingResponseList = sharingList.stream()
                .map(m -> new CommunityDto.CommunityResponse(m.getPostId(), m.getUser().getUserId(), m.getTitle(), m.getLikesCommList().size(), m.getBookmarkCommList().size(), m.getImageUrlList().split("#"), m.getCategory(), 2))
                .collect(Collectors.toList());

        List<CommunityDto.CommunityResponse> boastingResponseList = boastingList.stream()
                .map(m -> new CommunityDto.CommunityResponse(m.getPostId(), m.getUser().getUserId(), m.getTitle(), m.getLikesCommList().size(), m.getBookmarkCommList().size(), m.getImageUrlList().split("#"), m.getCategory(), 2))
                .collect(Collectors.toList());

        postList.put("popular", popularResponseList);
        postList.put("meeting", meetingResponseList);
        postList.put("sharing", sharingResponseList);
        postList.put("boasting", boastingResponseList);

        return ApiResponse.success("data", postList);
    }

    /*
     list를 개수만큼
     */
    public List<Community> getCommunityListBySize(List<Community> list, int size) {
        return list.subList(0, size);
    }


    /**
     * 검색
     */
    @GetMapping("/search")
    public ApiResponse search(HttpServletRequest request,
                              @RequestParam String keyword,
                              @RequestParam int page,
                              @RequestParam int size) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        // 사용자 주소를 가져오기
        User user = userService.findByUserId(userId);
        String[] addressList = user.getAddress().split("#");

        System.out.println(" >>>>>>>>>>>>>> " + keyword);

        // 페이지네이션
        List<Community> searchList = communityService.findByKeyword(keyword, addressList, page, size);

        for(Community c : searchList) {
            System.out.println(" >>>>>>>>>>>>>> " + c.getTitle());
        }
        List<CommunityDto.CommunityListResponse> searchResponseList = searchList.stream()
                .map(m -> new CommunityDto.CommunityListResponse(m.getPostId(), new CommunityDto.Writer(m.getUser().getUserId()
                        , (m.getUser().getCustomImageUrl() == null ? m.getUser().getImageUrl() : m.getUser().getCustomImageUrl())
                        , m.getUser().getNickname())
                        , m.getTitle(), m.getLikesCommList().size()
                        , m.getBookmarkCommList().size()
                        , (m.getImageUrlList() == null ? null : m.getImageUrlList().split("#"))
                        , m.getCategory()
                        , m.getCommentCommList().size()
                        , setCreateDate(m.getDate())))
                .collect(Collectors.toList());


        return ApiResponse.success("data", searchResponseList);
    }

    /**
     * 카테고리 리스트
     */
    @GetMapping("/list")
    public ApiResponse getAllPostListByCategory(HttpServletRequest request,
                                                @RequestParam(value="category") String category,
                                                @RequestParam int page,
                                                @RequestParam int size) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        // 사용자 주소를 가져오기
        User user = userService.findByUserId(userId);
        String[] addressList = user.getAddress().split("#");

        if(category.equals("all")) {
            // 카테고리가 전체라면 LIKE 검색이 안되게
            category = "";
        }

        List<Community> categoryList = communityService.findByCategoryAndAddress(category, addressList, page, size);

        System.out.println(">>>>>>>> size : " + categoryList.size());
        // domain -> dto
        List<CommunityDto.CommunityListResponse> categoryResponseList = categoryList.stream()
                .map(m -> new CommunityDto.CommunityListResponse(m.getPostId(), new CommunityDto.Writer(m.getUser().getUserId()
                        , (m.getUser().getCustomImageUrl() == null ? m.getUser().getImageUrl() : m.getUser().getCustomImageUrl())
                        , m.getUser().getNickname()), m.getTitle()
                        , m.getLikesCommList().size()
                        , m.getBookmarkCommList().size()
                        , (m.getImageUrlList() == null ? null : m.getImageUrlList().split("#"))
                        , m.getCategory()
                        , m.getCommentCommList().size()
                        , setCreateDate(m.getDate())))
                .collect(Collectors.toList());

        return ApiResponse.success("data", categoryResponseList);
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{postId}")
    public ApiResponse getPostById(HttpServletRequest request,
                                   @PathVariable Long postId) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        User user = userService.findByUserId(userId);
        Community community = communityService.findCommunityById(postId);

        // 사용자 프로필
        String profile_image;
        if(community.getUser().getCustomImageUrl() != null) {
            profile_image = community.getUser().getCustomImageUrl();
        } else {
            profile_image = community.getUser().getImageUrl();
        }

        boolean isWriter = false;
        if(community.getUser().getUserId() == user.getUserId()) {
            isWriter = true;
        }

        boolean isLike = false;
        if(communityService.getLikes(user, community)) {
            isLike = true;
        }

        boolean isBookMark = false;
        if(communityService.getBookMark(user, community)) {
            isBookMark = true;
        }

        CommunityDto.CommunityDetailResponse communityResponse = new CommunityDto.CommunityDetailResponse(
                community.getPostId(), new CommunityDto.Writer(community.getUser().getUserId(), profile_image, community.getUser().getNickname()), isWriter, community.getTitle(), community.getContent(), setCreateDate(community.getDate()), community.getLikesCommList().size(),  community.getBookmarkCommList().size()
                , (community.getImageUrlList() == null ? null : community.getImageUrlList().split("#")), community.getCategory(), community.getCommentCommList().size(), isLike, isBookMark);
        return ApiResponse.success("data", communityResponse);
    }


    /**
     * 게시글 수정
     */
    @PutMapping("{postId}")
    public ApiResponse updatePost(HttpServletRequest request,
                                  @PathVariable(value="postId") Long postId,
                                  @RequestPart(value = "community_request") CommunityDto.CommunityRequest requestDto,
                                  @RequestPart(value = "imageList") List<MultipartFile> multipartFile) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        // 글쓴이의 주소를 가져오기
        User user = userService.findByUserId(userId);

        List<String> imageNameList = new ArrayList<>();
        if(multipartFile != null) {
            imageNameList = s3Uploader.uploadImage(multipartFile);
        }

        Long id = communityService.updatePost(postId, user, requestDto, imageNameList);

        return ApiResponse.updated("post_id", id);
    }
    /**
     * 게시글 삭제
     */
    @DeleteMapping("{postId}")
    public ApiResponse deletePost(@PathVariable(value="postId") Long postId) {
        return ApiResponse.success("post_id", communityService.deletePost(postId));

    }

    /**
    * 게시글 등록
     */
    @PostMapping("")
    public ApiResponse createPost(HttpServletRequest request,
                                  @RequestPart(value = "community_request") CommunityDto.CommunityRequest requestDto,
                                  @RequestPart(value = "imageList", required=false) List<MultipartFile> multipartFile) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        // 글쓴이의 주소를 가져오기
        User user = userService.findByUserId(userId);

        List<String> imageNameList = new ArrayList<>();
        if(multipartFile != null) {
            imageNameList = s3Uploader.uploadImage(multipartFile);
        }

        Long id = communityService.createPost(user, requestDto, imageNameList);

        return ApiResponse.created("post_id", id);
    }

    /**
     * 게시글 좋아요
     */
    @PostMapping("/{postId}/like")
    public ApiResponse createLikes(HttpServletRequest request,
                                   @PathVariable(value="postId")Long postId) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        User user = userService.findByUserId(userId);
        int cnt = communityService.saveLikes(user, postId);

        return ApiResponse.success("likes", cnt);
    }

    /**
     * 게시글 좋아요 취소
     */
    @DeleteMapping("/{postId}/like")
    public ApiResponse deleteLikes(HttpServletRequest request,
                                   @PathVariable(value="postId")Long postId) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        User user = userService.findByUserId(userId);
        int cnt = communityService.deleteLikes(user, postId);

        return ApiResponse.success("likes", cnt);
    }

    /**
     * 게시글 북마크
     */
    @PostMapping("/{postId}/bookmark")
    public ApiResponse createBookmark(HttpServletRequest request,
                                   @PathVariable(value="postId")Long postId) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        User user = userService.findByUserId(userId);
        int cnt = communityService.saveBookMark(user, postId);

        return ApiResponse.success("bookmark", cnt);
    }

    /**
     * 게시글 북마크 취소
     */
    @DeleteMapping("/{postId}/bookmark")
    public ApiResponse deleteBookmark(HttpServletRequest request,
                                   @PathVariable(value="postId")Long postId) {
        String accessToken = HeaderUtil.getAccessToken(request);
        Long userId = tokenProvider.getUserIdFromToken(accessToken);

        User user = userService.findByUserId(userId);
        int cnt = communityService.deleteBookMark(user, postId);

        return ApiResponse.success("bookmark", cnt);
    }

    //==LocalDateTime 커스텀==//
    public String setCreateDate(LocalDateTime localDateTime) {
        //월, 일
        String month = String.valueOf(localDateTime.getMonthValue());
        String day = String.valueOf(localDateTime.getDayOfMonth());
        //시, 분
        String hour = String.valueOf(localDateTime.getHour());
        String min = String.format("%02d", localDateTime.getMinute());
        return month + "/" + day + " " + hour + ":" + min;
    }
}
