package javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * References:
 * http://javaparser.org/
 * https://www.javaadvent.com/2017/12/javaparser-generate-analyze-modify-java-code.html
 * <p>
 * This class implemented a method to replace some method like:
 * Logger.infor2("aaa", "xx") -> LogMsg
 * <p>
 * Note: this will overwrite the files.
 * Created by edward.gao on 10/26/16.
 */
public class RemoveLogger {
    private static Stack<CompilationUnit> allCus = new Stack<>();

    private static String STARTPACKAGE = ""; //  the package name like "aaa.bbc.ccc"
    private static String SOURCE_FOLDER = ""; // the base source folder like "/code/java/src"

    static final Map<String /*method in Logger*/, String /*method in LogMsg*/> methodMap = new HashMap<>();

    // ignore methods for Logger
    static final Set<String> ignoreMethods = new HashSet<>();

    static {

        ignoreMethods.add("setComponent");
        ignoreMethods.add("setIdStr");
        ignoreMethods.add("removeListener");
        ignoreMethods.add("addListener");
        ignoreMethods.add("setLogLevel");

        methodMap.put("trace2", "debug");
        methodMap.put("trace", "debug");
        methodMap.put("debug2", "debug");
        methodMap.put("debug", "debug");
        methodMap.put("info", "info");
        methodMap.put("info2", "info");
        methodMap.put("warn", "warn");
        methodMap.put("warn2", "warn");
        methodMap.put("error", "error");
        methodMap.put("error2", "error");
        methodMap.put("exception", "error");
        methodMap.put("exception2", "error");
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Need at least two args:  codeSourceFolder startPackageName");
            return;
        }
        SOURCE_FOLDER = args[0];
        STARTPACKAGE = args[1];
        init(SOURCE_FOLDER, STARTPACKAGE);
        while (!allCus.isEmpty()) {
            CompilationUnit u = allCus.pop();
            if (processOneFile(u)) {
                allCus.push(reCompile(u.getStorage().get().getPath()));
            }
        }


        System.out.println("Round two.... add needed imports.....");

        init(SOURCE_FOLDER, STARTPACKAGE);
        while(!allCus.isEmpty()) {
            CompilationUnit u = allCus.pop();
            if (needAddLogMsgPackageImport(u)) {
                List<String> lines = Files.readAllLines(u.getStorage().get().getPath());
                int i = 0;
                for (; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("import ")) {
                        break;
                    }
                }
                lines.add(i+1, "import com.santaba.common.logger.LogMsg;");
                Files.write(u.getStorage().get().getPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Add import for " + u.getStorage().get().getFileName());
            }
        }

    }

    private static boolean needAddLogMsgPackageImport(CompilationUnit u) {
        for (ImportDeclaration id : u.getImports()) {
            String pack = id.getName().asString();
            if (pack.equals("com.santaba.common.logger.*")
                    ) {
                return false;
            } else if (pack.equals("com.santaba.common.logger.LogMsg")) {
                return false;
            }
        }
        if (u.toString().contains("LogMsg")) {
            return true;
        }
        System.out.println("No need add import for " + u.getStorage().get().getFileName());
        return false;
    }


    /**
     * Process a single compilation unit, that's a file too
     *
     * @param u
     * @return return whether we need to re-run for all files. because when a file changed, the lines and related information will be
     * changed.
     * return true when we want to re-run for all files.
     * @throws IOException
     */
    public static boolean processOneFile(CompilationUnit u) throws IOException {
        System.out.println("File " + u.getStorage().get().getFileName());
        Stack<Node> processStack = new Stack<>();

        // addd all constructor methods and normal methods to the process stack, we will process it later
        u.findAll(ConstructorDeclaration.class)
                .stream().map(m -> m.getBody())
                .forEach(m -> processStack.push(m));
        u.findAll(MethodDeclaration.class)
                .stream().filter(m -> m.getBody().isPresent()).map(m -> m.getBody().get())
                .forEach(m -> processStack.push(m));


        while (!processStack.isEmpty()) {

            Node curSt = processStack.pop();
            if (curSt instanceof ExpressionStmt) {
                Expression expr = ((ExpressionStmt) curSt).getExpression();
                if (expr instanceof MethodCallExpr) {
                    MethodCallExpr expression = (MethodCallExpr) expr;
                    if (expression.getScope().isPresent() && expression.getScope().get().toString().equals("Logger")
                            ) {
                        if (ignoreMethods.contains(expression.getNameAsString())) {
                            continue;
                        }

                        /**
                         * here we start to replace the methods....
                         * it's may be complex, but the main logic is:
                         * if some arg is a StringLiteralExpr we can
                         */
                        NodeList<Expression> arguments = expression.getArguments();
                        if (arguments.size() >=1 && arguments.size()<=3) {
                            Expression first = arguments.get(0);
                            if (isValidExpr(first)) {
                                String reComposedMethod = reComposeMethod(expression);

                                Range lineRange = expression.getRange().get();
                                if (!expression.toString().endsWith(";")) {
                                    reComposedMethod = reComposedMethod.substring(0, reComposedMethod.length() - 1);
                                }

                                Helper.reWriteCode(u.getStorage().get().getPath(), expression.toString(), reComposedMethod, lineRange);
                                System.out.println("The reformat string is " + reComposedMethod);

                                return true;
                            }
                            else {
                                if (first instanceof ObjectCreationExpr) {
                                    NodeList<BodyDeclaration<?>> nodeList = first.asObjectCreationExpr()
                                            .getAnonymousClassBody().get();
                                    nodeList.stream().forEach(n -> processStack.push(n));
                                }
                                else {
                                    throw new IllegalArgumentException("Not support this yet " + expression);
                                }
                            }

                        }
                        else {
                            throw new IllegalArgumentException("Not support expression with args numbers..." + expression);
                        }

                    }
                }
                else {
                    if (!curSt.getChildNodes().isEmpty()) {
                        for (Node n : curSt.getChildNodes()) {
                            if (n instanceof Node) {
                                processStack.push(n);
                            }
                        }
                    }
                }
            }
            else {
                curSt.getChildNodes().stream().forEach(n -> processStack.push(n));
            }
        }
        return false;
    }


    /**
     * re-compose of the LogMsg from LogNotice method.
     *
     * @param methodCallExpr
     * @return the re-write method string
     */
    static String reComposeMethod(MethodCallExpr methodCallExpr) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();
        String methodName = methodCallExpr.getNameAsString(); // info
        String methodNameForLogMsg = methodMap.get(methodName);
        if (methodNameForLogMsg == null) {
            throw new IllegalStateException(String.format("Not find matched method for method %s in LogMsg, expression=%s", methodName, methodCallExpr));
        }

        StringBuilder newString = new StringBuilder("LogMsg." + methodNameForLogMsg + "(");

        Expression first = arguments.get(0);
        if (arguments.size() == 1) {
            if (Helper.isException(first)) {
                // some expression like Logger.exception2(e)
                newString.append("\"\", \"\", ").append(first.toString());
            }
            else {
                newString.append(Helper.simpleCompose(first));
                newString.append(", \"\"");
            }
        }
        else if (arguments.size() == 2) {
            newString.append(Helper.simpleCompose(first));
            if (Helper.isException(arguments.get(1))) {
                // exception...
                // format is Logger.xxx(aaa, e)
                newString.append(", \"\", ").append(arguments.get(1).toString());
            }
            else {
                newString.append(", ").append(Helper.simpleCompose(arguments.get(1)));
            }
        }
        else if (arguments.size() == 3) {
            newString.append(Helper.simpleCompose(arguments.get(0))).append(", ")
                    .append(Helper.simpleCompose(arguments.get(1))).append(", ")
                    .append(Helper.simpleCompose(arguments.get(2)));
        }
        else {
            throw new IllegalStateException("Not implement args:" + methodCallExpr);
        }

        newString.append(");");

        return newString.toString();


    }


    /**
     * whether the cu contains the LogNotice class import
     *
     * @param u
     * @return
     */
    private static boolean containsOurMethod(CompilationUnit u) {
        for (ImportDeclaration id : u.getImports()) {
            if (id.getName().asString().equals("com.santaba.common.logger.Logger")
                    ) {
                return true;
            }
        }
        if (u.toString().contains("Logger")) {
            return true;
        }
        System.out.println("Skipped " + u.getStorage().get().getFileName());
        return false;
    }

    private static boolean isValidExpr(Expression e) {
        return true;
    }


    private static CompilationUnit reCompile(Path file) throws IOException {
        try {
            return JavaParser.parse(file);
        } catch (Exception e) {
            System.err.println("Fail to parse file " + file);
            throw e;
        }
    }

    /**
     * init the cu sets.
     *
     * @param srcFolder
     * @param startPackage
     * @throws IOException
     */
    private static void init(String srcFolder, String startPackage) throws IOException {
        allCus.clear();
        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);
        // Parse all source files
        SourceRoot sourceRoot = new SourceRoot(new File(srcFolder).toPath());
        sourceRoot.setParserConfiguration(conf);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse(startPackage);

        allCus.addAll(parseResults.stream()
                .filter(ParseResult::isSuccessful)
                .filter(pr -> containsOurMethod(pr.getResult().get()))
                .map(r -> r.getResult().get())
                .collect(Collectors.toList())
        );
    }

}
