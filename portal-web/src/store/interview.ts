import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface InterviewQuestion {
  order: number
  questionId?: number | null
  content: string
  type?: number
  difficulty?: number
  referencePoints?: string[]
}

export interface InterviewSessionState {
  sessionId: string
  subjectId: number
  difficulty: number
  questions: InterviewQuestion[]
  currentOrder: number
  answers: Record<number, string>
}

/**
 * AI 面试会话：未完成的面试仅存前端本地（D16），不跨设备续做；
 * 逐题评分与最终报告以后端为准，本 Store 只维护交互过程。
 */
export const useInterviewStore = defineStore('interview', () => {
  const session = ref<InterviewSessionState | null>(null)

  function start(state: Omit<InterviewSessionState, 'currentOrder' | 'answers'>): void {
    session.value = { ...state, currentOrder: 1, answers: {} }
  }

  function recordAnswer(order: number, answer: string): void {
    if (session.value) {
      session.value.answers[order] = answer
    }
  }

  function moveTo(order: number): void {
    if (session.value) {
      session.value.currentOrder = order
    }
  }

  function clear(): void {
    session.value = null
  }

  return { session, start, recordAnswer, moveTo, clear }
})
