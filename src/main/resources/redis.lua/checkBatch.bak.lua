local key = KEYS[1]
local domainTable = {}

for mu_id in string.gmatch(KEYS[2], "([^,]+)") do
  if mu_id then
    table.insert(domainTable, mu_id)
  end
end

local result = {}
local lockDomain = {}
local idx = 1
for i = 1, #domainTable do
  local pair = {}
  local val = domainTable[i]
  for mu_id in string.gmatch(val, "([^|]+)") do
    if mu_id then
      table.insert(pair, mu_id)
    end
  end
  local rev = 0
  local pairLen = table.getn(pair)
  if pairLen>0 and pairLen < 3 then
    result[i] = pair[1]..':'..rev
  end
  if pairLen > 2 then
    local id = pair[1]
    local domain = pair[2]
    local max = tonumber(pair[3])
    local not_lock = true
    for j = 1, #lockDomain do
      if lockDomain[j] == domain then
          result[i] = id..':'..rev
          not_lock = false
          break
      end
    end
    if not_lock then
      local v = redis.call('hget', key, domain)
      local c = 0
      if v then
        c = tonumber(v)
      end
      if c < max then
        local setV = c+1
        redis.call('hset', key, domain,setV)
        if setV >= max then
            lockDomain[idx] = domain
            idx = idx+1
            rev = 2
        else
          rev = 1
        end
      end
      result[i] = id..':'..rev
    end
  end
end
return table.concat(result,"|")