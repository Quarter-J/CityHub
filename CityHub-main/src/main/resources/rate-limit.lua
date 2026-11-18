-- KEYS[1] 限流 key
-- ARGV[1] 当前时间（毫秒）
-- ARGV[2] 窗口长度（毫秒）
-- ARGV[3] 窗口内最大请求数
-- ARGV[4] 本次请求唯一标识

local now = tonumber(ARGV[1])
local windowMillis = tonumber(ARGV[2])
local maxRequests = tonumber(ARGV[3])
local cutoff = now - windowMillis

redis.call('zremrangebyscore', KEYS[1], '-inf', cutoff)
if redis.call('zcard', KEYS[1]) >= maxRequests then
    return 0
end

redis.call('zadd', KEYS[1], now, ARGV[4])
redis.call('pexpire', KEYS[1], windowMillis + 1000)
return 1
