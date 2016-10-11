local key = KEYS[1]
local domainTable = cjson.decode(KEYS[2])
for domain,count in pairs(domainTable) do
  local c = tonumber(redis.call('hget', key, domain));
  if c and c>0 then
    if c <= count then
      redis.call('hset', key, domain,0);
    else
      redis.call('hset', key, domain,c-count);
    end
  end
end
return 1;