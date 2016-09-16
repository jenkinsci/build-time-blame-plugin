//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse

class StaplerUtils {
    public static void redirectToParentURI(StaplerRequest request, StaplerResponse response) {
        response.sendRedirect(getParentURI(request))
    }

    private static String getParentURI(StaplerRequest request) {
        def originalRequestURI = request.getOriginalRequestURI()
        if (originalRequestURI.endsWith('/')) {
            originalRequestURI = originalRequestURI.substring(0, originalRequestURI.size() - 1)
        }

        return originalRequestURI.substring(0, originalRequestURI.lastIndexOf('/'))
    }

    static List<JSONObject> getAsList(JSONObject jsonObject, String key) {
        Object value = jsonObject.get(key)
        if (value instanceof JSONArray) {
            return value.collect() as List<JSONObject>
        }

        return [value] as List<JSONObject>
    }
}
