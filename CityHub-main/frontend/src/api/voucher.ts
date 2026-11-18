import { request } from './request'
import type { Voucher } from '@/types'

export const voucherAPI = {
  add: (voucher: unknown) => request<null>({ url: '/voucher', method: 'POST', data: voucher }),

  addSeckill: (voucher: unknown) =>
    request<null>({ url: '/voucher/seckill', method: 'POST', data: voucher }),

  listByShop: (shopId: number | string) => request<Voucher[]>({ url: `/voucher/list/${shopId}` }),

  detail: (id: number | string) => request<Voucher>({ url: `/voucher/${id}` }),

  all: (type: number | null, current = 1) =>
    request<Voucher[]>({ url: '/voucher/all', params: { type, current } }),
}

export const seckillAPI = {
  order: (voucherId: number | string) =>
    request<number>({ url: `/voucher-order/seckill/${voucherId}`, method: 'POST' }),
}
