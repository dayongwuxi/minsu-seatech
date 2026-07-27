package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.entity.CsQuickReply;
import com.seatech.minsu.service.CsQuickReplyService;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
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

/** 客服快捷回复管理 */
@RestController
@RequestMapping("/api/admin/cs/quick-replies")
@RequiredArgsConstructor
public class AdminQuickReplyController {

    private final CsQuickReplyService csQuickReplyService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<CsQuickReply>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword) {
        Page<CsQuickReply> page = csQuickReplyService.lambdaQuery()
                .like(StringUtils.hasText(keyword), CsQuickReply::getQuestion, keyword)
                .orderByAsc(CsQuickReply::getSort)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Void> save(@RequestBody CsQuickReply quickReply) {
        csQuickReplyService.save(quickReply);
        operationLogService.record(2, AuthContext.get(), "新增快捷回复");
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CsQuickReply quickReply) {
        quickReply.setId(id);
        csQuickReplyService.updateById(quickReply);
        operationLogService.record(3, AuthContext.get(), "修改快捷回复: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        csQuickReplyService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除快捷回复: " + id);
        return Result.ok();
    }
}
