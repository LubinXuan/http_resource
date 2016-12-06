/*
 * A class that handles date parsing from text strings
 *
 */

package com.adtime.common.date.extrator.dateparser;

import com.adtime.common.date.extrator.regexparser.AnnotatedRegularExpressionBinding;
import com.adtime.common.date.extrator.regexparser.ArrayAnnotatedMatcher;
import com.adtime.common.date.extrator.regexparser.BindingCallback;
import com.adtime.common.date.util.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class DateParser {

    /* list of regular expressions in order of preferences for assignment */
    private static final String ANNOTATED_REGULAR_EXPRESSIONS = "annotated_regular_expressions";

    /* all annotated expressions share the same global array of bindings */
    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDINGS = "annotated_regular_expression_bindings";

    /* each reg ex binding has: */
    /* binding name */
    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDING_NAME = "binding_name";

    /* binding value */
    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDING_VALUE = "binding_value";

    /* a callback that modifies the context */
    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDING_CALLBACK = "callback";

    /* an JSONObject of arguments for the callback */
    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDING_CALLBACK_ARGS = "callback_arguments";

    /* each array annotated matcher shares the same ctx */
    private static final String ANNOTATED_REGULAR_EXPRESSION_CTX = "context";

    private static final String ANNOTATED_REGULAR_EXPRESSION_CTX_CLASSNAME = "context_class";

    /* an JSONObject of arguments for the callback */
    private static final String ANNOTATED_REGULAR_EXPRESSION_CTX_ARGS = "context_class_args";


    private static final String ANNOTATED_PRE_FIX_HANDLER = "date_off_prefix_handler";

    /*
     * takes a data file code.typically these are the two-letter locale
     * abbreviations as output by libyell (for internationalized date parsers).
     * i.e. everything in iso 693, except for Chinese, where there is 'zh-hans'
     * for Simplified Chinese and 'zh-hant' for Traditional Chinese. however,
     * they can actually be set to an arbitrary string, as long as the date file
     * for the code can be loaded.
     */
    String dateFileCode = "dateparser";

    /* shared bindings for the annotated regular expressions */
    List<BindingInfo> bindingInfo;

    /* list of annotated regular expression strings */
    String[] annRegExs;

    String ctxClass = null;

    Map ctxArgs = null;

    /* logger for diagnostic information */
    static Logger log = null;

    /* composite class for storing callback information */
    static class BindingInfo {
        String bindingName;

        String bindingValue;

        String cbkClass;

        Map cbkArgs;
    }

    private void init(InputStream stream) throws Exception {

        JSONObject obj = JSONObject.parseObject(FileUtil.getFileContents(stream));

        DateParserPreFixHandler.init(obj.getJSONArray(ANNOTATED_PRE_FIX_HANDLER));

		/* get a list of annotated regular expressions */
        JSONArray regularExpressions = obj
                .getJSONArray(ANNOTATED_REGULAR_EXPRESSIONS);
        this.annRegExs = new String[regularExpressions.size()];
        for (int i = 0; i < regularExpressions.size(); i++)
            this.annRegExs[i] = regularExpressions.getString(i);

		/* record the context information */
        JSONObject context = obj
                .getJSONObject(ANNOTATED_REGULAR_EXPRESSION_CTX);
        if (context != null) {
            /* class name is required */
            this.ctxClass = context
                    .getString(ANNOTATED_REGULAR_EXPRESSION_CTX_CLASSNAME);

			/* class args are optional */
            this.ctxArgs = ArrayAnnotatedMatcher.toMap(context
                    .getJSONObject(ANNOTATED_REGULAR_EXPRESSION_CTX_ARGS));
        }

		/* get the bindings for these reg ex's */
        JSONArray jsonBindings = obj
                .getJSONArray(ANNOTATED_REGULAR_EXPRESSION_BINDINGS);
        this.bindingInfo = new LinkedList<BindingInfo>();

        JSONObject binding;

        for (int i = 0; i < jsonBindings.size(); i++) {
            binding = jsonBindings.getJSONObject(i);
            BindingInfo bi = new BindingInfo();
            bi.bindingName = binding
                    .getString(ANNOTATED_REGULAR_EXPRESSION_BINDING_NAME);
            bi.bindingValue = binding
                    .getString(ANNOTATED_REGULAR_EXPRESSION_BINDING_VALUE);
            bi.cbkClass = binding
                    .getString(ANNOTATED_REGULAR_EXPRESSION_BINDING_CALLBACK);
            if (bi.cbkClass != null)
                bi.cbkArgs = ArrayAnnotatedMatcher
                        .toMap((binding
                                .getJSONObject(ANNOTATED_REGULAR_EXPRESSION_BINDING_CALLBACK_ARGS)));
            else
                bi.cbkArgs = null;

            this.bindingInfo.add(bi);
        }
    }

    private Object instantiateCallback(String className, Map args) {
        Class[] implementedClasses = {BindingCallback.class};

        return ArrayAnnotatedMatcher.instantiateClass(className, args, null,
                implementedClasses);
    }

    public synchronized DateParserMatcher compile() throws Exception {
        LinkedList<AnnotatedRegularExpressionBinding> bindingList = new LinkedList<AnnotatedRegularExpressionBinding>();

        for (BindingInfo bi : this.bindingInfo) {
            DateComponentParser cbk = (DateComponentParser) instantiateCallback(
                    bi.cbkClass, bi.cbkArgs);

            AnnotatedRegularExpressionBinding regExBinding = new AnnotatedRegularExpressionBinding(
                    bi.bindingName, bi.bindingValue, cbk, log);
            bindingList.add(regExBinding);
        }

        ArrayAnnotatedMatcher aam = new ArrayAnnotatedMatcher(this.annRegExs,
                bindingList);
        aam.setContext(this.ctxClass, this.ctxArgs);

        return new DateParserMatcher(aam);
    }

    public static void setLogger(Logger logger) {
        log = logger;
    }

    public DateParser(String filePath) {
        InputStream intputStream = DateParser.class.getClassLoader().getResourceAsStream(filePath);
        try {
            init(intputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DateParser() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(dateFileCode + ".json");
        try {
            init(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DateParser(File config) {
        try {
            init(new FileInputStream(config));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
