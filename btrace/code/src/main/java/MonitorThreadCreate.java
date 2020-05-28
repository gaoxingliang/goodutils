import org.openjdk.btrace.core.BTraceUtils;
import org.openjdk.btrace.core.annotations.BTrace;
import org.openjdk.btrace.core.annotations.Kind;
import org.openjdk.btrace.core.annotations.Location;
import org.openjdk.btrace.core.annotations.OnMethod;
import org.openjdk.btrace.core.annotations.Self;

@BTrace
public class MonitorThreadCreate {


    @OnMethod(
            clazz = "java.lang.Thread",
            method = "<init>",
            location = @Location(Kind.RETURN)
    )
    public static void onThreadCreate(@Self Thread th) {
        BTraceUtils.println("==========================");
        BTraceUtils.println("Thread create below");
        BTraceUtils.println(BTraceUtils.Threads.name(th));
        BTraceUtils.Threads.jstack();
        BTraceUtils.println("==========================");
    }
}
