local key = KEYS[1]
local domainTable = cjson.decode(KEYS[2])
local retValMap = {}
for domain,count_pair in pairs(domainTable) do
  local pair = {}
  for mu_id in string.gmatch(count_pair, "([^|]+)") do
    table.insert(pair, mu_id)
  end
  local count = tonumber(pair[1])
  local max = tonumber(pair[2])
  local c = tonumber(redis.call('hget', key, domain));
  if c and c>0 then
    if c < max then
      local maxRes = max - c
      local avaRes = count
      if count > maxRes then
        avaRes = maxRes
      end
      retValMap[domain] = avaRes
      redis.call('hset', key, domain,c+avaRes)
    end
  else
    local res
    if count <= max then
      res = count
    else
      res = max
    end
    retValMap[domain] = res
    redis.call('hset', key, domain,res)
  end
end
return cjson.encode(retValMap)