import { ApiError } from '@/api/request'
import type { ApiResponse } from '@/types/api'
import { getAccessToken } from '@/utils/token'

/**
 * POST + SSE 客户端（API 设计 §2.9）。
 *
 * <p>EventSource 仅支持 GET 且不能携带 Authorization 头，故使用 fetch + ReadableStream；
 * 事件帧：status / token / grade / error，注释行（: keep-alive）自动忽略。</p>
 */
export interface SseOptions {
  signal?: AbortSignal
  onEvent: (event: string, data: unknown) => void
}

export async function postSse(url: string, body: unknown, options: SseOptions): Promise<void> {
  const baseURL = import.meta.env.VITE_API_BASE_URL
  const response = await fetch(`${baseURL}${url}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${getAccessToken()}`
    },
    body: JSON.stringify(body),
    signal: options.signal
  })

  if (!response.ok || !response.body) {
    const apiError = await parseError(response)
    throw new ApiError(apiError.code, apiError.message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    buffer = dispatchFrames(buffer, options.onEvent)
  }

  // 处理末尾未以空行结束的帧
  if (buffer.trim()) {
    dispatchFrames(`${buffer}\n\n`, options.onEvent)
  }
}

/** 按 \n\n 切帧；返回剩余不完整内容 */
function dispatchFrames(buffer: string, onEvent: SseOptions['onEvent']): string {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const frames = normalized.split('\n\n')
  const rest = frames.pop() ?? ''

  for (const frame of frames) {
    let event = 'message'
    const dataLines: string[] = []
    for (const line of frame.split('\n')) {
      if (line.startsWith(':')) {
        continue
      }
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }
    if (dataLines.length === 0) {
      continue
    }
    const raw = dataLines.join('\n')
    try {
      onEvent(event, JSON.parse(raw))
    } catch {
      onEvent(event, raw)
    }
  }
  return rest
}

async function parseError(response: Response): Promise<ApiResponse<unknown>> {
  try {
    const body = (await response.json()) as ApiResponse<unknown>
    if (body && typeof body.code === 'number') {
      return body
    }
  } catch {
    // 忽略非 JSON 响应
  }
  return { code: response.status, message: '实时评分服务暂不可用，请稍后重试', data: null }
}
