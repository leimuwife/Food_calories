import request from '../request'
import type { FeedItem, FeedPublishParam, FeedListResult } from '../types'

export function getFeedList(pageNum: number, pageSize: number) {
  return request<FeedListResult>({
    url: '/api/feed/list',
    method: 'GET',
    data: { pageNum, pageSize },
  })
}

export function publishFeed(data: FeedPublishParam) {
  return request({
    url: '/api/feed/publish',
    method: 'POST',
    data,
  })
}

export function toggleFeedLike(feedId: number) {
  return request({
    url: `/api/feed/${feedId}/like`,
    method: 'POST',
  })
}

export function addFeedComment(feedId: number, content: string) {
  return request({
    url: `/api/feed/${feedId}/comment`,
    method: 'POST',
    data: { content },
  })
}