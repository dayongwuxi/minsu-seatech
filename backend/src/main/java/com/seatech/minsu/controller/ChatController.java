package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.ChatMessageRequest;
import com.seatech.minsu.entity.CsMessage;
import com.seatech.minsu.entity.CsSession;
import com.seatech.minsu.service.CsMessageService;
import com.seatech.minsu.service.CsSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** 会员端在线客服（REST 轮询版，WebSocket 后续阶段接入） */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final CsSessionService csSessionService;
    private final CsMessageService csMessageService;

    /** 获取或创建我的会话 */
    @GetMapping("/session")
    public Result<CsSession> session() {
        Long memberId = AuthContext.get();
        CsSession session = csSessionService.lambdaQuery()
                .eq(CsSession::getMemberId, memberId)
                .ne(CsSession::getStatus, 2)
                .orderByDesc(CsSession::getId)
                .last("LIMIT 1")
                .one();
        if (session == null) {
            session = new CsSession();
            session.setMemberId(memberId);
            session.setSourceChannel("web");
            session.setStatus(0);
            session.setUnreadAdmin(0);
            session.setUnreadMember(0);
            session.setStartTime(LocalDateTime.now());
            csSessionService.save(session);
        }
        return Result.ok(session);
    }

    /** 增量拉取消息：afterId 之后的新消息，同时清零我的未读 */
    @GetMapping("/messages")
    public Result<List<CsMessage>> messages(@RequestParam Long sessionId,
                                            @RequestParam(required = false) Long afterId) {
        CsSession session = getOwnedSession(sessionId);
        List<CsMessage> list = csMessageService.lambdaQuery()
                .eq(CsMessage::getSessionId, session.getId())
                .gt(afterId != null, CsMessage::getId, afterId)
                .orderByAsc(CsMessage::getId)
                .list();
        csSessionService.lambdaUpdate()
                .eq(CsSession::getId, session.getId())
                .set(CsSession::getUnreadMember, 0)
                .update();
        return Result.ok(list);
    }

    /** 会员发送消息 */
    @PostMapping("/messages")
    public Result<CsMessage> send(@RequestBody ChatMessageRequest req) {
        if (req.getSessionId() == null || !StringUtils.hasText(req.getContent())) {
            throw new BusinessException("会话与消息内容不能为空");
        }
        CsSession session = getOwnedSession(req.getSessionId());
        CsMessage message = new CsMessage();
        message.setSessionId(session.getId());
        message.setSenderType(1);
        message.setSenderId(AuthContext.get());
        message.setContentType(req.getContentType() == null ? 1 : req.getContentType());
        message.setContent(req.getContent());
        message.setIsRead(0);
        csMessageService.save(message);

        String last = req.getContent().length() > 200 ? req.getContent().substring(0, 200) : req.getContent();
        csSessionService.lambdaUpdate()
                .eq(CsSession::getId, session.getId())
                .set(CsSession::getLastMessage, last)
                .set(CsSession::getLastTime, LocalDateTime.now())
                .setSql("unread_admin = unread_admin + 1")
                .update();
        return Result.ok(message);
    }

    private CsSession getOwnedSession(Long sessionId) {
        CsSession session = csSessionService.getById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!session.getMemberId().equals(AuthContext.get())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }
}
