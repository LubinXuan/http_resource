package com.adtime.common.date.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lubin.Xuan on 2015/1/9.
 * ie.
 */
public class BbsTimeAnalyseUtil {
    private static BBSPubTime bbsPubTime;

    static {
        try {
            String json = FileUtil.getFileContents("bbsPubTime.json");
            bbsPubTime = JSON.parseObject(json, new TypeReference<BBSPubTime>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Matcher m;
    private BPat p;

    public BbsTimeAnalyseUtil(String matchStr) {
        if (StringUtils.isNotBlank(matchStr) && null != bbsPubTime && bbsPubTime.getPatternList().length > 0) {
            for (BPat _p : bbsPubTime.getPatternList()) {
                Matcher _m = _p.getPattern().matcher(matchStr);
                if (_m.find()) {
                    p = _p;
                    m = _m;
                    break;
                }
            }
        }
    }

    public boolean hasNext() {
        return null != m && m.find();

    }


    public static int getFiled(String type) {
        try {
            Field f = Calendar.class.getField(type);
            if (null != f) {
                return f.getInt(Calendar.getInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


    public Date getDate(Calendar now) {
        if (null == p) {
            return null;
        }
        Calendar l_cal = Calendar.getInstance();
        l_cal.setTimeInMillis(now.getTimeInMillis());
        boolean modified = false;
        for (BIdx i : p.getIdx()) {
            try {
                String off = m.group(i.getOff());
                boolean isDigits = NumberUtils.isDigits(off);
                String type = m.group(i.getType());
                BType bType = bbsPubTime.getType().get(type);
                if (bbsPubTime.getSpecific().keySet().contains(off)) {
                    float v = bbsPubTime.getSpecific().get(off);
                    l_cal.add(getFiled(bType.getMatrixField()), -(int) (v * bType.getMatrix()));
                    modified = true;
                } else if (isDigits) {
                    int v = NumberUtils.createInteger(off);
                    l_cal.add(getFiled(bType.getMatrixField()), -v * bType.getMatrix());
                    modified = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (modified) {
            return l_cal.getTime();
        } else {
            return null;
        }
    }

    private static class BBSPubTime {
        private BPat[] patternList;
        private Map<String, BType> type;
        private Map<String, Float> specific;

        public BPat[] getPatternList() {
            return patternList;
        }

        public void setPatternList(BPat[] patternList) {
            this.patternList = patternList;
        }

        public Map<String, BType> getType() {
            return type;
        }

        public void setType(Map<String, BType> type) {
            this.type = type;
        }

        public Map<String, Float> getSpecific() {
            return specific;
        }

        public void setSpecific(Map<String, Float> specific) {
            this.specific = specific;
        }
    }

    private static class BPat {

        private Pattern pattern;
        private BIdx[] idx;

        public Pattern getPattern() {
            return pattern;
        }

        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public BIdx[] getIdx() {
            return idx;
        }

        public void setIdx(BIdx[] idx) {
            this.idx = idx;
        }
    }


    private static class BIdx {
        private int off;
        private int type;

        public int getOff() {
            return off;
        }

        public void setOff(int off) {
            this.off = off;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    private static class BType {

        private String field;
        private int matrix;
        private String matrixField;

        public String getMatrixField() {
            return matrixField;
        }

        public void setMatrixField(String matrixField) {
            this.matrixField = matrixField;
        }

        public int getMatrix() {
            return matrix;
        }

        public void setMatrix(int matrix) {
            this.matrix = matrix;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

}
