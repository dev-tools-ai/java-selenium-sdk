package ai.devtools.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The {@code Utils} class provide a function for turning strings into Booleans.
 */
public class Utils {
    public static Boolean StrToBool(String value) {
        ArrayList<String> truthValues = new ArrayList<String>(Arrays.asList("1", "true", "yes"));
        if (truthValues.contains(value != null ? value.toLowerCase() : "false")) {
            return true;
        } else {
            return false;
        }
    }

    public static JsonObject collectStackTrace() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        JsonArray jsStFilenames = new JsonArray();
        JsonArray jsStTraces = new JsonArray();

        for(int i = 0; i < st.length; i++) {
            StackTraceElement ste = st[i];
            String filename = ste.getFileName();
            if (filename != null) {
                JsonArray filenameLinenumber = new JsonArray();
                filenameLinenumber.add(filename);
                filenameLinenumber.add(ste.getLineNumber());
                jsStFilenames.add(filenameLinenumber);
            }
            jsStTraces.add(ste.toString());
        }

        JsonObject stackTrace = new JsonObject();
        stackTrace.add("filenames", jsStFilenames);
        stackTrace.add("traces", jsStTraces);

        return stackTrace;
    }

}
