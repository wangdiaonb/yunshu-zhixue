import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface PracticeSessionState {
  questionIds: number[]
  currentIndex: number
  answers: Record<number, string>
}

/**
 * 练习会话：断点续练依赖前端本地状态（服务端仅存答题流水）。
 */
export const usePracticeStore = defineStore('practice', () => {
  const session = ref<PracticeSessionState | null>(null)

  function start(questionIds: number[]): void {
    session.value = { questionIds, currentIndex: 0, answers: {} }
  }

  function recordAnswer(questionId: number, answer: string): void {
    if (session.value) {
      session.value.answers[questionId] = answer
    }
  }

  function moveTo(index: number): void {
    if (session.value && index >= 0 && index < session.value.questionIds.length) {
      session.value.currentIndex = index
    }
  }

  function clear(): void {
    session.value = null
  }

  return { session, start, recordAnswer, moveTo, clear }
})
