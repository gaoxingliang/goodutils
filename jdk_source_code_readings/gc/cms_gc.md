# CMS gc 代码阅读
基于[jdk11](https://github.com/gaoxingliang/openjdk-jdk11u/tree/master/src/hotspot/share/gc/cms)的源码

# 测试代码
```
import java.util.ArrayList;
public class TExample {
    public static void main(String[] args) {
        ArrayList l = new ArrayList<>();
        ArrayList old = new ArrayList();
        for (int i = 0; i < 1000000; i++) {
            if (i % 20000 == 0) {
                l.clear();
            }
            if (i %2 == 0) {
                old.add("WQWQWQWQ" + i);
            }
            if (old.size() > 100000) {
                old.remove(0);
            }
            
            l.add("xsaxsasaxsasaxsasaxsasaxsasaxsasasa" + i);
        }
    }
}

```
在JDK11 上运行:
```
-XX:+UseConcMarkSweepGC -Xlog:gc* -Xmx60m -XX:+UseConcMarkSweepGC
```

## 示例GC输出
```
[4.958s][info][gc,start     ] GC(7) Pause Initial Mark
[4.958s][info][gc           ] GC(7) Pause Initial Mark 25M->25M(58M) 0.346ms
[4.958s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[4.958s][info][gc           ] GC(7) Concurrent Mark
[4.958s][info][gc,task      ] GC(7) Using 2 workers of 3 for marking
[4.963s][info][gc           ] GC(7) Concurrent Mark 4.980ms
[4.963s][info][gc,cpu       ] GC(7) User=0.01s Sys=0.00s Real=0.01s
[4.963s][info][gc           ] GC(7) Concurrent Preclean
[4.964s][info][gc           ] GC(7) Concurrent Preclean 0.537ms
[4.964s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[4.964s][info][gc,start     ] GC(7) Pause Remark
[4.965s][info][gc           ] GC(7) Pause Remark 25M->25M(58M) 1.272ms
[4.965s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[4.965s][info][gc           ] GC(7) Concurrent Sweep
[4.983s][info][gc           ] GC(7) Concurrent Sweep 17.231ms
[4.983s][info][gc,cpu       ] GC(7) User=0.03s Sys=0.00s Real=0.02s
[4.983s][info][gc           ] GC(7) Concurrent Reset
[4.983s][info][gc           ] GC(7) Concurrent Reset 0.158ms
[4.983s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[4.983s][info][gc,heap      ] GC(7) Old: 23943K->7095K(40960K)
```
# CMS 收集的阶段

![img](img/7f5a9b64.png)
入口地方[CMSCollector::collect_in_background](https://github.com/gaoxingliang/openjdk-jdk11u/blob/master/src/hotspot/share/gc/cms/concurrentMarkSweepGeneration.cpp#L1709)
```
CMSCollector::collect_in_background
```
基础结构:
```
  while (_collectorState != Idling) {
      switch (state) {
         case inital marking
         case final marking
         case ....
      }
  
  }
```
## Initial Marking
[STW 阶段(业务线程停止)](https://github.com/gaoxingliang/openjdk-jdk11u/blob/master/src/hotspot/share/gc/cms/concurrentMarkSweepGeneration.cpp#L2823), [checkpointRootsInitial](https://github.com/gaoxingliang/openjdk-jdk11u/blob/master/src/hotspot/share/gc/cms/concurrentMarkSweepGeneration.cpp#L5527)
可能是多线程操作(默认多线程, 有配置)[CMSParallelInitialMarkEnabled](https://github.com/gaoxingliang/openjdk-jdk11u/blob/master/src/hotspot/share/gc/cms/concurrentMarkSweepGeneration.cpp#L2871)
扫描GC roots.
将年轻代引用了老年代的对象都作为GCRoots.
![](img/389289ef.png)

## Marking
非STW, [并发标记(多个线程同时标记)]()


## TODOS 
1. 在初始化标记阶段 需要检查是否在safepoint. 这个在哪里设置的!?
2. 