package javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.Assert;
import org.junit.Test;

/**
 * test the java parser func.
 *
 * LogNotice.info(firstArg, secondArg, .....)
 */
public class TestJavaParser {


    /**
     * first arg is not string, second is string.
     */
    @Test
    public void test1() {

        String method = "LogNotice.warn(\"Duplicated port config in item \" + Configuration.PORTS + \" and \" + Configuration.SFLOW_PORTS" +
                " + \" of agent.conf\", \"Ignore the sflow port\", \"port=\" + port)\n" +
                "                            ";

        MethodCallExpr callExpr = JavaParser.parseExpression(method);
        String re = RemoveLogNotice.reComposeMethod(callExpr);
        String shouldbe = "LogMsg.warn(\"Duplicated port config in item \" + Configuration.PORTS + \" and \" + Configuration.SFLOW_PORTS " +
                "+ \" of agent.conf\" + \"Ignore the sflow port\", \"port=\" + port);\n";
        Assert.assertEquals(shouldbe, re);

    }

    /**
     * first is not string, second is not string
     */
    @Test
    public void test3() {
        String method = "LogNotice.info(LogWords.EXCEPTION, String.format(\"Context=%s\", 1), \"Yes it's it\")\n";

        MethodCallExpr callExpr = JavaParser.parseExpression(method);
        String re = RemoveLogNotice.reComposeMethod(callExpr);
        String shouldbe =
                "LogMsg.info(LogWords.EXCEPTION + String.format(\"Context=%s\", 1), \"Yes it's it\")";
        Assert.assertEquals(shouldbe, re);
    }


    /**
     * first is string, second is string
     */
    @Test
    public void test2() {
        String method = "LogNotice.info(\"Error happened\", \"Quit it\", String.format(\"Context=%s\", 1))";

        MethodCallExpr callExpr = JavaParser.parseExpression(method);
        String re = RemoveLogNotice.reComposeMethod(callExpr);
        String shouldbe = "LogMsg.info(\"Error happened, Quit it\", String.format(\"Context=%s\", 1));";
        Assert.assertEquals(shouldbe, re);
    }


    /**
     * first is string, second is not string
     */
    @Test
    public void test4() {
        String method = "LogNotice.info(\"Found exception happened\", String.format(\"Ignore, Context=%s\", 1), \"Yes it's it\")\n";
        MethodCallExpr callExpr = JavaParser.parseExpression(method);
        String re = RemoveLogNotice.reComposeMethod(callExpr);
        String shouldbe = "LogMsg.info(\"Found exception happened\" + String.format(\"Ignore, Context=%s\", 1), \"Yes it's it\");";
        Assert.assertEquals(shouldbe, re);
    }
}
