package com.seatech.minsu.controller.admin;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.JwtUtil;
import com.seatech.minsu.dto.AdminLoginRequest;
import com.seatech.minsu.dto.AdminLoginVO;
import com.seatech.minsu.dto.AdminVO;
import com.seatech.minsu.entity.Admin;
import com.seatech.minsu.entity.Role;
import com.seatech.minsu.service.AdminService;
import com.seatech.minsu.service.CaptchaService;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 管理端登录 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;
    private final RoleService roleService;
    private final CaptchaService captchaService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody AdminLoginRequest req) {
        if (!captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        Admin admin = adminService.lambdaQuery()
                .eq(Admin::getUsername, req.getUsername())
                .one();
        if (admin == null || !passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        admin.setLastLoginTime(LocalDateTime.now());
        adminService.updateById(admin);
        operationLogService.record(1, admin.getId(), "登录系统");

        String roleName = null;
        if (admin.getRoleId() != null) {
            Role role = roleService.getById(admin.getRoleId());
            roleName = role == null ? null : role.getRoleName();
        }
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(jwtUtil.createToken(admin.getId(), JwtUtil.AUD_ADMIN));
        vo.setAdmin(AdminVO.of(admin, roleName));
        return Result.ok(vo);
    }
}
