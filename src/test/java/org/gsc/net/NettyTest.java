package org.gsc.net;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.Test;
import org.springframework.util.DigestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class NettyTest {

    private class NettyServer{
        private final Charset UTF_8 = Charset.forName("utf-8");

        private ChannelFuture future;

        private boolean isClosed = false;

        private boolean init = false;

        private ServerBootstrap bootstrap;

        public void start() {

            if(init) {
                throw new RuntimeException("Client is already started!");
            }
            //thread model：one selector thread，and one worker thread pool。
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);//more than 1 is not needed!
            EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() - 1);
            try {
                bootstrap = new ServerBootstrap();//create ServerSocket transport。
                bootstrap.group(bossGroup,workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(10240, 0, 2, 0, 2))
                                        .addLast(new StringDecoder(UTF_8))
                                        .addLast(new LengthFieldPrepender(2))
                                        .addLast(new StringEncoder(UTF_8))
                                        .addLast(new ServerHandler());
                            }
                        }).childOption(ChannelOption.TCP_NODELAY, true);
                future = bootstrap.bind(18080).sync();
                init = true;
                //
                System.out.println("server started");
            } catch (Exception e) {
                isClosed = true;
            } finally {
                if(isClosed) {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                    System.out.println("server closed");
                }
            }
        }


        public void close() {
            if(isClosed) {
                return;
            }
            try {
                future.channel().close();
            } finally {
                bootstrap.childGroup().shutdownGracefully();
                bootstrap.group().shutdownGracefully();
            }
            isClosed = true;
            System.out.println("server closed");
        }
    }

    private class ServerHandler extends SimpleChannelInboundHandler<String> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
            System.out.println("from client:" + message);
            JSONObject json = JSONObject.parseObject(message);
            String source = json.getString("source");

            String md5 = DigestUtils.md5DigestAsHex(source.getBytes());
            //解析成JSON
            json.put("md5Hex",md5);
            ctx.writeAndFlush(json.toString());//write bytes to socket,and flush(clear) the buffer cache.
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<String>{
        private Map<Integer,String> response = new ConcurrentHashMap<Integer, String>();

        //key is sequence ID，value is request thread.
        private final Map<Integer,Thread> waiters = new ConcurrentHashMap<Integer, Thread>();

        private final AtomicInteger sequence = new AtomicInteger();


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //当channel就绪后。
            System.out.println("client channel is ready!");
            //ctx.writeAndFlush("started");//阻塞知道发送完毕
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
            System.out.println("client handler msg:"+message);
            JSONObject json = JSONObject.parseObject(message);
            Integer id = json.getInteger("id");
            response.put(id,json.getString("md5Hex"));

            Thread thread = waiters.remove(id);//读取到response后，从waiters中移除并唤醒线程。
            synchronized (thread) {
                thread.notifyAll();
            }
        }


        public String call(String message,Channel channel) throws Exception {
            int id = sequence.incrementAndGet();//产生一个ID，并与当前request绑定
            Thread current = Thread.currentThread();
            waiters.put(id,current);
            JSONObject json = new JSONObject();
            json.put("id",id);
            json.put("source",message);
            channel.writeAndFlush(json.toString());
            while (!response.containsKey(id)) {
                synchronized (current) {
                    current.wait();//阻塞请求调用者线程，直到收到响应响应
                }
            }
            waiters.remove(id);
            return response.remove(id);

        }
    }
    private class Client{
        private final Charset UTF_8 = Charset.forName("utf-8");

        private ClientHandler clientHandler = new ClientHandler();

        private Bootstrap bootstrap;

        private ChannelFuture future;

        private boolean init = false;

        private boolean isClosed = false;

        public void start() {
            if(init) {
                throw new RuntimeException("client is already started");
            }
            //thread model: one worker thread pool,contains selector thread and workers‘.
            EventLoopGroup workerGroup = new NioEventLoopGroup(2);//1 is OK
            try {
                bootstrap = new Bootstrap();
                bootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class) //create SocketChannel transport
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,10000)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(10240, 0, 2, 0, 2))
                                        .addLast(new StringDecoder(UTF_8))
                                        .addLast(new LengthFieldPrepender(2))
                                        .addLast(new StringEncoder(UTF_8))
                                        .addLast(clientHandler);//the same as ServerBootstrap
                            }
                        });
                //keep the connection with server，and blocking until closed!
                future = bootstrap.connect(new InetSocketAddress("127.0.0.1", 18080)).sync();
                init = true;
            } catch (Exception e) {
                isClosed = true;
            } finally {
                if(isClosed) {
                    workerGroup.shutdownGracefully();
                }
            }
        }

        public void close() {
            if(isClosed) {
                return;
            }
            try {
                future.channel().close();
            } finally {
                bootstrap.group().shutdownGracefully();
            }
            isClosed = true;
        }

        /**
         * 发送消息
         * @param message
         * @return
         * @throws Exception
         */
        public String send(String message) throws Exception {
            if(isClosed || !init) {
                throw new RuntimeException("client has been closed!");
            }
            //send a request call,and blocking until recevie a response from server.
            return clientHandler.call(message,future.channel());
        }
    }

    @Test
    public void testNettySample() throws InterruptedException {
        NettyServer nettyServer = new NettyServer();
        nettyServer.start();//启动server
        Thread.sleep(3000);

        Client nettyClient = new Client();
        nettyClient.start();

        try {
            for (int i = 0; i < 5; i++) {
                String response = nettyClient.send("this is a message,"+i);
                System.out.println("response:" + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            nettyClient.close();
        }

        nettyServer.close();

    }

}

