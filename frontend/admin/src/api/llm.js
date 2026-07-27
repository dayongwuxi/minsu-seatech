import request from '@/utils/request'

// LLM 网关（Ollama Cloud），后端 /api/admin/llm/**
export const llmHealth = () => request.get('/llm/health')
export const listLlmModels = () => request.get('/llm/models')
// 房间多语言文案的生成/保存在 @/api/room（/rooms/{id}/i18n/**）
