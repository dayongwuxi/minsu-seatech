package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.MemberAdminVO;
import com.seatech.minsu.dto.MemberSaveRequest;
import com.seatech.minsu.dto.MemberVO;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.MemberType;
import com.seatech.minsu.mapper.MemberMapper;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.MemberTypeService;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会员管理 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private static final String DEFAULT_PASSWORD = "123456";

    private final MemberService memberService;
    private final MemberTypeService memberTypeService;
    private final MemberMapper memberMapper;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<PageResult<MemberAdminVO>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String phone,
                                                  @RequestParam(required = false) Long typeId,
                                                  @RequestParam(required = false) Integer status) {
        IPage<MemberAdminVO> page = memberMapper.selectAdminPage(
                new Page<>(current, size), name, phone, typeId, status);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<MemberVO> detail(@PathVariable Long id) {
        Member member = memberService.getById(id);
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }
        String typeName = null;
        if (member.getMemberTypeId() != null) {
            MemberType type = memberTypeService.getById(member.getMemberTypeId());
            typeName = type == null ? null : type.getTypeName();
        }
        return Result.ok(MemberVO.of(member, typeName, false));
    }

    @PostMapping
    public Result<Void> save(@RequestBody MemberSaveRequest req) {
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPhone())) {
            throw new BusinessException("用户名与手机号不能为空");
        }
        if (memberService.lambdaQuery().eq(Member::getUsername, req.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (memberService.lambdaQuery().eq(Member::getPhone, req.getPhone()).count() > 0) {
            throw new BusinessException("手机号已被注册");
        }
        Member member = new Member();
        member.setMemberNo(NoGenerator.gen("M"));
        member.setUsername(req.getUsername());
        member.setPhone(req.getPhone());
        member.setPassword(passwordEncoder.encode(
                StringUtils.hasText(req.getPassword()) ? req.getPassword() : DEFAULT_PASSWORD));
        member.setName(req.getName());
        member.setEmail(req.getEmail());
        member.setAvatar(req.getAvatar());
        member.setMemberTypeId(req.getMemberTypeId() == null ? 1L : req.getMemberTypeId());
        member.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        memberService.save(member);
        operationLogService.record(2, AuthContext.get(), "新增会员: " + member.getUsername());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MemberSaveRequest req) {
        Member member = memberService.getById(id);
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }
        if (StringUtils.hasText(req.getUsername())) {
            member.setUsername(req.getUsername());
        }
        if (StringUtils.hasText(req.getPhone())) {
            member.setPhone(req.getPhone());
        }
        if (StringUtils.hasText(req.getPassword())) {
            member.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        member.setName(req.getName());
        member.setEmail(req.getEmail());
        member.setAvatar(req.getAvatar());
        if (req.getMemberTypeId() != null) {
            member.setMemberTypeId(req.getMemberTypeId());
        }
        if (req.getStatus() != null) {
            member.setStatus(req.getStatus());
        }
        memberService.updateById(member);
        operationLogService.record(3, AuthContext.get(), "修改会员: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        Member member = new Member();
        member.setId(id);
        member.setStatus(req.getStatus());
        memberService.updateById(member);
        operationLogService.record(3, AuthContext.get(), "修改会员状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除会员: " + id);
        return Result.ok();
    }
}
