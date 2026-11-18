// 后端统一响应结构（对应 com.hmdp.dto.Result）
export interface Result<T = unknown> {
  success: boolean
  errorMsg?: string | null
  data: T
  total?: number | null
}

// 当前登录用户（对应 UserDTO）
export interface UserDTO {
  id: number
  nickName?: string
  icon?: string
  name?: string
}

// 滚动分页结果（对应 ScrollResult）
export interface ScrollResult<T = unknown> {
  list: T[]
  minTime?: number | null
  offset?: number
}

// 商铺
export interface Shop {
  id: number
  name: string
  images?: string
  area?: string
  address?: string
  score?: number
  sold?: number
  averagePrice?: number
  typeName?: string
  typeId?: number
}

// 商铺分类
export interface ShopType {
  id: number
  name: string
}

// 优惠券
export interface Voucher {
  id: number
  type: number // 0 普通券，1 秒杀券
  title: string
  subTitle?: string
  payValue: number
  actualValue?: number
  condition?: number
  shopId?: number
  shopName?: string
  stock?: number
  beginTime?: string
  endTime?: string
  status?: number
}

// 笔记
export interface Blog {
  id: number
  userId: number
  name?: string
  icon?: string
  title: string
  content: string
  images?: string
  liked?: number
  comments?: number
  isLike?: boolean
  createTime?: string
  shopId?: number
}

// 用户详情（对应 UserInfo）
export interface UserInfo {
  id?: number
  introduce?: string
  city?: string
  followee?: number
  fans?: number
}

// 评论
export interface BlogComment {
  id: number
  userId: number
  name?: string
  icon?: string
  content: string
  createTime?: string
}

// 关注列表里的用户
export interface FollowUser {
  id: number
  nickName?: string
  icon?: string
  name?: string
}
