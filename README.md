# StreamMQ
通用的文件传输消息组件，主要面向文件传输场景，异步消息，支持断点续传，弱网环境也能保障连网续传，确保数据最终送达

## 功能描述
1. 基于Netty在TCP层面实现的P2P数据传输，支持有中心和无中心部署。无中心部署，结点之间只能互相发布文件/文本消息；有中心部署，中心结点收集每个结点上报的日志、服务器监控指标等信息，维护所有结点的状态。
2. 支持文件和普通文本消息，异步传输、断点续传、流量限速。高可靠性的数据送达保障，服务重启、断网重连等极端场景，也能确保数据最终送达。借鉴Kafka的消息订阅体验，消息体缓存到本地文件系统，支持按消息序号反复读取近期的数据。
3. 推荐采用Jar包方式集成到业务应用，也可Jar包单独部署做为简单的文件收发服务。

## 环境依赖
1. 依赖JDK1.8+
2. 如果启动监控服务，由于依赖第三方包 Sigar，需要针对不同的操作系统，将Sigar的动态连接库部署到服务器的java lib扩展目录下，动态连接库在sigar-lib.tar.gz内

- linux   64位系统将  libsigar-amd64-linux.so 放到 jre/lib/amd64 或者 /user/lib 目录下
- macos   64位系统将  libsigar-universal64-macosx.dylib 放到 /Library/Java/Extensions 目录下
- windows 64位系统将  sigar-amd64-winnt.dll 放到 jre/bin目录下

## 测试 & 使用
1. 启动消息接收服务  
构造Server实例并启动服务，轮询消费文件/文本消息  
2. 构造客户端实例  
构造Client对象实例，可以发布文件/文本到任意启动了Server服务的结点  

参考 ClientTest.java, ServerTest.java