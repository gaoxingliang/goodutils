package javaparser;

import com.github.javaparser.Range;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Helper {
    /**
     * Re-write the code by
     * (1) if the original line found in file, just replace it.
     * (2) if not found in file, the file will be rewrite according to the line range infor
     *
     * @param file         the file which will be replaced or changed
     * @param originalLine the original line (may be multiple lines) in file
     * @param replaceWith  replace the method to this...
     * @param lineRange    the original line range position
     * @throws IOException
     */
    public static void reWriteCode(Path file, String originalLine, String replaceWith, Range lineRange) throws IOException {
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
            for (int i = 0; i < lineRange.begin.column - 1; i++) {
                toFileLine.append(" "); // make sure it's align right
            }

            // if replace with don't contains ; let's add it here
            if (!replaceWith.endsWith(";")) {
                replaceWith+= ";";
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

    public static String simpleCompose(Expression expression) {
        if (expression instanceof StringLiteralExpr) {
            return String.format("\"%s\"", expression.asStringLiteralExpr().asString());
        }
        else {
            return expression.toString();
        }
    }

    public static boolean isException(Expression e) {
        return e instanceof NameExpr && ( e.toString().equals("e") ||e.toString().equals("t"));
    }

}
