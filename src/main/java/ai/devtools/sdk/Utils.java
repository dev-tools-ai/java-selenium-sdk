package ai.devtools.sdk;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The {@code Utils} class provide a function for turning strings into Booleans.
 */
class  Utils {
    static Boolean StrToBool(String value) {
        ArrayList<String> truthValues = new ArrayList<String>(Arrays.asList("1", "true", "yes"));
        if (truthValues.contains(value != null ? value.toLowerCase() : "false")) {
            return true;
        } else {
            return false;
        }
    }
}
