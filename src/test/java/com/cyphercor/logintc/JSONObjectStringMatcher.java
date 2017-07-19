
package com.cyphercor.logintc;

import static org.mockito.Matchers.argThat;

import org.hamcrest.Description;
import org.json.JSONException;
import org.mockito.ArgumentMatcher;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Match String representation of JSON data
 */
public class JSONObjectStringMatcher extends ArgumentMatcher<String> {
    private String expected;

    /**
     * @param expected
     */
    public JSONObjectStringMatcher(String expected) {
        this.expected = expected;
    }

    /**
     * @param expected The expected value to match.
     * @return The matcher.
     */
    public static String eq(String expected) {
        return argThat(new JSONObjectStringMatcher(expected));
    }

    @Override
    public boolean matches(Object argument) {
        try {
            if (expected == null)
                return argument == null;
            if (!(argument instanceof String))
                return false;

            try {
                JSONAssert.assertEquals(expected, (String) argument, JSONCompareMode.NON_EXTENSIBLE);
            } catch (AssertionError e) {
                return false;
            }

            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expected.toString());
    }
}
