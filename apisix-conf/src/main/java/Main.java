import cn.hutool.http.*;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.*;
import org.apache.commons.cli.*;

import java.util.*;

public class Main {

    private static final String ADMIN_UPSTREAMS_URL = "/apisix/admin/upstreams";
    private static final String ADMIN_ROUTES_URL = "/apisix/admin/routes";

    static String url;
    static String apiKey;
    static Map<String, String> authHeaders = new HashMap<>();

    /**
     * apisix cli:support routes、upstreams
     * example:
     * routes|upstreams update '{"timeout":{"connect": 10,"send": 10,"read": 30}}' -u http://192.168.102.221:9427 -k edd1c9f034335f136f87ad84b625c8f1 -all
     * routes|upstreams list
     *
     * @param inputArgs
     * @throws ParseException
     */
    public static void main(String[] inputArgs) throws ParseException {

        Option all = new Option("all", "apply to all objects");
        Option id = new Option("id", "the id of the object");
        Options options = new Options();
        options.addOption(all);
        options.addOption(id);
        options.addRequiredOption("u", "url", true, "the url to connect to. eg: http://127.0.0.1:9180");
        options.addRequiredOption("k", "key", true, "the api-key to use");

        CommandLineParser p = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = p.parse(options, inputArgs);
        } catch (Exception e) {
            System.out.println(e);
            printHelp(options);
            return;
        }
        String[] args = commandLine.getArgs();
        if (args.length == 0) {
            printHelp(options);
            return;
        }
        url = commandLine.getOptionValue("u");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        apiKey = commandLine.getOptionValue("k");
        authHeaders.put("X-API-KEY", apiKey);

        switch (args[0]) {
            case "routes":
                processRoutes(commandLine, args[1]);
                break;
            case "upstreams":
                processUpstreams(commandLine, args[1]);
                break;
            default:
                System.out.println("not support object type : " + args[0]);
                break;
        }
    }

    private static void processUpstreams(CommandLine commandLine, String operation) {
        switch (operation) {
            case "list":
                JSONObject resp = httpGetJson(ADMIN_UPSTREAMS_URL);
                System.out.println(resp.toString(SerializerFeature.PrettyFormat));
                break;
            case "update":
                JSONArray allstreams = httpGetJson(ADMIN_UPSTREAMS_URL).getJSONArray("list");
                String updateArg = commandLine.getArgs()[2];
                JSONObject updateArgJson = JSONObject.parseObject(updateArg);
                for (int i = 0; i < allstreams.size(); i++) {
                    JSONObject currentStream = allstreams.getJSONObject(i).getJSONObject("value");
                    String routeId = currentStream.getString("id");
                    if (commandLine.hasOption("all") || commandLine.getOptionValue("id", "").equals(routeId)) {
                        String uri = ADMIN_UPSTREAMS_URL + "/" + routeId;
                        System.out.println("Will update upstream " + routeId + " with " + updateArg);
                        currentStream.putAll(updateArgJson);
                        JSONObject patchResp = httpPutJson(uri, currentStream);
                        System.out.println("\t\t" + patchResp);
                    } else {
                        System.out.println("SKIPed upstream " + routeId);
                    }
                }
                break;
            default:
                System.out.println("unknown operation for upstreams " + operation);
        }
    }

    private static void processRoutes(CommandLine commandLine, String operation) {
        switch (operation) {
            case "list":
                JSONObject resp = httpGetJson("");
                System.out.println(resp.toString(SerializerFeature.PrettyFormat));
                break;
            case "update":
                JSONArray allroutes = httpGetJson(ADMIN_ROUTES_URL).getJSONArray("list");
                String updateArg = commandLine.getArgs()[2];
                JSONObject updateArgJson = JSONObject.parseObject(updateArg);
                for (int i = 0; i < allroutes.size(); i++) {
                    JSONObject currentRoute = allroutes.getJSONObject(i).getJSONObject("value");
                    String routeId = currentRoute.getString("id");
                    if (commandLine.hasOption("all") || commandLine.getOptionValue("id", "").equals(routeId)) {
                        String uri = ADMIN_ROUTES_URL + "/" + routeId;
                        System.out.println("Will update route " + routeId + " with " + updateArg);
                        JSONObject upstream = currentRoute.getJSONObject("upstream");
                        if (upstream != null) {
                            upstream.putAll(updateArgJson);
                            currentRoute.put("upstream", upstream);
                            JSONObject patchResp = httpPutJson(uri, currentRoute);
                            System.out.println("\t\t" + patchResp);
                        } else {
                            System.out.println("\t\t Stream not set. maybe it has a stream id? (if so, use `upstreams update`) : " + currentRoute.getString("upstream_id"));
                        }
                    } else {
                        System.out.println("SKIPed route " + routeId);
                    }
                }
                break;
            default:
                System.out.println("unknown operation for routes " + operation);
        }
    }

    private static void updateStream(String streamId, JSONObject stream) {

    }

    private static void printHelp(Options options) {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("[update routes [updateArg]] | list", options);
    }

    private static JSONObject httpPutJson(String uri, JSONObject putJson) {
        HttpRequest r = HttpUtil.createRequest(Method.PATCH, url + uri);
        r.addHeaders(new HashMap<>(authHeaders));
        r.body(putJson.toString());
        String resp = r.execute().body();
        return JSONObject.parseObject(resp);
    }

    private static JSONObject httpPatchJson(String uri, JSONObject patchedJson) {
        HttpRequest r = HttpUtil.createRequest(Method.PATCH, url + uri);
        r.addHeaders(new HashMap<>(authHeaders));
        r.body(patchedJson.toString());
        String resp = r.execute().body();
        return JSONObject.parseObject(resp);
    }

    private static JSONArray httpGetJsonArray(String uri) {
        return JSONArray.parseArray(httpGet(uri));
    }

    private static String httpGet(String uri) {
        HttpRequest r = HttpUtil.createGet(url + uri);
        r.addHeaders(new HashMap<>(authHeaders));
        String resp = r.execute().body();
        return resp;
    }

    private static JSONObject httpGetJson(String uri) {
        return JSONObject.parseObject(httpGet(uri));
    }
}
