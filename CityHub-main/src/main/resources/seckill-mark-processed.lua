-- KEYS[1] 待落库订单 key
-- ARGV[1] 用户 id
-- ARGV[2] 订单 id

if redis.call('hget', KEYS[1], ARGV[1]) == ARGV[2] then
    return redis.call('hdel', KEYS[1], ARGV[1])
end
return 0
