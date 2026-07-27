package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.CheckinUserVO;
import com.seatech.minsu.dto.FavoriteVO;
import com.seatech.minsu.dto.MemberVO;
import com.seatech.minsu.dto.MyReviewVO;
import com.seatech.minsu.dto.PasswordUpdateRequest;
import com.seatech.minsu.dto.ProfileUpdateRequest;
import com.seatech.minsu.entity.Favorite;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.MemberType;
import com.seatech.minsu.mapper.CheckinRecordMapper;
import com.seatech.minsu.mapper.FavoriteMapper;
import com.seatech.minsu.mapper.ReviewMapper;
import com.seatech.minsu.service.FavoriteService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.MemberTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会员个人中心：资料/密码/入住记录/评价/收藏 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final MemberService memberService;
    private final MemberTypeService memberTypeService;
    private final FavoriteService favoriteService;
    private final FavoriteMapper favoriteMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final ReviewMapper reviewMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/api/user/profile")
    public Result<MemberVO> profile() {
        Member member = currentMember();
        String typeName = null;
        if (member.getMemberTypeId() != null) {
            MemberType type = memberTypeService.getById(member.getMemberTypeId());
            typeName = type == null ? null : type.getTypeName();
        }
        return Result.ok(MemberVO.of(member, typeName, true));
    }

    @PutMapping("/api/user/profile")
    public Result<Void> updateProfile(@RequestBody ProfileUpdateRequest req) {
        Member member = currentMember();
        member.setName(req.getName());
        member.setEmail(req.getEmail());
        member.setAvatar(req.getAvatar());
        memberService.updateById(member);
        return Result.ok();
    }

    @PutMapping("/api/user/password")
    public Result<Void> updatePassword(@RequestBody PasswordUpdateRequest req) {
        if (!StringUtils.hasText(req.getNewPassword())) {
            throw new BusinessException("新密码不能为空");
        }
        Member member = currentMember();
        if (!passwordEncoder.matches(req.getOldPassword(), member.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        memberService.updateById(member);
        return Result.ok();
    }

    @GetMapping("/api/user/checkins")
    public Result<PageResult<CheckinUserVO>> myCheckins(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        IPage<CheckinUserVO> page = checkinRecordMapper.selectUserPage(
                new Page<>(current, size), AuthContext.get());
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/api/user/reviews")
    public Result<PageResult<MyReviewVO>> myReviews(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size) {
        IPage<MyReviewVO> page = reviewMapper.selectMyPage(new Page<>(current, size), AuthContext.get());
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/api/user/favorites")
    public Result<PageResult<FavoriteVO>> myFavorites(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size) {
        IPage<FavoriteVO> page = favoriteMapper.selectMyPage(new Page<>(current, size), AuthContext.get());
        return Result.ok(PageResult.of(page));
    }

    @PostMapping("/api/favorites/{roomId}")
    public Result<Void> addFavorite(@PathVariable Long roomId) {
        Long memberId = AuthContext.get();
        long exists = favoriteService.lambdaQuery()
                .eq(Favorite::getMemberId, memberId)
                .eq(Favorite::getRoomId, roomId)
                .count();
        if (exists == 0) {
            Favorite favorite = new Favorite();
            favorite.setMemberId(memberId);
            favorite.setRoomId(roomId);
            favoriteService.save(favorite);
        }
        return Result.ok();
    }

    @DeleteMapping("/api/favorites/{roomId}")
    public Result<Void> removeFavorite(@PathVariable Long roomId) {
        favoriteService.lambdaUpdate()
                .eq(Favorite::getMemberId, AuthContext.get())
                .eq(Favorite::getRoomId, roomId)
                .remove();
        return Result.ok();
    }

    private Member currentMember() {
        Member member = memberService.getById(AuthContext.get());
        if (member == null) {
            throw new BusinessException("用户不存在");
        }
        return member;
    }
}
