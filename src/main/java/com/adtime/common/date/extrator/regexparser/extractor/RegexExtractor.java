/*
 * A class that handles date parsing from text strings
 *
 */

package com.adtime.common.date.extrator.regexparser.extractor;

import com.adtime.common.date.extrator.regexparser.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.logging.Logger;


public class RegexExtractor {
    /* where all JSON files are loaded from */
    private static String CONFIG_PATH = "/home/y/conf/lf_cricket";

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

    private static HashMap<String, RegexExtractor> instances = new HashMap<String, RegexExtractor>();

    /*
     * takes a data file code.typically these are the two-letter locale
     * abbreviations as output by libyell (for internationalized date parsers).
     * i.e. everything in iso 693, except for Chinese, where there is 'zh-hans'
     * for Simplified Chinese and 'zh-hant' for Traditional Chinese. however,
     * they can actually be set to an arbitrary string, as long as the date file
     * for the code can be loaded.
     */
    String dataFileCode;

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

    private RegexExtractor(String dataFileCode) throws Exception {
        this.dataFileCode = dataFileCode;

        DataFileConfiguration dconf = DataFileReader.getConfiguration(
                dataFileCode, false);

		/* get a list of annotated regular expressions */
        JSONArray regularExpressions = dconf
                .getArray(ANNOTATED_REGULAR_EXPRESSIONS);
        this.annRegExs = new String[regularExpressions.size()];
        for (int i = 0; i < regularExpressions.size(); i++) {
            this.annRegExs[i] = regularExpressions.getString(i);
        }

		/* record the context information */
        JSONObject context = dconf.getObject(ANNOTATED_REGULAR_EXPRESSION_CTX);
        if (context != null) {
            /* class name is required */
            this.ctxClass = context
                    .getString(ANNOTATED_REGULAR_EXPRESSION_CTX_CLASSNAME);

			/* class args are optional */
            this.ctxArgs = ArrayAnnotatedMatcher.toMap(context
                    .getJSONObject(ANNOTATED_REGULAR_EXPRESSION_CTX_ARGS));
        }

		/* get the bindings for these reg ex's */
        JSONArray jsonBindings = dconf
                .getArray(ANNOTATED_REGULAR_EXPRESSION_BINDINGS);
        this.bindingInfo = new LinkedList<BindingInfo>();

        JSONObject binding = null;

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

    public synchronized ParserMatcher compile() throws Exception {
        LinkedList<AnnotatedRegularExpressionBinding> bindingList = new LinkedList<AnnotatedRegularExpressionBinding>();

        Iterator iter = this.bindingInfo.iterator();
        while (iter.hasNext()) {
            BindingInfo bi = (BindingInfo) iter.next();
            BindingCallback cbk = (BindingCallback) instantiateCallback(
                    bi.cbkClass, bi.cbkArgs);

            AnnotatedRegularExpressionBinding regExBinding = new AnnotatedRegularExpressionBinding(
                    bi.bindingName, bi.bindingValue, cbk, log);
            bindingList.add(regExBinding);
        }

        ArrayAnnotatedMatcher aam = new ArrayAnnotatedMatcher(this.annRegExs,
                bindingList);
        aam.setContext(instantiateContext(this.ctxClass, this.ctxArgs));

        return new ParserMatcher(aam);
    }

    public Context instantiateContext(String contextClass, Map contextArgs) throws Exception {
        Context context = null;
        /* instantiate the context if it has been specified */
        if (contextClass != null && !contextClass.trim().equals("")) {
            Object[] args = new Object[1];
            args[0] = contextArgs;
            Class[] signature = {Map.class};
            context = (Context) instantiateClass(contextClass, args, signature, null, null);
        }
        return context;
    }

    public static Object instantiateClass(String className, Object[] args, Class[] signature,
                                          Class extendClass, Class[] implementedClasses) throws Exception {
        if (className == null || className.trim().equals(""))
            return null;

        Object newObject = null;
        try {
            newObject = Class.forName(className).getConstructor(
                    signature).newInstance(args);

            if (extendClass != null && !extendClass.isInstance(newObject))
                throw new Exception("class " + className
                        + "does not (in)directly extend"
                        + extendClass.getName());
            else if (implementedClasses != null) {
                for (int i = 0; i < implementedClasses.length; i++) {
                    if (!implementedClasses[i].isInstance(newObject))
                        throw new Exception("class " + className
                                + "does not (in)directly implement "
                                + implementedClasses[i].getName());
                }
            }

        } catch (Exception e) {
            System.out.println("error while loading class " + className);
            e.printStackTrace(System.out);

            System.err.println("error while loading class " + className);
            e.printStackTrace(System.err);

            throw e;
        }

		/* all checks passed, return the object */
        return newObject;
    }

    /**
     * direct the date parsing library to the location of your json
     * configuration files
     */
    public static void setConfigPath(String path) {
        RegexExtractor.CONFIG_PATH = path;
        DataFileReader.setConfigPath(CONFIG_PATH);
    }

    /**
     * set logger
     *
     * @param logger
     */
    public static void setLogger(Logger logger) {
        log = logger;
    }

    /**
     * get parser by data file code
     *
     * @param dataFileCode
     * @return
     * @throws Exception
     */
    public synchronized static RegexExtractor getParser(String dataFileCode)
            throws Exception {
        RegexExtractor dp = instances.get(dataFileCode);

        if (log != null) {
            log.info("cached date parser for file code '" + dataFileCode
                    + "': " + dp);
        }

        if (dp == null) {
            dp = new RegexExtractor(dataFileCode);
            instances.put(dataFileCode, dp);
        }

        return dp;
    }

}
