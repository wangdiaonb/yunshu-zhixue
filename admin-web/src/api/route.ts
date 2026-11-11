import { http } from '@/api/request'
import type {
  RouteCard,
  RouteCourseOption,
  RouteDetail,
  RouteQuery,
  RouteSavePayload
} from '@/types/route'

export function listRoutes(query: RouteQuery = {}): Promise<RouteCard[]> {
  return http.get('/routes', { params: query })
}

export function createRoute(payload: RouteSavePayload): Promise<number> {
  return http.post('/routes', payload)
}

export function updateRoute(id: number, payload: RouteSavePayload): Promise<void> {
  return http.put(`/routes/${id}`, payload)
}

export function updateRouteStatus(id: number, status: number): Promise<void> {
  return http.post(`/routes/${id}/status`, { status })
}

export function deleteRoute(id: number): Promise<void> {
  return http.delete(`/routes/${id}`)
}

export function getRouteDetail(id: number): Promise<RouteDetail> {
  return http.get(`/routes/${id}/detail`)
}

export function listRouteCourseOptions(id: number, keyword?: string): Promise<RouteCourseOption[]> {
  return http.get(`/routes/${id}/course-options`, { params: { keyword: keyword || undefined } })
}

export function addRouteCourses(id: number, courseIds: number[]): Promise<void> {
  return http.post(`/routes/${id}/courses`, { courseIds })
}

export function removeRouteCourse(id: number, courseId: number): Promise<void> {
  return http.delete(`/routes/${id}/courses/${courseId}`)
}

export function sortRouteCourses(id: number, orderedIds: number[]): Promise<void> {
  return http.put(`/routes/${id}/courses/sort`, { orderedIds })
}
