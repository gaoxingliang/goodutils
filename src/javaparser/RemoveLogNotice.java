package javaparser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * References:
 * http://javaparser.org/
 * https://www.javaadvent.com/2017/12/javaparser-generate-analyze-modify-java-code.html
 *
 * This class implemented a method to replace some method like:
 *  LogNotice.info("aaa", "bb", "cc", [e])
 *      -> to
 *      LogMsg.info("aa, bb", "cc", [e])
 *
 *
 * Note: this will overwrite the files.
 * Created by edward.gao on 10/26/16.
 */
public class RemoveLogNotice {
    private static Stack<CompilationUnit> allCus = new Stack<>();

    private static String STARTPACKAGE = ""; //  the package name like "aaa.bbc.ccc"
    private static String SOURCE_FOLDER = ""; // the base source folder like "/code/java/src"
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Need at least two args:  codeSourceFolder startPackageName");
            return;
        }
        SOURCE_FOLDER = args[0];
        STARTPACKAGE = args[1];

        reInit(SOURCE_FOLDER, STARTPACKAGE);
        while (!allCus.isEmpty()) {
            CompilationUnit u = allCus.pop();
            if (processOneFile(u)) {
                reInit(SOURCE_FOLDER, STARTPACKAGE);
            }
        }
    }

    /**
     *
     * Process a single compilation unit, that's a file too
     * @param u
     * @return return whether we need to re-run for all files. because when a file changed, the lines and related information will be changed.
     *          return true when we want to re-run for all files.
     * @throws IOException
     */
    public static boolean processOneFile(CompilationUnit u) throws IOException {
        System.out.println("File " + u.getStorage().get().getFileName());
        List<MethodDeclaration> methods = u.findAll(MethodDeclaration.class);
        for (MethodDeclaration m : methods) {
            if (!m.getBody().isPresent()) {
                continue;
            }
            BlockStmt block = m.getBody().get();

            Stack<Node> processStack = new Stack<>();
            processStack.push(block);
            while (!processStack.isEmpty()) {
                Node curSt = processStack.pop();
                if (curSt instanceof ExpressionStmt) {
                    Expression expr = ((ExpressionStmt) curSt).getExpression();
                    if (expr instanceof MethodCallExpr) {
                        MethodCallExpr expression = (MethodCallExpr) expr;
                        if (expression.getScope().isPresent() && expression.getScope().get().toString().equals("LogNotice")) {

                            /**
                             * here we start to replace the methods....
                             * it's may be complex, but the main logic is:
                             * if some arg is a StringLiteralExpr we can
                             */
                            NodeList<Expression> arguments = expression.getArguments();
                            if (arguments.size() >= 3 && arguments.size() <= 4) {
                                Expression first = arguments.get(0);
                                Expression second = arguments.get(1);
                                if (isValidExpr(first) && isValidExpr(second)) {
                                    String reComposedMethod = reComposeMethod(expression);

                                    Range lineRange = expression.getRange().get();
                                    // expression = JavaParser.parseExpression(newString.toString());
                                    reWriteCode(u.getStorage().get().getPath(), expression.toString(), reComposedMethod, lineRange);
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
                                throw new IllegalArgumentException("The arguments is not 3 or 4 args - " + expression);
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
        }
        return false;
    }


    /**
     * re-compose of the LogMsg from LogNotice method.
     * @param methodCallExpr
     * @return the re-write method string
     */
    static String reComposeMethod(MethodCallExpr methodCallExpr) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();
        String methodName = methodCallExpr.getNameAsString(); // info

        StringBuilder newString = new StringBuilder("LogMsg." + methodName + "(");

        Expression first = arguments.get(0);
        Expression second = arguments.get(1);

        if (first instanceof StringLiteralExpr) {
            if (second instanceof StringLiteralExpr) {
                //
                newString.append("\"").append(first.asStringLiteralExpr().asString() + ", " +
                        second.asStringLiteralExpr().asString()).append("\"");
            }
            else {
                //
                newString.append("\"").append(first.asStringLiteralExpr().asString()).append("\" " +
                        "+ ").append(second.toString());
            }
        }
        else {
            if (second instanceof StringLiteralExpr) {
                //
                newString.append(first.toString()).append(" + ").append(second
                        .asStringLiteralExpr().toString());
            }
            else {
                //
                newString.append(first.toString()).append(" + ").append(second.toString());
            }
        }
        newString.append(", ");
        newString.append(arguments.get(2).toString());
        if (arguments.size() == 4) {
            newString.append(", ").append(arguments.get(3).toString());
        }
        newString.append(");");

        return newString.toString();


    }


    /**
     * whether the cu contains the LogNotice class import
     * @param u
     * @return
     */
    private static boolean containsOurMethod(CompilationUnit u) {
        for (ImportDeclaration id : u.getImports()) {
            if (id.getName().asString().equals("com.santaba.common.logger.LogNotice")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidExpr(Expression e) {
        return true;
    }


    /**
     * re init the cu sets.
     * @param srcFolder
     * @param startPackage
     * @throws IOException
     */
    private static void reInit(String srcFolder, String startPackage) throws IOException {
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

    /**
     * Re-write the code by
     *  (1) if the original line found in file, just replace it.
     *  (2) if not found in file, the file will be rewrite according to the line range infor
     * @param file  the file which will be replaced or changed
     * @param originalLine the original line (may be multiple lines) in file
     * @param replaceWith replace the method to this...
     * @param lineRange the original line range position
     * @throws IOException
     */
    private static void reWriteCode(Path file, String originalLine, String replaceWith, Range lineRange) throws IOException {
        String rawString = new String(Files.readAllBytes(file));
        if (rawString.indexOf(originalLine) < 0) {

            // if we didn't found that string...
            // this happened, when the method is more than 1 line....
            // and the \n is auto ignored in the Node.toString()
            // we have to replace .....
            List<String> lines = Files.readAllLines(file);
            int startLine = lineRange.begin.line - 1;// the range start with 1
            int endLine = lineRange.end.line - 1;
            StringBuilder toFileLine = new StringBuilder();
            for (int i = 0; i < lineRange.begin.column; i++) {
                toFileLine.append(" "); // make sure it's align right
            }
            toFileLine.append(replaceWith);
            List<String> toFileLines = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (i == startLine) {
                    toFileLines.add(toFileLine.toString());
                }
                else if (i > startLine && i <= endLine) {
                    continue;
                }
                else {
                    toFileLines.add(lines.get(i));
                }
            }
            Files.write(file, toFileLines, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }

        String replaced = rawString.replace(originalLine, replaceWith);
        Files.write(file, replaced.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }
}
