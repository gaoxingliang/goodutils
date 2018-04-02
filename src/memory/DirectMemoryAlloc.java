package memory;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.nio.ByteBuffer;

/**
 * a class to allocate direct memory
 */
public class DirectMemoryAlloc {

    public static void main(String[] args) {
        try {
            ByteBuffer.allocateDirect(Integer.valueOf(args[0]) * 1024 * 1024);
        } catch (Throwable e) {
            System.out.println(String.format("Error when alloc %d MB native memory", Integer.valueOf(args[0])));
            System.out.println(ExceptionUtils.getFullStackTrace(e));
        }
    }
}
