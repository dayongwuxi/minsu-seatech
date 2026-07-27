package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.AdminVO;
import com.seatech.minsu.dto.RoleMenusRequest;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.Admin;
import com.seatech.minsu.entity.Menu;
import com.seatech.minsu.entity.OperationLog;
import com.seatech.minsu.entity.Role;
import com.seatech.minsu.entity.RoleMenu;
import com.seatech.minsu.entity.SysConfig;
import com.seatech.minsu.service.AdminService;
import com.seatech.minsu.service.MenuService;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.RoleMenuService;
import com.seatech.minsu.service.RoleService;
import com.seatech.minsu.service.SysConfigService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 系统管理：菜单/角色/管理员/系统参数/操作日志 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSystemController {

    private static final String DEFAULT_PASSWORD = "123456";

    private final MenuService menuService;
    private final RoleService roleService;
    private final RoleMenuService roleMenuService;
    private final AdminService adminService;
    private final SysConfigService sysConfigService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    // ---------------- 菜单 ----------------

    @GetMapping("/menus")
    public Result<List<Menu>> menus() {
        return Result.ok(menuService.lambdaQuery().orderByAsc(Menu::getSort).list());
    }

    @PostMapping("/menus")
    public Result<Void> saveMenu(@RequestBody Menu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        menuService.save(menu);
        operationLogService.record(2, AuthContext.get(), "新增菜单: " + menu.getMenuName());
        return Result.ok();
    }

    @PutMapping("/menus/{id}")
    public Result<Void> updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        operationLogService.record(3, AuthContext.get(), "修改菜单: " + id);
        return Result.ok();
    }

    @DeleteMapping("/menus/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除菜单: " + id);
        return Result.ok();
    }

    // ---------------- 角色 ----------------

    @GetMapping("/roles")
    public Result<List<Role>> roles() {
        return Result.ok(roleService.list());
    }

    @PostMapping("/roles")
    public Result<Void> saveRole(@RequestBody Role role) {
        roleService.save(role);
        operationLogService.record(2, AuthContext.get(), "新增角色: " + role.getRoleName());
        return Result.ok();
    }

    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        operationLogService.record(3, AuthContext.get(), "修改角色: " + id);
        return Result.ok();
    }

    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        roleMenuService.lambdaUpdate().eq(RoleMenu::getRoleId, id).remove();
        operationLogService.record(4, AuthContext.get(), "删除角色: " + id);
        return Result.ok();
    }

    @GetMapping("/roles/{id}/menus")
    public Result<List<Long>> roleMenus(@PathVariable Long id) {
        return Result.ok(roleMenuService.lambdaQuery()
                .eq(RoleMenu::getRoleId, id)
                .list().stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList()));
    }

    /** 保存角色菜单(全量重建) */
    @PutMapping("/roles/{id}/menus")
    public Result<Void> saveRoleMenus(@PathVariable Long id, @RequestBody RoleMenusRequest req) {
        roleMenuService.lambdaUpdate().eq(RoleMenu::getRoleId, id).remove();
        if (req.getMenuIds() != null && !req.getMenuIds().isEmpty()) {
            List<RoleMenu> list = new ArrayList<>();
            for (Long menuId : req.getMenuIds()) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(id);
                rm.setMenuId(menuId);
                list.add(rm);
            }
            roleMenuService.saveBatch(list);
        }
        operationLogService.record(3, AuthContext.get(), "保存角色菜单: " + id);
        return Result.ok();
    }

    // ---------------- 管理员 ----------------

    @GetMapping("/admins")
    public Result<PageResult<AdminVO>> admins(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long roleId,
                                              @RequestParam(required = false) Integer status) {
        Page<Admin> page = adminService.lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Admin::getUsername, keyword)
                        .or()
                        .like(Admin::getName, keyword))
                .eq(roleId != null, Admin::getRoleId, roleId)
                .eq(status != null, Admin::getStatus, status)
                .orderByAsc(Admin::getId)
                .page(new Page<>(current, size));

        Map<Long, String> roleNames = roleService.list().stream()
                .collect(Collectors.toMap(Role::getId, Role::getRoleName, (a, b) -> a));
        Page<AdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(a -> AdminVO.of(a, roleNames.get(a.getRoleId())))
                .collect(Collectors.toList()));
        return Result.ok(PageResult.of(voPage));
    }

    @PostMapping("/admins")
    public Result<Void> saveAdmin(@RequestBody Admin admin) {
        if (!StringUtils.hasText(admin.getUsername())) {
            throw new BusinessException("账号不能为空");
        }
        if (adminService.lambdaQuery().eq(Admin::getUsername, admin.getUsername()).count() > 0) {
            throw new BusinessException("账号已存在");
        }
        admin.setId(null);
        admin.setPassword(passwordEncoder.encode(
                StringUtils.hasText(admin.getPassword()) ? admin.getPassword() : DEFAULT_PASSWORD));
        if (admin.getStatus() == null) {
            admin.setStatus(1);
        }
        adminService.save(admin);
        operationLogService.record(2, AuthContext.get(), "新增管理员: " + admin.getUsername());
        return Result.ok();
    }

    @PutMapping("/admins/{id}")
    public Result<Void> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        admin.setId(id);
        if (StringUtils.hasText(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(null);
        }
        adminService.updateById(admin);
        operationLogService.record(3, AuthContext.get(), "修改管理员: " + id);
        return Result.ok();
    }

    @PutMapping("/admins/{id}/status")
    public Result<Void> adminStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setStatus(req.getStatus());
        adminService.updateById(admin);
        operationLogService.record(3, AuthContext.get(), "修改管理员状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/admins/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        if (id.equals(AuthContext.get())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        adminService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除管理员: " + id);
        return Result.ok();
    }

    // ---------------- 系统参数 ----------------

    @GetMapping("/configs")
    public Result<List<SysConfig>> configs() {
        return Result.ok(sysConfigService.list());
    }

    /** 批量保存：按 configKey 更新，不存在则新增 */
    @PutMapping("/configs")
    public Result<Void> saveConfigs(@RequestBody List<SysConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return Result.ok();
        }
        for (SysConfig config : configs) {
            if (!StringUtils.hasText(config.getConfigKey())) {
                continue;
            }
            SysConfig existing = sysConfigService.lambdaQuery()
                    .eq(SysConfig::getConfigKey, config.getConfigKey())
                    .one();
            if (existing == null) {
                config.setId(null);
                sysConfigService.save(config);
            } else {
                existing.setConfigValue(config.getConfigValue());
                if (StringUtils.hasText(config.getConfigName())) {
                    existing.setConfigName(config.getConfigName());
                }
                sysConfigService.updateById(existing);
            }
        }
        operationLogService.record(3, AuthContext.get(), "批量保存系统参数");
        return Result.ok();
    }

    // ---------------- 操作日志 ----------------

    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> logs(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) Integer logType,
                                                 @RequestParam(required = false) String keyword) {
        Page<OperationLog> page = operationLogService.lambdaQuery()
                .eq(logType != null, OperationLog::getLogType, logType)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(OperationLog::getContent, keyword)
                        .or()
                        .like(OperationLog::getOperatorName, keyword))
                .orderByDesc(OperationLog::getCreateTime)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @DeleteMapping("/logs/clear")
    public Result<Void> clearLogs() {
        operationLogService.remove(new QueryWrapper<>());
        operationLogService.record(4, AuthContext.get(), "清空操作日志");
        return Result.ok();
    }
}
