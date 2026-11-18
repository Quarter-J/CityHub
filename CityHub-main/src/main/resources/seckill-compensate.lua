-- KEYS[1] 库存 key
-- KEYS[2] 购买资格 key
-- KEYS[3] 待落库订单 key
-- ARGV[1] 用户 id
-- ARGV[2] 订单 id

local reservedOrderId = redis.call('hget', KEYS[2], ARGV[1])
if reservedOrderId ~= ARGV[2] then
    return 0
end

redis.call('hdel', KEYS[2], ARGV[1])
redis.call('hdel', KEYS[3], ARGV[1])
redis.call('incrby', KEYS[1], 1)
return 1
