package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.ChatMessageRequest;
import com.seatech.minsu.dto.CsSessionVO;
import com.seatech.minsu.entity.CsMessage;
import com.seatech.minsu.entity.CsSession;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.mapper.CsSessionMapper;
import com.seatech.minsu.service.CsMessageService;
import com.seatech.minsu.service.CsSessionService;
import com.seatech.minsu.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** 在线客服管理(会话/消息) */
@RestController
@RequestMapping("/api/admin/cs")
@RequiredArgsConstructor
public class AdminCsController {

    private final CsSessionService csSessionService;
    private final CsSessionMapper csSessionMapper;
    private final CsMessageService csMessageService;
    private final MemberService memberService;

    @GetMapping("/sessions")
    public Result<PageResult<CsSessionVO>> sessions(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String keyword) {
        IPage<CsSessionVO> page = csSessionMapper.selectAdminPage(
                new Page<>(current, size), status, keyword);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/sessions/{id}")
    public Result<CsSessionVO> sessionDetail(@PathVariable Long id) {
        CsSession session = requireSession(id);
        CsSessionVO vo = new CsSessionVO();
        vo.setId(session.getId());
        vo.setMemberId(session.getMemberId());
        vo.setAdminId(session.getAdminId());
        vo.setSourceChannel(session.getSourceChannel());
        vo.setStatus(session.getStatus());
        vo.setLastMessage(session.getLastMessage());
        vo.setLastTime(session.getLastTime());
        vo.setUnreadAdmin(session.getUnreadAdmin());
        vo.setUnreadMember(session.getUnreadMember());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        Member member = memberService.getById(session.getMemberId());
        if (member != null) {
            vo.setMemberName(member.getUsername());
            vo.setMemberAvatar(member.getAvatar());
        }
        return Result.ok(vo);
    }

    /** 拉取会话消息，同时清零客服未读 */
    @GetMapping("/sessions/{id}/messages")
    public Result<List<CsMessage>> messages(@PathVariable Long id,
                                            @RequestParam(required = false) Long afterId) {
        requireSession(id);
        List<CsMessage> list = csMessageService.lambdaQuery()
                .eq(CsMessage::getSessionId, id)
                .gt(afterId != null, CsMessage::getId, afterId)
                .orderByAsc(CsMessage::getId)
                .list();
        csSessionService.lambdaUpdate()
                .eq(CsSession::getId, id)
                .set(CsSession::getUnreadAdmin, 0)
                .update();
        return Result.ok(list);
    }

    /** 客服发送消息：接入会话并置为会话中 */
    @PostMapping("/sessions/{id}/messages")
    public Result<CsMessage> send(@PathVariable Long id, @RequestBody ChatMessageRequest req) {
        if (!StringUtils.hasText(req.getContent())) {
            throw new BusinessException("消息内容不能为空");
        }
        CsSession session = requireSession(id);
        if (session.getStatus() != null && session.getStatus() == 2) {
            throw new BusinessException("会话已结束");
        }
        Long adminId = AuthContext.get();
        CsMessage message = new CsMessage();
        message.setSessionId(id);
        message.setSenderType(2);
        message.setSenderId(adminId);
        message.setContentType(req.getContentType() == null ? 1 : req.getContentType());
        message.setContent(req.getContent());
        message.setIsRead(0);
        csMessageService.save(message);

        String last = req.getContent().length() > 200 ? req.getContent().substring(0, 200) : req.getContent();
        csSessionService.lambdaUpdate()
                .eq(CsSession::getId, id)
                .set(CsSession::getAdminId, adminId)
                .set(CsSession::getStatus, 1)
                .set(CsSession::getLastMessage, last)
                .set(CsSession::getLastTime, LocalDateTime.now())
                .setSql("unread_member = unread_member + 1")
                .update();
        return Result.ok(message);
    }

    @PutMapping("/sessions/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        CsSession session = requireSession(id);
        session.setStatus(2);
        session.setEndTime(LocalDateTime.now());
        csSessionService.updateById(session);
        return Result.ok();
    }

    private CsSession requireSession(Long id) {
        CsSession session = csSessionService.getById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }
}
