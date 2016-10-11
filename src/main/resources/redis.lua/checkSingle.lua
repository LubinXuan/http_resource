local v = redis.call('hget', KEYS[1], KEYS[2]);
local c = 0;
if v then
    c = tonumber(v)
end

local max = tonumber(KEYS[3])

if c < max then
  redis.call('hset', KEYS[1], KEYS[2],c+1);
  return 1;
else
  return 0;
end