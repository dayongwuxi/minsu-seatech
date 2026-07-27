package com.seatech.minsu.controller.admin;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.AdminVO;
import com.seatech.minsu.dto.PasswordUpdateRequest;
import com.seatech.minsu.dto.ProfileUpdateRequest;
import com.seatech.minsu.entity.Admin;
import com.seatech.minsu.entity.Role;
import com.seatech.minsu.service.AdminService;
import com.seatech.minsu.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员个人中心 */
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminService adminService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<AdminVO> profile() {
        Admin admin = currentAdmin();
        String roleName = null;
        if (admin.getRoleId() != null) {
            Role role = roleService.getById(admin.getRoleId());
            roleName = role == null ? null : role.getRoleName();
        }
        return Result.ok(AdminVO.of(admin, roleName));
    }

    @PutMapping
    public Result<Void> update(@RequestBody ProfileUpdateRequest req) {
        Admin admin = currentAdmin();
        admin.setName(req.getName());
        admin.setPhone(req.getPhone());
        admin.setEmail(req.getEmail());
        admin.setAvatar(req.getAvatar());
        adminService.updateById(admin);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordUpdateRequest req) {
        if (!StringUtils.hasText(req.getNewPassword())) {
            throw new BusinessException("新密码不能为空");
        }
        Admin admin = currentAdmin();
        if (!passwordEncoder.matches(req.getOldPassword(), admin.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        admin.setPassword(passwordEncoder.encode(req.getNewPassword()));
        adminService.updateById(admin);
        return Result.ok();
    }

    private Admin currentAdmin() {
        Admin admin = adminService.getById(AuthContext.get());
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        return admin;
    }
}
