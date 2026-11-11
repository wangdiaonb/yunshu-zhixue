import { onBeforeUnmount, ref } from 'vue'

/**
 * 浏览器语音识别与播报（需求 3.8.2 / T3）：
 * 使用 Web Speech API，识别结果写入输入框；不支持时降级为文字作答并提示。
 */

interface SpeechRecognitionLike {
  lang: string
  continuous: boolean
  interimResults: boolean
  start(): void
  stop(): void
  abort(): void
  onresult: ((event: SpeechRecognitionEventLike) => void) | null
  onerror: ((event: { error: string }) => void) | null
  onend: (() => void) | null
}

interface SpeechRecognitionEventLike {
  resultIndex: number
  results: ArrayLike<{
    isFinal: boolean
    0: { transcript: string }
  }>
}

type SpeechRecognitionCtor = new () => SpeechRecognitionLike

function getRecognitionCtor(): SpeechRecognitionCtor | null {
  const w = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor
    webkitSpeechRecognition?: SpeechRecognitionCtor
  }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null
}

export function useSpeech() {
  const supported = Boolean(getRecognitionCtor()) || 'speechSynthesis' in window
  const recognizing = ref(false)
  const transcript = ref('')
  const error = ref('')

  let recognition: SpeechRecognitionLike | null = null

  function startRecognition(): void {
    if (recognizing.value) {
      return
    }
    const Recognition = getRecognitionCtor()
    if (!Recognition) {
      error.value = '当前浏览器不支持语音识别，请使用文字作答'
      return
    }
    recognition = new Recognition()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true
    transcript.value = ''
    error.value = ''

    recognition.onresult = (event) => {
      let text = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        text += event.results[i][0].transcript
      }
      transcript.value += text
    }
    recognition.onerror = (event) => {
      error.value = event.error === 'not-allowed' ? '麦克风权限被拒绝' : '语音识别失败，请改用文字作答'
      recognizing.value = false
    }
    recognition.onend = () => {
      recognizing.value = false
    }

    recognition.start()
    recognizing.value = true
  }

  function stopRecognition(): void {
    recognition?.stop()
    recognizing.value = false
  }

  /** 检查浏览器是否支持语音识别（用于页面提示降级文案）。 */
  function isRecognitionSupported(): boolean {
    return Boolean(getRecognitionCtor())
  }

  /** 语音播报（评分反馈等场景）。 */
  function speak(text: string): void {
    if (!('speechSynthesis' in window)) {
      return
    }
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(utterance)
  }

  onBeforeUnmount(() => {
    recognition?.abort()
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
  })

  return {
    supported,
    recognizing,
    transcript,
    error,
    startRecognition,
    stopRecognition,
    isRecognitionSupported,
    speak
  }
}
