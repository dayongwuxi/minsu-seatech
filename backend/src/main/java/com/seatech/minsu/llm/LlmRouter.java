package com.seatech.minsu.llm;

import com.seatech.minsu.config.LlmProperties;
import com.seatech.minsu.llm.dto.LlmModelInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 逻辑角色 → 具体云模型 的路由。业务侧只认角色（如 copywriter/default/fast），
 * 具体模型可在配置里随时切换。命中优先级：角色精确匹配 → 默认角色 → 路由表首项 → 降级模型。
 */
@Component
@RequiredArgsConstructor
public class LlmRouter {

    private final LlmProperties props;

    public String resolve(String role) {
        Map<String, String> models = props.getModels();
        if (StringUtils.hasText(role) && StringUtils.hasText(models.get(role))) {
            return models.get(role);
        }
        String byDefault = models.get(props.getDefaultRole());
        if (StringUtils.hasText(byDefault)) {
            return byDefault;
        }
        if (!models.isEmpty()) {
            return models.values().iterator().next();
        }
        return props.getFallbackModel();
    }

    public List<LlmModelInfo> list() {
        List<LlmModelInfo> out = new ArrayList<>();
        props.getModels().forEach((role, model) -> out.add(new LlmModelInfo(role, model)));
        return out;
    }
}
