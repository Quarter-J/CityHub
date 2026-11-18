import { request } from './request'
import type { Shop, ShopType } from '@/types'

export const shopAPI = {
  detail: (id: number | string) => request<Shop>({ url: `/shop/${id}` }),

  save: (shop: unknown) => request<null>({ url: '/shop', method: 'POST', data: shop }),

  update: (shop: unknown) => request<null>({ url: '/shop', method: 'PUT', data: shop }),

  byType: (typeId: number, current = 1, x?: number, y?: number) =>
    request<Shop[]>({ url: '/shop/of/type', params: { typeId, current, x, y } }),

  byName: (name: string, current = 1) =>
    request<Shop[]>({ url: '/shop/of/name', params: { name, current } }),
}

export const shopTypeAPI = {
  list: () => request<ShopType[]>({ url: '/shop-type/list' }),
}
