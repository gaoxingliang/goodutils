Plumber copyright 中文版本
![logo](res/gcbook/logo_plumber_handbook.png)
*该文章被翻译仅做学习之用*

# 目录
### 什么是垃圾回收
### Java中的垃圾回收
### GC 算法: 基础
### GC 算法: 实现
### GC 优化: 基础
### GC 优化: 工具
### GC 优化: 实践

# 什么是垃圾回收(Garbage Collection, GC)
初看之下, 垃圾回收Garbage Collection, GC)应该是用来-找到和清理掉垃圾的. 但是现实中,它却是做的完全相反的工作. GC是用来追踪那些正在被使用的对象, 然后标记其他的对象为垃圾对象. 将这个牢牢记住, 我们马上就仔细看看JVM中被称为自动垃圾回收再利用的过程是如何实现的.

在开始直接讨论细节之前, 我们来看看最最开始的时候, 垃圾回收的本质, 核心概念和途径.

*声明:这本手册主要关注于Oracle Hotspot和OpenJDK的行为,在其他运行时或者JVM中,比如JROCKIT,IBM J9部分行为可能与本手册所说不太一样*

## **手动内存管理**
在我们开始讨论GC的现代化工作方式之前, 让我们来快速看下以前我们是如何手动和精确的控制内存的申请和释放的. 如果你忘记释放内存,你就没法重用该内存. 这片内存也没法被声明为未使用. 这就是**内存泄漏**

下面是一个使用C写的演示手动内存管理的代码:
```c
int send_request() {
size_t n = read_size();
int *elements = malloc(n * sizeof(int));
    if(read_elements(n, elements) < n) {
        // elements not freed!
        return -1;
    }
    // ...
    free(elements)
    return 0;
}
```
正如我们看到的, 很容易就会忘记释放内存. 内存泄漏也变成了一个非常常见的问题. 你只能通过修改代码来解决. 一个更好的方法就是能够自动回收掉不在使用的内存, 消除人为错误的可能性. 这样的自动化方式称为**垃圾回收(Garbage Collection, 简单来说就是GC)**

### **智能指针**
一个最初的方法来自动垃圾回收就是构建一个引用计数. 对于每个对象, 我们可以知道这个对象还被引用了多少次, 当他的引用计数次数变为0了, 这个对象就可以被回收了. 一个熟知的例子就是c++中的智能指针:
```c++
int send_request() {
    size_t n = read_size();
    shared_ptr<vector<int>> elements
              = make_shared<vector<int>>();
    if(read_elements(n, elements) < n) {
        return -1;
    }
    return 0;
}
```
这里的*shared_ptr*就是用来跟踪引用计数的. 这个计数随着你的分发而增加, 当它离开函数Scope时就减少. 当引用计数的值变为0时, *shared_ptr*就自动释放了底层的vector. 无可否认的, 这个例子在真实代码中并不多见, 但是用来演示是足够的.

## **自动内存管理**
在上面的c++代码中, 我们还是必须精确指定说, 我们需要内存管理来帮我们做这些事(*译注:shared_ptr*). 那么我们怎么才能让所有的对象都拥有类似的行为呢? 这么一来就很容易了, 开发者再也不需要关心如何清理他们了. 运行时会自动关注哪些内存不再被使用然后释放它们. 换句话说, 它自动**收集垃圾**. 第一个垃圾回收器是在1959年为Lisp设计的. 现在这门技术已经有了更近一步的发展.

### **引用计数**
在前面c++代码中的共享指针的方式能被用在所有对象上面. 许多语言(Perl, Python, PHP)都是这么做的. 下图更好的展示了这个过程:
![引用计数1](res/gcbook/reference-count-1.png)
绿色的云朵代表了那个仍被程序员使用的对象. 专业点说, 这可能是比如一个正在执行方法中的局部变量, 或者一个静态变量或者其他. 不同的语言可能不同, 我们也并不关注于此.
蓝色的圆圈代表内存中存活的对象, 里面的数字代表了它的应用计数. 灰色圆圈代表了不在被任何正在使用的对象(被绿色云朵引用的对象)引用的对象.

这看起来还不错? 是的. 但是这个方法有个很大的缺陷. 它很容易出现**分离圆圈(detached cycle)**
该范围中的所有对象都因为循环引用而导致引用计数不为0. 如下图:
![循环引用](res/gcbook/reference-cyclic.png)
看到了吗? 上图中,红色对象实际也是垃圾并且应用也不会使用它们. 但是因为引用计数,实际上还是有内存泄漏.

有一些办法可以克服这个问题. 比如使用特殊的'弱'引用或者使用单独的算法来处理循环. 前面提到的语言(Perl, Python, PHP)-都能处理循环引用, 但这个已经超过了本手册的范围. 接下来, 我们将更进一步了解JVM的办法.

### **标记和清扫**
首先, JVM对如何定义一个对象的可达性有更明确的定义. 与前面章节中用模糊的绿色云朵来表示的特殊对象不同, JVM有一个非常明确和清晰的对象集合称为GC Roots:
  - 局部变量
  - 活跃线程
  - 静态域
  - JNI应用

标记和清扫是JVM用来跟踪所有可达(活/live)对象以及保证内存被不可达对象释放后可被从用的常见算法. 它包括以下2步:
  - **标记** 遍历所有从GC Roots能够达到的对象并在本地内存中记录一个这些对象的总账
  - **清扫** 保证所有被不可达对象占用的内存都能够在现在内存分配的时候使用
JVM中不同的GC 算法比如:*Parallel Scavenge, Parallel Mark+Copy , CMS*在实现这些阶段都有细微的不同, 但是从概念上来看都跟上面2步差不多.

这种的办法的一个关键点就是解决了循环导致的内存泄漏:
![标记清扫解决循环引用](res/gcbook/mark-sweep-solve-cyclic.png)
一个不太好的事情就是, 垃圾回收时,应用线程需要被暂停. 因为你没法统计引用如果它一直在变的话. 这被称为*Stop The world pause (STW)* - 当应用被临时暂停来让JVM有机会可以来做一些内存管理工作(housekeeping). 这可能有各种因素触发, 但是GC是最常见的一个.

在本手册中, 我们会解释GC 在JVM中是如何工作的, 以及如何达到最好的状态来减少STW.
# Java中的GC
前面关于标记和清扫 GC 是一个比较理论化的介绍. 在现实中, 应用了很多调整来适应现实世界中的场景和需求. 一个简单的例子, 来让我们看下JVM需要做怎样的记录来保证我们可以持续的生成对象.
## **碎片和压缩**
无论什么时候,当清扫发生时, JVM必须保证被不可达占用的内存空间可以被重用. 这会(最终)导致内存碎片, 跟磁盘碎片类似, 并导致如下问题:
  - 为了找到合适大小的内存块,写操作变得耗时更长
  - 当创建新对象时, JVM申请连续的内存块. 所以如果有越来越多的碎片,会导致没有一个足够大的内存块可以容纳下新创建的对象, 分配失败的错误就会产生

为了避免这个问题, JVM会保证碎片得到控制. 所以除了标记和清扫外, 在GC的过程也会整理内存碎片, 跟整理磁盘碎片类似. 这个过程重新定位可达对象,让其一个接一个来消除(减少)碎片. 如下图:
![内存压缩](res/gcbook/memory-compact.png)

## **分代假设**
正如我们前面说到的, 垃圾回收会完全停止应用. 很显然, 对象越多就需要更久的时间. 但如果我们有可能在更小的内存区域上来做GC呢? 在探索各种方式后, 有些学者发现大多数的应用的内存分配都可以分为以下2类:
  - 大多对象很快就没用了
  - 其他的一般都会存活很长的一段时间
这些发现最终导出了弱分代假设. 基于这个假设, VM中的内存被分为2类: **年轻代(Young Generation)**和**老年代(Old Generation/Tenured)**.
![对象年龄](res/gcbook/objects-age.png)
当有了这个比较区分和独立的区域划分后, 便有了后来很多GC算法的各种性能优化的方案.

并不是说这样的方法就没有问题. 比如不同代的对象可能会相互应用, 这在GC中被称为实际上的(de facto)GC roots.

但更重要的是, 分代假设可能不适用于有些应用. 因为GC 算法对很快死掉和很可能存活的对象做了优化, 那么在对于中等存活期望的场景下就有比较糟糕的性能.
## **内存池**
如下对堆内存的划分对很多人都比较熟悉了. 很多人不了解的是GC如何对不同的内存池做回收的. 注意到不同的GC算法可能在实现细节上有所不同, 但同样的, 本章里面所讲的都基本一致.
![内存池](res/gcbook/memory-pool.png)

### **Eden**
Eden 区是大多数对象创建时分配的地方. 而且经常有多线程同时创建的情况, 所以Eden区被分为1个或者多个**线程局部分配缓冲(Thread Local Allocation Buffer, TLAB)**. 这些缓冲可以让一个线程直接在自己的TLAB快速分配很多对象并且减少了与其他线程的同步.

当没法在一个TLAB中分配时(大多数情况是因为空间不足), 分配会在一个共享的Eden区域进行.如果这里也没有足够的空间, 就会触发一个在年轻代上的GC过程来释放更多的空间. 如果这次GC也没有在Eden区中生成足够的空来内存, 这个对象就会在老年代上分配.
![tlab](res/gcbook/TLAB.png)

当Eden区在收集过程中, GC遍历所有的从Roots中可达的对象,并标记它们是存活的.

我们前面提到, 会有些对象有跨代的链接, 所以一个简单的办法就是检查所有从其他代到Eden的引用. 尝试这样做会打破我们最前面的分代假设. JVM 使用了一个很取巧的办法: *card-marking*. 本质上来说, JVM只会粗略标记脏对象(可能有年老代中的对象引用他们)在Eden中的位置. 详细可以参考这篇![博客](http://psy-lob-saw.blogspot.com/2014/10/the-jvm-write-barrier-card-marking.html).

在标记阶段结束后,所有在Eden区中的存活对象都被拷贝到2个Survivor区中的其中一个. 现在整个Eden区被认为是空的并可被用来分配更多对象. 这种方式被称为标记并拷贝:标记所有存活的对象然后拷贝(不是移动)到一个Survivor中.
### **Survivors**
与Eden区相邻的是2个Survivor区分别被称为from和to. 需要认识到其中一个Survivor总是是空的.

空的Survivor区会被用来分配给下次从年轻代存活下来的对象. 所有在年轻代存活下来的对象(包括Eden区和另外一个非空的from区)都被拷贝到Survivor的to区. 当这个步骤完成后, to区就包含对象, 但是from区没有. 然后下一次它们变交换了.
![young gc](res/gcbook/eden-to-survivor.png)

在2个Survivor之间拷贝存活的对象的过程会重复执行多次, 直到有些对象已经成熟并且足够老了. 记住我们基于分代的假设, 那些已经存活一段时间的对象会被期望能够存活更长的时间.

这些成熟的对象会被**提升**到老年代. 当这个发生时, 这些对象不会再从from拷贝到to而是直接到老年代.他们会一直在那儿直到被认为不可达.

实际的成熟阈值可以动态调整, JVM提供参数 *-XX:+MaxTenuringThreshold*来指定上限. 当设置*-XX:+MaxTenuringThreshold=0*,对象会立即提升到老年代而不会拷贝到Survivor区. 默认的值是15. 这也是HotSpot的最大值.

当Survivor的大小不够容纳年轻代所有存活的对象时, 提升会提前发生.
### **老年代**
老年代的实现就更加复杂了. 老年代一般都更大, 而且存放的对象都不太可能被认为是垃圾.

老年代GC的频率比年轻代GC的频率低得多. 而且因为大多数在老年代的对象都预期是存活的, 所以不会发生标记和拷贝. 取而代之的是, 这些对象会被移动来最小化碎片. 清理老年代的算法可能不同. 总的来说, 会经历如下的步骤:
  - 通过设置标记位来标记所有从GC Roots可达的对象
  - 删除所有的不可达对象
  - 通过拷贝(*译注:应该是移动*)所有的存活对象到老年代的头部来压缩老年代的内存空间

从描述可以看出, 老年代的GC必须精确压缩来避免大量的碎片.

### **持久代**
在Java 8 之前, 有个特殊的区域称为持久代. 这里存放了一些元数据(比如类信息). 一些额外的东西比如内在的string(*译注string.intern方法*)也会放在这里. 这也给Java开发造成了很多的困难,因为很难预测这个空间到底要多大. 失败的预测会导致如下的异常:
<p style="color:  #33ccff;font-size: 15px;">java.lang.OutOfMemoryError: Permgen space</p>
与其他的真的内存泄漏-OutOfMemoryError不同, 解决该问题的方案是,增大持久代的大小. 可以通过如下的配置来设置持久代为256MB:
```
java -XX:MaxPermSize=256m com.mycompany.MyApplication
```

### **元空间**
因为要预测元数据的大小实在是太难了而且也不方便, 所以在Java 8 中, 持久代被移除, 取而代之的是使用元空间. 从此开始, 大多数混杂的对象都被从常规Java 堆中移除.

类定义现在也被存入元空间. 它归属于本地(native)内存, 而且不干扰java heap中的常规对象. 默认情况下, 元空间的大小受限于java进程的本地内存可用大小. 这避免了因为程序员添加了一个类导致的 ***java.lang.OutOfMemoryError: Permgen space***. 不限制空间同时意味着风险-让元空间无限制的增长会导致大量的内存换页或者本地内存分配失败.

当你想象限制持久代一样限制元空间的大小, 你可以使用如下的配置:
```
java -XX:MaxMetaspaceSize=256m com.mycompany.MyApplication
```

## **Minor GC, Major GC, Full GC**
清理堆内存中不同区域的GC事件也被称为Minor GC, Major GC, Full GC. 本章中我们会见到不同事件之间的区别. 这些时间的区别也没有很大的相关性.

与GC相关的常见指标就是应用是否满足了SLA(Service Level Agreement) 也就是说是否满足了延迟性或者吞吐量指标. 然后才是GC 事件与结果之间的关联. 更为重要的是, 这些事件是否会停止应用以及停多久.

但是因为Minor, Major和Full GC被广泛使用而且也没有合适的定义, 让我们来更仔细的看下它到底是啥.

### **Minor GC**
在Young区的垃圾回收被称为**Minor GC**. 这个定义很清楚也被广泛接受. 但是还是有很多知识你需要意识到在处理Minor GC事件时:
  1. Minor GC总是在JVM无法为新建对象分配空间时触发. 比如Eden满了. 所以越高的对象分配率意味着更频繁的Minor GC.
  2. 在Minor GC过程中, 老年代被忽略了, 所有从老年代到年轻代的应用都被作为GC Roots. 从年轻代到老年代的应用在标记阶段就被忽略了.
  3. 与常识违背的是, Minor GC也会触发STW暂停, 挂起应用线程. 对于大多数应用而言, 如果大多数对象都被认为是垃圾而且从不拷贝到Survivor/Old区, 暂停的时间是微不足道. 相反的, 如果大多数新生对象都不是垃圾, Minor GC暂停时间就会占用更长的时间.

### **Major GC vs Full GC**
值得注意的是, 无论是JVM规范还是GC得研究论文里面都没有关于这2个的正式定义. 但是第一眼想到的是, 基于我们对Minor GC用于清理年轻代的认识上, 我们不难得出如下定义:
  - **Major GC** 是清理老年代的
  - **Full GC**用来清理整个堆包括年轻代和老年代

不幸的是, 这个可能更加复杂和迷惑. 很多情况下, Major GC是由Minor GC触发的, 想要把它们2个分开是不可能的. 另一方面, 现代的GC算法, 比如G1, 会不停的清理一部分垃圾(所以清理只能是半对的).

从这点我们知道, 我们没必要担心GC是Major 还是 Full GC. 我们更应该关注于这个GC是否会暂停所有的应用线程还是可以和应用线程并发执行.

这个迷惑甚至还体现在JVM的标准工具上. 我们可以通过下面的例子来看下. 我们将比较当某个JVM使用CMS (Concurrent Mark And Sweep) GC时的输出:

让我们先来看下[jstat](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jstat.html)的输出:
![jstat](res/gcbook/jstat-output1.png)
这个片段是从一个JVM启动后的前17s的输出. 根据输出信息, 我们可以知道一共进行了12次Minor GC和2次Full GC用时 50ms. 你可以用GUI工具jconsole或者jvisualvm得出同样的结果.
*译注: YGC: Young GC次数, FGC: Full GC次数, FGCT: Full GC时间, GCT:总GC时间*

在下结论之前, 我们还看下同一个JVM使用另一个工具打印的GC日志(使用*-XX:+PrintGCDetails*我们可以看到一个不同的而且更详细的信息):
```
java -XX:+PrintGCDetails -XX:+UseConcMarkSweepGC eu.plumbr.demo.GarbageProducer
```
![PrintGCDetails](res/gcbook/gcdetail-output1.png)
基于这个信息我们可以知道, 经过12次Minor GC后, 开始发生一些不同的事情了, 与2次Full GC不同的是, 实际上是一个单次的老年代GC, 并且包含如下阶段:
  - 初始标记阶段(Initial Mark), 耗时:0.0041705s(接近4ms). 该阶段是STW的. 所有的应用线程都被暂停等待初始标记完成.
  - 标记和预清理阶段(Markup and Preclean), 与应用线程并发执行
  - 最终重标记阶段(Final Remark), 耗时:0.0462010s(接近46ms). 该阶段也是STW.
  - 清扫阶段(Sweep). 并发执行而且不会暂停应用线程.

所以我们能从GC日志中看到的, 并不是2次Full GC而是只有1次Major GC来清理老年代.

如果你追求的是延时, 那么从*jstat*的输出就可以让你得出正确的结论. 它正确的列出了两次STW事件耗时总共50ms, 这个会影响当时应用程序的延时. 但如果你尝试优化吞吐量, 你就会被误导了 - *jstat*的输出完全掩盖了并发的工作而只展示了STW 的初始标记和最终重标记阶段.

# GC 算法: 基础
在我们尝试解读GC算法之前, 定义常见的术语和基本原则将更有利于我们理清楚GC实现. 不同的GC算法可能会有不同的实现细节, 但是大多数情况下, 所有的GC算法都关注如下2个领域:
  - 找到所有存活的对象
  - 清除其他所有的东西 -可能死亡的和无用的对象

第一部分, 存活对象的统计都是通过一个叫做**标记Marking**的过程实现的.
## **标记可达对象**
每一个现代的GC都是从找到所有存活的对象开始工作的. 这个概念可以分好的通过下面的图解释(前面讲JVM内存布局的时候说过):
![marksweep](res/gcbook/mark-sweep-solve-cyclic.png)
首先, GC定义了一些特殊的对象叫做GC Roots. 可能的GC Roots有:
  - 局部变量和当前执行方法的输入参数
  - 活跃线程
  - 已加载的class的静态域
  - JNI应用

然后, GC开始遍历整个内存来得到一个完整的对象图(object graph). 从GC Roots开始, 然后找到从GC Roots链接的其他对象, 比如实例域. 每个被GC访问到的对象都被**标记**为存活.

活着的对象在上图中被标记为蓝色. 当标记阶段结束时, 所有存活的对象都被标记了. 所有其他的对象(图中的灰色数据结构)都是从GC Roots不可达的对象, 也就暗示了你的应用再也不能使用这些不可达对象了. 这些对象就会被认为是垃圾. GC应该在后面的阶段清除它们.

在标记阶段, 需要注意到以下方面:
  - 应用线程需要被暂停, 因为你没法真正遍历整个图如果它一直在变的话. 当线程被临时暂停, 然后JVM有机会来参与管理(Housekeeping)工作的场景被称为**安全点(Safe Point)**, 同时会导致**Stop The World(STW)**暂停. 安全点可能被各种原因触发, 但是目前为止, GC是最常见的一个. *译注, 还有代码逆优化, 刷新代码缓存, 类从定义(hotswap or instrumentation), 偏向锁撤销, debug操作(比如死锁检测, 堆栈dump)都会触发safepoint* [link](http://blog.ragozin.info/2012/10/safepoints-in-hotspot-jvm.html)
  - 暂停的时间并不取决于整个堆中对象的个数, 也不取决于整个堆的大小, 而是取决于**存活对象**的个数. 所以增大堆的大小并不会直接影响标记阶段的时间.

当**标记**完成狗, GC就可以继续下一步-移除不可达对象

## **移除不可达对象**
不同的GC算法可能会常用不同的方式来移除不可达对象, 可分为以下3种:
  - 清扫 Sweeping
  - 压缩 Compacting
  - 拷贝 Copying

### **清扫**
**标记和清扫Mark and Sweep**算法在概念上选用了最简单的方式来处理垃圾 - 忽略它们.这也就是说, 当标记阶段完成后, 所有被未访问到的对象说占用的空间都被认为是空闲的,可以被用来分配新的对象.

这个方法需要一个所谓的**空闲列表free-list** 来记录每个空闲区间和它的大小. 管理空闲列表给对象分配增加了额外的负担. 这个方式的另一个弱点就是-有很多小的空闲区间,但是没有一个足够大的区间来分配对象. 分配还是会失败(也就是Java中的**OutOfMemoryError**)
![free-list](res/gcbook/free-list-sweep.png)

### **压缩**
**标记,清扫,压缩Mark-Sweep-Compact**解决了前面和清扫的问题--移动所有标记了的(也就是存活的)对象到内存的最前面. 这样做的缺点就是会增加GC暂停的时间,因为我们需要拷贝所有对象到一个新的地方然后更新这些对象的引用. 这样的好处也是显而易见的-- 通过压缩操作后, 新对象的分配变得非常的简单, 只需要通过指针碰撞(pointer bumping)就可以了. 通过这样的方式, 我们总是可以知道可用空间的大小, 而且也没有了碎片问题.
![mark-sweep-compact](res/gcbook/mark-sweep-compact.png)


### **拷贝**
**标记和拷贝Mark and Copy**算法与标记和压缩算法很像, 他们都会重新定位所有的存活对象. 一个重要的不同就是copy会将所有存活的对象拷贝到一个完全新的内存区域.标记和拷贝方法有一个优点就是, 它能够在标记的同时进行拷贝操作. 缺点就是, 他需要更多更大的内存空间来容纳存活的对象.
![mark-copy](res/gcbook/mark-copy.png)

# GC 算法: 实现
现在我们已经回顾了GC算法背后的核心概念, 接下来我们会介绍特定算法的JVM实现. 一个重要的事情我们必须意识到的是, 对于大多数的JVM而言, 我们都需要2个不同的GC算法--一个用来清理年轻代, 一个用来清理老年代.

你可以从JVM中绑定的一系列算法中选择. 如果你不指定, 那么就会用一个平台相关的算法作为默认. 在这个章节, 每个算法的工作原理都会被讲到.

作为一个快速参考, 下面的列表可以帮你快速参考哪些算法是可以组合在一起的. 注意到这个适用于Java 8, 老的Java版本可能有所不同:
![gc-combinations](res/gcbook/gc-combinations.png)
如果上面的看起来很复杂, 不用担心, 现实中, 只会用到4个加粗了的组合. 其他的都被废弃了, 不支持或者在现实生活中使用不太实际. 所以下面的章节会介绍如下4种组合的GC算法实现:
 - Serial GC (适宜用于年轻代和老年代)
 - Parallel GC (适用于年轻代和老年代)
 - Parallel New (年轻代) 和 Concurrent Mark and Sweep (CMS) (老年代)
 - G1 适用于年轻代和老年代被没有分开的情况

## **Serial GC**
这个GC对年轻代使用[标记和拷贝](#拷贝), 对老年代使用[标记清扫和压缩](#压缩). 正如名称所暗示的那样, 这2个收集器都是单线程的收集器, 不能并行执行任务. 这2个收集器都会触发STW, 暂停所有应用线程.

这个GC算法不能发挥现代硬件中多核CPU的优势, 不管CPU有多少个核, 在GC时, 都只会使用一个核.
可以通过下面的脚本来使用该GC算法:
```
java -XX:+UseSerialGC com.mypackages.MyExecutableClass
```
这个选项应该在只有几百M大小的堆内存以及在单核CPU环境下使用. 大多数的服务器环境都不太会是单核CPU. 所以当你在有多核的服务器上使用该选项来启动应用时, 会人为的设置一个可用的系统资源的上限. 这会导致限制的资源并不能用来降低延时或者提高吞吐量.

让我们来看下使用Serial GC时, GC 日志会打印什么有用的信息. 你需用通过如下的参数来打开GC日志:
```
–XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps
```
输出与下图类似:
> 2015-05-26T14:45:37.987-0200: 151.126: [GC (Allocation Failure) 151.126: [DefNew: 629119K->69888K(629120K), 0.0584157 secs] 1619346K->1273247K(2027264K), 0.0585007 secs] [Times: user=0.06 sys=0.00, real=0.06 secs]

> 2015-05-26T14:45:59.690-0200: 172.829: [GC (Allocation Failure) 172.829: [DefNew: 629120K->629120K(629120K), 0.0000372 secs]172.829: [Tenured: 1203359K- >755802K(1398144K), 0.1855567 secs] 1832479K->755802K(2027264K), [Metaspace: 6741K- >6741K(1056768K)], 0.1856954 secs] [Times: user=0.18 sys=0.00, real=0.18 secs]


这段简短的片段给出很多JVM内部发生的信息. 事实上,上面的片段包括2个GC事件, 一个是清理年轻代, 一个是清理整个堆. 让我们从年轻代的GC开始分析.

### **Minor GC**
下面的片段包括一次在年轻代发生的GC事件的信息:

> 2015-05-26T14:45:37.987-0200<sup>1</sup>: 151.126<sup>2</sup>: [GC <sup>3</sup>(Allocation Failure<sup>4</sup>) 151.126: [DefNew<sup>5</sup>: 629119K->69888K<sup>6</sup>(629120K)<sup>7</sup>, 0.0584157 secs] 1619346K->1273247K<sup>8</sup>(2027264K)<sup>9</sup>, 0.0585007 secs<sup>10</sup>] [Times: user=0.06 sys=0.00, real=0.06 secs]<sup>11</sup>

  1. *2015-05-26T14:45:37.987-0200* GC 开始时间
  2. *151.126* GC开始时距JVM启动多少秒
  3. *GC* 标志是Minor 还是 Full GC. 这一次是Minor GC.
  4. *Allocation Failure* GC触发原因. 这一次是因为无法在年轻代中的任何一区分配对象导致.
  5. *DefNew* GC收集器的名字. 这个神秘的名字代表了单线程的标记和拷贝STW收集器被用来在年轻代收集.
  6. *629119K->69888K* 年轻代回收前和回收后的空间.
  7. *(629120K)* 年轻代总大小
  8. *1619346K->1273247K* 堆的回收前和回收后的空间
  9. *2027264K* 总的可用堆空间
  10. *0.0585007 secs* GC耗时
  11. *[Times: user=0.06 sys=0.00, real=0.06 secs]* GC持续时间, 分为如下3类:
          - user GC线程在GC期间所用的总的CPU时间
          - sys  系统调用和等待系统事件的时间
          - real 应用线程总的停顿时间. 因为Serial GC始终只有一个线程, 所以real time等于user+sys.

从上面的片段, 我们可以精确了解JVM在GC过程中的内存消耗. 收集前, 总共有1619346K空间, 其中年轻代用了 629119K. 从此我们可以知道老年代用了 990227K.

从后面的一系列数字, 我们可以知道, 经过这次收集后, 年轻代降低了559231K, 总的堆空间降低了346099K. 从这里我们可以知道有213132K的对象从年轻代提升到了老年代.

下图给出了GC前和GC后内存使用的快照:
![serial-gc-minor](res/gcbook/serial-gc-minor.png)

### **Full GC**
当我们理解了第一个Minor GC事件后, 让我们来看下日志中的第二个事件:
> 2015-05-26T14:45:59.690-0200<sup>1</sup>: 172.829<sup>2</sup>: [GC (Allocation Failure) 172.829: [DefNew: 629120K->629120K(629120K), 0.0000372 secs<sup>3</sup>]172.829: [Tenured<sup>4</sup>: 1203359K- >755802K<sup>5</sup>(1398144K)<sup>6</sup>, 0.1855567 secs<sup>7</sup>] 1832479K->755802K<sup>8</sup>(2027264K)<sup>9</sup>, [Metaspace: 6741K- >6741K(1056768K)]<sup>10</sup>, 0.1856954 secs] [Times: user=0.18 sys=0.00, real=0.18 secs]<sup>11</sup>

  1. *2015-05-26T14:45:59.690-0200* GC 开始时间
  2. *172.829* GC开始时距JVM启动多少秒
  3. *[DefNew: 629120K->629120K(629120K), 0.0000372 secs* 与前面类似, 因为分配失败Allocation Failure, 在年轻代发生了一次Minor GC. 对于这次收集, 使用DefNew收集器让年轻代的空间从629120K降到了0. 这里JVM报的事件有点问题(报年轻代还是满的), 这是JVM的bug. 这次回收耗时0.0000372秒
  4. *Tenured* 用于老年代的垃圾回收器名字. Tenured 意味着使用了单线程的STW 标记+清扫+压缩 GC回收器.
  5. *1203359K->755802K* 老年代回收前和回收后的空间.
  6. *(1398144K)* 老年代总空间
  7. *0.1855567 secs* 回收老年代所用时间
  8. *1832479K->755802K* 堆的回收前和回收后的空间
  9. *(2027264K)* 当前JVM堆可用总空间
  10. *[Metaspace: 6741K->6741K(1056768K)]* 元空间的垃圾回收, 可以看见这次没有发现垃圾.
  11. *[Times: user=0.18 sys=0.00, real=0.18 secs]* GC持续时间, 分为如下3类:
          - user GC线程在GC期间所用的总的CPU时间
          - sys  系统调用和等待系统事件的时间
          - real 应用线程总的停顿时间. 因为Serial GC始终只有一个线程, 所以real time等于user+sys.

与Minor GC有明显的区别, 这次GC中, 老年代和元空间也被清理了. 下图给出了GC前和GC后内存使用的快照:
![serial-gc-full](res/gcbook/serial-gc-full.png)

## **Parallel GC**
这个GC对年轻代使用[标记和拷贝](#拷贝), 对老年代使用[标记清扫和压缩](#压缩). 年轻代和老年代GC都会触发STW事件, 停止所有应用线程来执行垃圾回收. 这2个收集器在拷贝/压缩阶段都使用了多线程, 这就是为啥是'Parallel'的原因. 通过这种方式降低了垃圾回收的时间.

GC时的线程数可以通过命令行参数*-XX:ParallelGCThreads=NNN*设置. 默认值是机器的核数.

下面的任何一个JVM启动脚本都会选择Parallel GC:
```
java -XX:+UseParallelGC com.mypackages.MyExecutableClass
java -XX:+UseParallelOldGC com.mypackages.MyExecutableClass
java -XX:+UseParallelGC -XX:+UseParallelOldGC com.mypackages.MyExecutableClass
```
Parallel GC 适用于多核系统, 而且你的主要目标是提交吞吐量. 高的吞吐量是通过对系统资源的有效使用实现的:
  - 收集时, 所有的cpu 核都会用来并行清理垃圾, 这缩短了暂停时间
  - 在GC周期间, 收集器不会消耗任何资源.

另一方面, 垃圾收集的所有阶段都不能被中断, 这些收集器当你的应用线程被暂停时,很容易有很长的暂停. 所以如果延时是你的主要目标, 你需要看看下一个GC组合.

现在让我们来看下使用Parallel GC时,GC日志的输出. 下面的GC日志包含了1次Minor GC和一次Major GC:
> 2015-05-26T14:27:40.915-0200: 116.115: [GC (Allocation Failure) [PSYoungGen: 2694440K- >1305132K(2796544K)] 9556775K->8438926K(11185152K), 0.2406675 secs] [Times: user=1.77 sys=0.01, real=0.24 secs]

> 2015-05-26T14:27:41.155-0200: 116.356: [Full GC (Ergonomics) [PSYoungGen: 1305132K- >0K(2796544K)] [ParOldGen: 7133794K->6597672K(8388608K)] 8438926K->6597672K(11185152K), [Metaspace: 6745K->6745K(1056768K)], 0.9158801 secs] [Times: user=4.49 sys=0.64, real=0.92 secs]

### **Minor GC**
第一条事件是年轻代的Minor GC:
> 2015-05-26T14:27:40.915-0200<sup>1</sup>: 116.115<sup>2</sup>: [GC <sup>3</sup>(Allocation Failure<sup>4</sup>) [PSYoungGen<sup>5</sup>: 2694440K- >1305132K<sup>6</sup>(2796544K)<sup>7</sup>] 9556775K->8438926K<sup>8</sup>(11185152K)<sup>9</sup>, 0.2406675 secs<sup>10</sup>] [Times: user=1.77 sys=0.01, real=0.24 secs]<sup>11</sup>

  1. *2015-05-26T14:27:40.915-0200* GC 开始时间
  2. *116.115* GC开始时距JVM启动多少秒
  3. *GC* 标志是Minor 还是 Full GC. 这一次是Minor GC.
  4. *Allocation Failure* GC触发原因. 这一次是因为无法在年轻代中的任何一区分配对象导致.
  5. *PSYoungGen* GC收集器的名字, 这里代表了一个并行的标记拷贝 STW收集器被用来清理年轻代
  6. *2694440K->1305132K* 年轻代回收前和回收后的空间.
  7. *(2796544K)* 年轻代总大小
  8. *9556775K->8438926K* 堆的回收前和回收后的空间
  9. *(1118512K)* 总的可用堆空间
  10. *0.2406675 secs* GC耗时
  11. *[Times: user=1.77 sys=0.01, real=0.24 secs]* GC持续时间, 分为如下3类:
          - user GC线程在GC期间所用的总的CPU时间
          - sys  系统调用和等待系统事件的时间
          - real 应用线程总的停顿时间. 对Parallel GC而言, 该值应大致等于 (user+system)/GC所用线程数.
          - 在这个例子中,使用了8个线程. 因为有些活动没有被并行化, 所以真实值总是比计算的值大一点.

所以, 简单来说, 总的堆占用在收集前是9556775K, 其中年轻代占用2694440K, 老年代也就是6862335K.收集后,年轻代降低了1389308K, 但是总的堆只降低了1117849K. 则意味着有271459K对象从年轻代提升到老年代.
 ![parallel-gc-minor](res/gcbook/parallel-gc-minor.png)

### **Full GC**
当我们了解了Parallel GC是如何清理年轻代后, 让我们来接着看下下一条GC日志中是如何清理整个堆的:
> 2015-05-26T14:27:41.155-0200<sup>1</sup>: 116.356<sup>2</sup>: [Full GC<sup>3</sup> (Ergonomics<sup>4</sup>) [PSYoungGen: 1305132K- >0K(2796544K)]<sup>5</sup> [ParOldGen<sup>6</sup>: 7133794K->6597672K<sup>7</sup>(8388608K)<sup>8</sup>] 8438926K->6597672K<sup>9</sup>(11185152K)<sup>10</sup>, [Metaspace: 6745K->6745K(1056768K)]<sup>11</sup>, 0.9158801 secs<sup>12</sup>] [Times: user=4.49 sys=0.64, real=0.92 secs]<sup>13</sup>

  1. *2015-05-26T14:27:41.155-0200* GC 开始时间
  2. *116.356* GC开始时距JVM启动多少秒
  3. *Full GC* 标志这是一次Full GC. 清理整个堆
  4. *Ergonomics* GC发生原因. 这里表示JVM内部决定现在是进行GC的合适时间.
  5. *[PSYoungGen: 1305132K- >0K(2796544K)]* 与前面类似, 一个名叫PSYoungGen的并行标记拷贝收集器被用来收集年轻代.年轻代的占用从1305132K变为了0, 在Full GC后, 整个年轻代都空了出来.
  6. *ParOldGen* 老年代回收器的名字. 一个名叫ParOldGen的标记+清扫+压缩的STW垃圾收集器被用来收集老年代.
  7. *7133794K->6597672K* 老年代回收前和回收后的空间.
  8. *(8388608K)* 老年代总空间
  9. *8438926K->6597672K* 堆的回收前和回收后的空间
  10. *(11185152K)* 总的可用堆空间
  11. *[Metaspace: 6745K->6745K(1056768K)]* 元空间的垃圾回收, 可以看见这次没有发现垃圾.
  12. *0.9158801 secs* GC耗时
  13. *[Times: user=4.49 sys=0.64, real=0.92 secs]* GC持续时间, 分为如下3类:
          - user GC线程在GC期间所用的总的CPU时间
          - sys  系统调用和等待系统事件的时间
          - real 应用线程总的停顿时间. 对Parallel GC而言, 该值应大致等于 (user+system)/GC所用线程数.
          - 在这个例子中,使用了8个线程. 因为有些活动没有被并行化, 所以真实值总是比计算的值大一点.

同样的, 与Minor GC有明显的区别, 这次GC中, 老年代和元空间也被清理了. 下图给出了GC前和GC后内存使用的快照:
![parallel-gc-full](res/gcbook/parallel-gc-full.png)

## **Concurrent Mark and Sweep**
官方名字是"最多并发的标记和清楚垃圾回收器". 它使用[标记拷贝](#拷贝)STW算法来收集年轻代和最多并发的[标记和清扫](#清扫)算法来清理老年代.

这个收集器被设计来在GC时避免长暂停. 它通过2种方式来实现. 首先, 它不会压缩老年代而是使用空闲列表来管理可回收空间. 其次, 它大多数阶段都是与应用线程并行执行. 这意味着GC不会完全停止应用线程, 而且它也使用多线程来收集. 默认的垃圾线程个数是核数/4.

这个GC算法可以通过如下的选项启用:
```
java -XX:+UseConcMarkSweepGC com.mypackages.MyExecutableClass
```

如果你的主要目标是低时延, 这个组合非常适合在多核系统上. 降低了各个GC 暂停时间, 最直接的影响就是减少了终端用户对应用暂停的感知, 并且让它们觉得反应更加及时了. 因为大多数时间内,只有小部分CPU时间被GC占用来而且没有执行你的应用代码, 所以CMS的吞吐量表现上比Parallel GC会差些.

同前面的一样, 让我们来看下这个GC日志, 其中包含1次Minor 和一次Major GC:
> 2015-05-26T16:23:07.219-0200: 64.322: [GC (Allocation Failure) 64.322: [ParNew: 613404K- >68068K(613440K), 0.1020465 secs] 10885349K->10880154K(12514816K), 0.1021309 secs] [Times: user=0.78 sys=0.01, real=0.11 secs]

> 2015-05-26T16:23:07.321-0200: 64.425: [GC (CMS Initial Mark) [1 CMS-initial-mark: 10812086K(11901376K)] 10887844K(12514816K), 0.0001997 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]

> 2015-05-26T16:23:07.321-0200: 64.425: [CMS-concurrent-mark-start]

> 2015-05-26T16:23:07.357-0200: 64.460: [CMS-concurrent-mark: 0.035/0.035 secs] [Times: user=0.07 sys=0.00, real=0.03 secs]

> 2015-05-26T16:23:07.357-0200: 64.460: [CMS-concurrent-preclean-start]

> 2015-05-26T16:23:07.373-0200: 64.476: [CMS-concurrent-preclean: 0.016/0.016 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]

> 2015-05-26T16:23:07.373-0200: 64.476: [CMS-concurrent-abortable-preclean-start]

> 2015-05-26T16:23:08.446-0200: 65.550: [CMS-concurrent-abortable-preclean: 0.167/1.074 secs] [Times: user=0.20 sys=0.00, real=1.07 secs]

> 2015-05-26T16:23:08.447-0200: 65.550: [GC (CMS Final Remark) [YG occupancy: 387920 K (613440 K)]65.550: [Rescan (parallel) , 0.0085125 secs]65.559: [weak refs processing, 0.0000243 secs]65.559: [class unloading, 0.0013120 secs]65.560: [scrub symbol table, 0.0008345 secs]65.561: [scrub string table, 0.0001759 secs][1 CMS-remark: 10812086K(11901376K)] 11200006K(12514816K), 0.0110730 secs] [Times: user=0.06 sys=0.00, real=0.01 secs]

> 2015-05-26T16:23:08.458-0200: 65.561: [CMS-concurrent-sweep-start]

> 2015-05-26T16:23:08.485-0200: 65.588: [CMS-concurrent-sweep: 0.027/0.027 secs] [Times: user=0.03 sys=0.00, real=0.03 secs]

> 2015-05-26T16:23:08.485-0200: 65.589: [CMS-concurrent-reset-start]

> 2015-05-26T16:23:08.497-0200: 65.601: [CMS-concurrent-reset: 0.012/0.012 secs] [Times: user=0.01 sys=0.00, real=0.01 secs]

### **Minor GC**
GC日志中的一个代表一次年轻代的GC事件:
> 2015-05-26T16:23:07.219-0200<sup>1</sup>: 64.322<sup>2</sup>: [GC <sup>3</sup>(Allocation Failure<sup>4</sup>) 64.322: [ParNew<sup>5</sup>: 613404K- >68068K<sup>6</sup>(613440K)<sup>7</sup>, 0.1020465 secs<sup>8</sup>] 10885349K->10880154K<sup>9</sup>(12514816K)<sup>10</sup>, 0.1021309 secs<sup>11</sup>] [Times: user=0.78 sys=0.01, real=0.11 secs]<sup>12</sup>

  1. *2015-05-26T16:23:07.219-0200* GC 开始时间
  2. *64.322* GC开始时距JVM启动多少秒
  3. *GC* 标志此次GC是Minor还是Full GC. 本次是Minor GC.
  4. *Allocation Failure* GC触发原因. 这一次是因为无法在年轻代中的任何一区分配对象导致.
  5. *ParNew* 使用收集器的名字. 这里表示它是一个并行的标记拷贝STW收集器来收集年轻代的. 被设计来与在老年代使用的CMS共同工作.
  6. *613404K- >68068K* 年轻代在收集前后的使用大小.
  7. *(613440K)* 年轻代总大小
  8. *0.1020465 secs* GC耗时
  9. *10885349K->10880154K* 整个堆在收集前后的大小
  10. *(12514816K)* 整个堆可用空间.
  11. *0.1021309 secs* 在年轻代GC过程中用来标记和拷贝对象所小号的时间.
  12. *[Times: user=0.78 sys=0.01, real=0.11 secs]* GC持续时间, 分为如下3类:
          - user GC线程在GC期间所用的总的CPU时间
          - sys  系统调用和等待系统事件的时间
          - real 应用线程总的停顿时间. 对Parallel GC而言, 该值应大致等于 (user+system)/GC所用线程数.
          - 在这个例子中,使用了8个线程. 因为有些活动没有被并行化, 所以真实值总是比计算的值大一点.

从上面可以看出, 在收集前, 整个堆占用了10885349K, 其中年轻代占用了613404K. 也就是说老年代用了10271945K. 经过收集后, 年轻代占用降低了545336K, 但是整个堆只降低了5195K. 所以有540141K的对象从年轻代提升到老年代.
![cms-gc-minor](res/gcbook/cms-gc-minor.png)

### **Full GC**
现在你已经比较习惯来阅读GC日志了. 这次会姐要一个完全不同格式的GC事件. 下面冗长的输出包括了在老年代使用CMS GC的不同阶段的输出. 我们会一个一个来看这些阶段相关的日志信息而不是在一次性解释所有的event. 作为回忆, 这个GC事件的完整输出应该像下面这样:
> 2015-05-26T16:23:07.321-0200: 64.425: [GC (CMS Initial Mark) [1 CMS-initial-mark: 10812086K(11901376K)] 10887844K(12514816K), 0.0001997 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]

> 2015-05-26T16:23:07.321-0200: 64.425: [CMS-concurrent-mark-start]

> 2015-05-26T16:23:07.357-0200: 64.460: [CMS-concurrent-mark: 0.035/0.035 secs] [Times: user=0.07 sys=0.00, real=0.03 secs]

> 2015-05-26T16:23:07.357-0200: 64.460: [CMS-concurrent-preclean-start]

> 2015-05-26T16:23:07.373-0200: 64.476: [CMS-concurrent-preclean: 0.016/0.016 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]

> 2015-05-26T16:23:07.373-0200: 64.476: [CMS-concurrent-abortable-preclean-start]

> 2015-05-26T16:23:08.446-0200: 65.550: [CMS-concurrent-abortable-preclean: 0.167/1.074 secs] [Times: user=0.20 sys=0.00, real=1.07 secs]

> 2015-05-26T16:23:08.447-0200: 65.550: [GC (CMS Final Remark) [YG occupancy: 387920 K (613440 K)]65.550: [Rescan (parallel) , 0.0085125 secs]65.559: [weak refs processing, 0.0000243 secs]65.559: [class unloading, 0.0013120 secs]65.560: [scrub symbol table, 0.0008345 secs]65.561: [scrub string table, 0.0001759 secs][1 CMS-remark: 10812086K(11901376K)] 11200006K(12514816K), 0.0110730 secs] [Times: user=0.06 sys=0.00, real=0.01 secs]

> 2015-05-26T16:23:08.458-0200: 65.561: [CMS-concurrent-sweep-start]

> 2015-05-26T16:23:08.485-0200: 65.588: [CMS-concurrent-sweep: 0.027/0.027 secs] [Times: user=0.03 sys=0.00, real=0.03 secs]

> 2015-05-26T16:23:08.485-0200: 65.589: [CMS-concurrent-reset-start]

> 2015-05-26T16:23:08.497-0200: 65.601: [CMS-concurrent-reset: 0.012/0.012 secs] [Times: user=0.01 sys=0.00, real=0.01 secs]

**阶段1:初始标记**这是CMS中一个STW事件, 这个阶段用来收集所有的GC Roots.
> 2015-05-26T16:23:07.321-0200: 64.425<sup>1</sup>: [GC (CMS Initial Mark<sup>2</sup>) [1 CMS-initial-mark: 10812086K<sup>3</sup>(11901376K)<sup>4</sup>] 10887844K<sup>5</sup>(12514816K)<sup>6</sup>, 0.0001997 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]<sup>7</sup>
  1. *2015-05-26T16:23:07.321-0200: 64.425* GC开始时间, 包括绝对时间和相对JVM启动的时间. 后面的阶段与此相同, 为了简单, 不再描述.
  2. *CMS Initial Mark* 阶段名称 - "Initial Mark" -- 用来收集所有GC Roots
  3. *10812086K* 当前老年代占用大小
  4. *(11901376K)* 老年代总可用大小
  5. *10887844K* 当前堆使用空间
  6. *(12514816K)* 当前堆可用空间
  7. *0.0001997 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]* 时间相关

**阶段2:并发标记**在这个阶段垃圾回收器会遍历老年代的所有存货的对象, 从初始标记阶段发现的GC Roots开始. 并发标记阶段正如名称所说那样, 会与应用程序线程并行执行, 也不会暂停应用程序线程.

> 2015-05-26T16:23:07.321-0200: 64.425: [CMS-concurrent-mark-start]

> 2015-05-26T16:23:07.357-0200: 64.460: [CMS-concurrent-mark<sup>1</sup>: 0.035/0.035 secs<sup>2</sup>] [Times: user=0.07 sys=0.00, real=0.03 secs]<sup>3</sup>

  1. *CMS-concurrent-mark* 阶段名称 -- 用来标记老年代中所有存活对象
  2. *0.035/0.035 secs* 显示阶段经过的时间.
  3. *[Times: user=0.07 sys=0.00, real=0.03 secs]* 这里对该阶段意义不大.因为它从开始并发标记开始,并且包含了不止并发标记完成的时间.

**阶段3:并发预清理**这是一个并发阶段, 与应用线程并行运行而不会暂停他们. 在前一个阶段中, 因为与应用相册那个并行运行,所以某些应用已经改变了. 那些自己的域已经改变了的对象被JVM标记为脏对象也就是Card Marking. 在预清理阶段, 这也对象也被认为是存活的.虽然这可能有误报(垃圾也会被标记为活的),但是重要的是这个避免了实际存活的对象却没有被标记.这个阶段也会做一些最终重标记阶段的准备工作.
> 2015-05-26T16:23:07.357-0200: 64.460: [CMS-concurrent-preclean-start]

> 2015-05-26T16:23:07.373-0200: 64.476: [CMS-concurrent-preclean<sup>1</sup>: 0.016/0.016 secs<sup>2</sup>] [Times: user=0.02 sys=0.00, real=0.02 secs]<sup>3</sup>

  1. *CMS-concurrent-preclean* 阶段名称 - 将前面标记阶段改变的应用当做存活对象
  2. *0.016/0.016 secs* 显示阶段经过的时间
  3. *[Times: user=0.02 sys=0.00, real=0.02 secs]* 这里对该阶段意义不大.因为它从开始并发标记开始,并且包含了不止并发标记完成的时间.


**阶段4:并发可取消预清理**这也是一个并发阶段(不会停止应用线程). 该阶段尝试尽可能减轻最终重标记阶段(STW)的工作压力.该阶段的时间与很多因素有关. 因为它不停地迭代做同一件事直到有取消条件满足(比如迭代次数,总的有意义的工作量,经历时间等)
> 2015-05-26T16:23:07.373-0200: 64.476: [CMS-concurrent-abortable-preclean-start]

> 2015-05-26T16:23:08.446-0200: 65.550: [CMS-concurrent-abortable-preclean<sup>1</sup>: 0.167/1.074 secs<sup>2</sup>] [Times: user=0.20 sys=0.00, real=1.07 secs]<sup>3</sup>

  1. *CMS-concurrent-abortable-preclean* 阶段名称
  2. *0.167/1.074 secs* 阶段持续时间. 有意思的是这里user时间比clock时间小得多. 通常情况下, 我们看到real时间比user时间小, 这就意味着某些工作被并行执行所以逝去的clock时间小于使用的cpu时间. 这里我们看到-只有0.167s的cpu时间, 然后垃圾回收线程就等待了1s左右的时间(不知道等待啥), 什么也没做.
  3. *[Times: user=0.20 sys=0.00, real=1.07 secs]* 这里对该阶段意义不大.因为它从开始并发标记开始,并且包含了不止并发标记完成的时间

**阶段5:最终重标记**这是第二个也是最后一个STW阶段. 该阶段的目的是为了最后标记老年代所有存活的对象.这意味着要从与初始标记一样的GC Roots开始来表里对象, 加上所谓的脏对象(比如那些在前面并发阶段修改过自己域的对象)

通常情况下, CMS会在年轻代尽可能空的情况下运行追踪标记, 以此来减少STW阶段一个接着一个的情况.

这个事件看起来比前面的阶段复杂一点:
> 2015-05-26T16:23:08.447-0200: 65.550<sup>1</sup>: [GC (CMS Final Remark<sup>2</sup>) [YG occupancy: 387920 K (613440 K)<sup>3</sup>]65.550: [Rescan (parallel) , 0.0085125 secs]<sup>4</sup>65.559: [weak refs processing, 0.0000243 secs]65.559<sup>5</sup>: [class unloading, 0.0013120 secs]65.560<sup>6</sup>: [scrub symbol table, 0.0008345 secs]65.561: [scrub string table, 0.0001759 secs<sup>7</sup>][1 CMS-remark: 10812086K(11901376K)<sup>8</sup>] 11200006K(12514816K)<sup>9</sup>, 0.0110730 secs<sup>10</sup>] [Times: user=0.06 sys=0.00, real=0.01 secs]<sup>11</sup>

  1. *2015-05-26T16:23:08.447-0200: 65.550* GC开始时间, 包括绝对时间和相对JVM启动的时间
  2. *CMS Final Remark* 阶段名称 - 标记老年代所有存活的对象包括在前面并发阶段创建和修改的引用.
  3. *YG occupancy: 387920 K (613440 K)* 当前年轻代占用空间和容量
  4. *Rescan (parallel) , 0.0085125 secs* 当应用被暂停时,Rescan 完成标记存活的对象. 该阶段是并行执行的, 消耗了0.0085125 secs.
  5. *[weak refs processing, 0.0000243 secs]65.559* 第一个子阶段就是处理弱应用以及消耗的时间和时间戳.
  6. *[class unloading, 0.0013120 secs]65.560*下一个子阶段就是卸载掉不使用的类以及消耗的时间和时间戳.
  7. *[scrub string table, 0.0001759 secs*最后一个子阶段就是清除符号和字符串表.保留了类级别元信息以及internalized 字符串. 消耗时间也记录在内.
  8. *10812086K(11901376K)* 该阶段完成后, 老年代的占用和容量
  9. *11200006K(12514816K)* 该阶段完成后, 整个堆的占用和容量
  10. *0.0110730 secs* 阶段耗时
  11. *[Times: user=0.06 sys=0.00, real=0.01 secs]* 暂停时间.
  
经过5个标记阶段,老年代所有存活对象都被标记了.现在收集器将要通过清扫老年代回收这些无用对象占用的空间:
**阶段6:并发清扫** 与应用线程并发执行, 不需要STW. 该阶段目的是清除无用对象并回收其占用空间以备将来之用.

> 2015-05-26T16:23:08.458-0200: 65.561: [CMS-concurrent-sweep-start]

> 2015-05-26T16:23:08.485-0200: 65.588: [CMS-concurrent-sweep<sup>1</sup>: 0.027/0.027 secs<sup>2</sup>] [Times: user=0.03 sys=0.00, real=0.03 secs]

  1. *CMS-concurrent-sweep* 阶段名称 -  清扫未被标记对象以回收空间
  2. *0.027/0.027 secs* 占用时间

**阶段7:并发重置**并发执行, 重置CMS算法中的内部数据结构,以备下次回收使用
> 2015-05-26T16:23:08.485-0200: 65.589: [CMS-concurrent-reset-start]

> 2015-05-26T16:23:08.497-0200: 65.601: [CMS-concurrent-reset<sup>1</sup>: 0.012/0.012 secs<sup>2</sup>] [Times: user=0.01 sys=0.00, real=0.01 secs]<sup>3</sup>

  1. *CMS-concurrent-reset* 阶段名称 - 重置CMS算法内部数据结构,以备下次收集使用
  2. *0.012/0.012 secs* 占用时间

总而言之, CMS 垃圾回收器通过将大量工作交给并发线程来做而且不需要暂停应用来减少暂停时间. 然而,它也有一些缺点, 最大的就是老年代的碎片化问题以及缺乏一个可预测的停顿时间, 这对于一些比较大的堆更为明显.

## **G1 - Garbage First**
G1的主要设计目标就是保证STW的时间和分布都可以很好的预测和配置. 事实上, Garbage-First 是一个*类实时*GC. 也就是你可以设定特定的性能要求. 你可以要求在给定的y ms中, STW的时间不能超过x ms. 比如在任何1s内都不超过5ms. G1 会尽量满足设定的目标(但不能完全肯定, 所以不是绝对实时的).

为了达到这个目标, G1建立在大量的见解之上. 首先, 堆不再需要被分割进连续的2个年轻代和老年代. 取而代之的是, 堆被分为很多(典型的是2048)个小的*堆区域(heap regions)*来存储对象. 每个region都可能是Eden region, 或者Survivor region, Old region. 所有Eden和Survivor 区组合成了逻辑上的年轻代, 所有的Old region组合在一起成了老年代:
![g1-pool](res/gcbook/g1-pool.png)

这让垃圾收集器不需要每次都收集整个堆,而是每次*增量*的解决问题:每次只会有所有region集合的一个子集会被考虑, 称为*收集集合Collection set*.年轻代的所有region在每个暂停的时候都被收集,但是老年代只有一部分会被收集:
![g1-collection-set](res/gcbook/g1-collection-set.png)
在并发阶段的另一个新奇的事就是G1会估计每个region中包含的存活对象的个数.这被用来构建Collection set:包含最多垃圾的region总是优先被收集. 这个是名称*Garbage-first*的由来.

像下面这样来使用G1 GC:
```
java -XX:+UseG1GC com.mypackages.MyExecutableClass
```

### **Evacuation Pause: Fully Young**
在引用的开始阶段, G1不能从还没执行过的并发阶段知道任何额外的信息, 所以它最开始工作在*fully-young*模式.当年轻代满了后,应用线程被暂停,年轻代regions的活跃对象被拷贝到Surivior regions(任何其他空闲的region因此就变成了Survivor).

拷贝的过程称为Evacuation(*译注:意为疏散*). 这与我们前面讲到的年轻代的收集器完全一样.Evacuation Pause的GC 日志非常长, 所以这里简单期间, 我们只留下了一些与第一次fully-young Evacuation Pause相关的日志. 我们随后讲到并发阶段时还会详细讲到. 除此之外, 因为日子很大, 并发阶段和其他Other阶段会在单独的段落讲到:
> 0.134: [GC pause (G1 Evacuation Pause) (young), 0.0144119 secs]<sup>1</sup>

>  [Parallel Time: 13.9 ms, GC Workers: 8]<sup>2</sup>

>  ...<sup>3</sup>

>  [Code Root Fixup: 0.0 ms]<sup>4</sup>

>  [Code Root Purge: 0.0 ms]<sup>5</sup> [Clear CT: 0.1 ms]

>  [Other: 0.4 ms]<sup>6</sup>

>  ...<sup>7</sup>

>  [Eden: 24.0M(24.0M)->0.0B(13.0M)<sup>8</sup>

>  Survivors: 0.0B->3072.0K <sup>9</sup> 

>  Heap: 24.0M(256.0M)->21.9M(256.0M)]<sup>10</sup>

>  [Times: user=0.04 sys=0.04, real=0.02 secs]<sup>11</sup>

  1. *0.134: [GC pause (G1 Evacuation Pause) (young), 0.0144119 secs]* G1暂停只清理年轻代的region. 这个暂停在JVM启动后134ms开始,暂停时间0.0144s.
  2. *[Parallel Time: 13.9 ms, GC Workers: 8]* 表示有13.9ms (real time)用于后面的8个线程的GC活动
  3. *...* 简单起见,省略了, 后面会讲到
  4. *Code Root Fixup: 0.0 ms* 用于释放管理并发活动的数据结构, 该值应该总是0. 这是顺序执行的.
  5. *[Code Root Purge: 0.0 ms]* 清理更多的数据结构, 应该会很快. 但不一定是0. 这是顺序执行的.
  6. *[Other: 0.4 ms]* 混杂的其他活动时间, 其中的许多也是并行的.
  7. *...* 后面会讲到
  8. *[Eden: 24.0M(24.0M)->0.0B(13.0M)* 暂停前后Eden的使用量和容量
  9. *Survivors: 0.0B->3072.0K * 暂停前后, Survivor的使用量.
  10. *Heap: 24.0M(256.0M)->21.9M(256.0M)]* 暂停前后, 堆得使用量和容量.
  11. *[Times: user=0.04 sys=0.04, real=0.02 secs]*GC持续时间, 分为如下3类:
                  - user GC线程在GC期间所用的总的CPU时间
                  - sys  系统调用和等待系统事件的时间
                  - real 应用线程总的停顿时间. 该值应大致等于 (user+system)/GC所用线程数.
                  - 在这个例子中,使用了8个线程. 因为有些活动没有被并行化, 所以真实值总是比计算的值大一点.

大多数繁重的工作都被多个专有GC线程完成, 下面的日志描述了它们的活动:
> [Parallel Time: 13.9 ms, GC Workers: 8]<sup>1</sup>

>   [GC Worker Start (ms)<sup>2</sup>: Min: 134.0, Avg: 134.1, Max: 134.1, Diff: 0.1]

>   [Ext Root Scanning (ms)<sup>3</sup>: Min: 0.1, Avg: 0.2, Max: 0.3, Diff: 0.2, Sum: 1.2] 

>   [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]

>   >  [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Sum: 0]

>   [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]

>   [Code Root Scanning (ms)<sup>4</sup>: Min: 0.0, Avg: 0.0, Max: 0.2, Diff: 0.2, Sum: 0.2]

>   [Object Copy (ms)<sup>5</sup>: Min: 10.8, Avg: 12.1, Max: 12.6, Diff: 1.9, Sum: 96.5]

>   [Termination (ms)<sup>6</sup>: Min: 0.8, Avg: 1.5, Max: 2.8, Diff: 1.9, Sum: 12.2]

>   >   [Termination Attempts<sup>7</sup>: Min: 173, Avg: 293.2, Max: 362, Diff: 189, Sum: 2346]


>   [GC Worker Other (ms)<sup>8</sup>: Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]

>   [GC Worker Total (ms)<sup>9</sup>: Min: 13.7, Avg: 13.8, Max: 13.8, Diff: 0.1, Sum: 110.2]

>   [GC Worker End (ms)<sup>10</sup>: Min: 147.8, Avg: 147.8, Max: 147.8, Diff: 0.0]]       

  1. *[Parallel Time: 13.9 ms, GC Workers: 8]* 表明下面的活动耗时13.9ms(clock), 由8个线程并行执行.
  2. *GC Worker Start (ms)* GC开始工作时间. 与暂停开始时间对应, 如果Min和Max相差很大,可能意味着(1)GC线程过多.或者(2)该机器上有其他的进程从GC进程*盗取*了很多cpu时间.
  3. *Ext Root Scanning (ms)* 扫描其他Roots(非堆)消耗了多少时间. 包括类加载器, JNI应用, JVM System Roots. 单位是clock 时间. "Sum"是cpu 时间.
  4. *Code Root Scanning (ms)* 扫描从实际代码(本地变量)引出的Roots的时间.
  5. *Object Copy (ms)* 从收集的regions里面拷贝存活的对象消耗的时间
  6. *Termination (ms)* GC workers用时多久来保证可以停止下来而且没有更多的事情需要做. 然后真的停止下来.
  7. *Termination Attempts* GC workers尝试终止的次数. 如果worker发现还有更多的工作需要做时,就尝试停止就被认为是一次失败的终止尝试. 因为它终止的太早了.
  8. *GC Worker Other (ms)* 其他混杂的活动的时间. 没有在日志中体现的阶段.
  9. *GC Worker Total (ms)* workers工作的总时间.
  10. *GC Worker End (ms)* workers 停止工作的时间戳. 正常情况他们应该大致相等. 否则意味着有太多的线程hangs或者有噪声.
  

除此之外, 在Evacuation pause期间,有一些其他混杂的活动. 这里我们只会涉及其中的一部分. 其他的会在后面讲到.

> [Other: 0.4 ms]<sup>1</sup>
>   [Choose CSet: 0.0 ms]
>   [Ref Proc: 0.2 ms]<sup>2</sup>
>   [Ref Enq: 0.0 ms]<sup>3</sup>
>   [Redirty Cards: 0.1 ms] [Humongous Register: 0.0 ms] [Humongous Reclaim: 0.0 ms]
>   [Free CSet: 0.0 ms]<sup>4</sup>

  1. *[Other: 0.4 ms]* 其他活动的耗时, 大多数都是并行的.
  2. *[Ref Proc: 0.2 ms]* 处理非强引用的时间: 清理或者决定是否需要清理
  3. *[Ref Enq: 0.0 ms]* 将非强引用放入对应的ReferenceQueue的时间.
  4. *[Free CSet: 0.0 ms]* 返回收集集合中的释放了的region的时间. 这些集合变为空闲可用.
  
### **并发标记** 
G1构建于前面章节的很多概念之上. 所以在继续之前,请确保你对前面的知识有充分的理解.虽然有很多方法, 但是并发标记的目标却是类似的. G1并发标记使用了一个叫Snapshot-At-The-Beginning/SATB的方法在标记阶段的开始来标记所有存活的对象, 即便它们一会儿会变成垃圾. 关于哪些对象是存活的信息可以用来构建每个region的存活统计, 以便选出高效的回收集合.
这些信息随后也被用来在老年代上进行垃圾回收. 如果标记发现某个region只有垃圾或者在老年代的STW evacuation pause阶段, 那么它可以完全并行.

并发标记在整个堆占用足够大时开始执行. 默认值是45%, 可以通过JVM选项**InitiatingHeapOccupancyPercent**设置. 与CMS类似, G1中的并发标记也包含很多阶段, 其中一些是完全并行的, 其中一些需要暂停应用线程.


**阶段1:初始标记Initial Mark**  这个阶段会标记从GC roots直接可达的对象, 在CMS中, 它需要一个单独的STW暂停, 但是在G1中, 它一般都在Evacuation Pause 阶段运行(*译注:原文piggy-backed on an Evacuation Pause*), 所以它的副作用很小. 从GC日志上可以看到在Evacuation Pause有一个额外的*(initial-mark)*标记

> 1.631: [GC pause (G1 Evacuation Pause) (young) (initial-mark), 0.0062656 secs]

 
<sup></sup>


  1. **
  2. **
  3. **
  4. **
  5. **
  6. **
  7. **
  8. **
  9. **
  10. **
  11. **



# GC 优化: 基础

# GC 优化: 工具

# GC 优化: 实践



# 参考
## user/sys/real时间
[link](https://blog.gceasy.io/2016/04/06/gc-logging-user-sys-real-which-time-to-use/)
- Real is wall clock time – time from start to finish of the call. This is all elapsed time including time slices used by other processes and time the process spends blocked (for example if it is waiting for I/O to complete).
- User is the amount of CPU time spent in user-mode code (outside the kernel) within the process. This is only actual CPU time used in executing the process. Other processes and time the process spends blocked do not count towards this figure.
- Sys is the amount of CPU time spent in the kernel within the process. This means executing CPU time spent in system calls within the kernel, as opposed to library code, which is still running in user-space. Like ‘user’, this is only CPU time used by the process.
- User+Sys will tell you how much actual CPU time your process used. Note that this is across all CPUs, so if the process has multiple threads, it could potentially exceed the wall clock time reported by Real.