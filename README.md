# goodutils
good utils methods, a repo contains some helpful utils methods.
Add a support for 
`gradle fatjar`

## PRoxy test
[proxytest in java](src/network/ProxyTest.java)<br>
[setup a proxy](src/network/ConfigAProxyByCCProxy.md)

# GC handbook/GC中文手册
非常详细的[GC 中文手册](gc_handbook_zh.md)

# 下载网易公开课视频的代码
[DownloadNetEaseVideo](src/netease/DownloadNetEaseVideo.java)
可以参考我的博客, 获取通过该代码下载的算法导论和机器学习的视频文件.
https://blog.csdn.net/scugxl/article/details/42093031

# different types of reverse proxy demo
[proxydemeo](proxydemeo)
run the jetty example, it can support http and https at same endpoint:
```
http://127.0.0.1:9999/?testme=true   -> to a https url
http://127.0.0.1:9999/ -> to a http url
```

# 通过JavaParser 实现的动态替换和修改源代码的示例代码
[javaparser.RemoveLogNotice](src/javaparser/RemoveLogNotice.java) <br>
这里展示了如何将LogNotice 类相关的调用 替换为LogMsg的实现.

# Btrace usage 文档
[btrace doc](btrace_usage.md)

# 梯度下降测试代码
[GredientDescentDemo](src/GredientDescentDemo.java)

# 获取JAVA cpu
[get cpu in java](src/cpu/CpuTest.java)

# JAVA中的BIO NIO BIO
[java 各种IO](src/io/README.MD)
可以参考博客: https://blog.csdn.net/scugxl/article/details/86742171

# 缓存
[使用FutureTask实现的高效缓存](src/multithread/UseFutureTaskImplementedCache.java)


# SSL diagnose
diagnose ssl relates protocols and ciphers:
build it with:<br>
```
gradle fatjar
``` 
[SSLTest](ssltest/src/main/java/SSLTest.java)
run it with:
```
java -cp ssltest-0.0.1.jar SSLTest -enabledprotocols TLS -sslprotocol TLSv1.2 -no-check-certificate -no-verify-hostname -unlimited-jce -sni -ciphers TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 baidu.com:443
```

# software installation related

[BeyondCompare](softs/beyondCompare_onMac/readme.md)