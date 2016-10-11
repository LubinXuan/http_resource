local c = tonumber(redis.call('hget', KEYS[1], KEYS[2]));
if c and c > 0 then
  redis.call('hset', KEYS[1], KEYS[2],c-1);
end
return 1;