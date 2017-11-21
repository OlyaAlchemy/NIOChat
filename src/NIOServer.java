import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class NIOServer implements Runnable {
    private final int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(256);
    private String nicName = null;

    NIOServer(int port) throws IOException {
        this.port = port;
        this.ssc = ServerSocketChannel.open();
        this.ssc.socket().bind(new InetSocketAddress(port));
        this.ssc.configureBlocking(false);
        this.selector = Selector.open();
        this.ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Looking for a client");

            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.ssc.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) this.handleAccept(key);
                    try {
                        if (key.isReadable()) this.handleRead(key);
                    } catch (IOException ex) {
                        System.out.println(key.attachment() + " left the chanel");
                        key.channel().close();
                        broadcast(key.attachment() + " left the chanel");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        StringBuffer stringBuffer = new StringBuffer();

        buf.clear();
        int read = 0;
        while ((read = socketChannel.read(buf)) > 0) {
            buf.flip();         //flip buf from read to write
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);     //write buff into bytes
            stringBuffer.append(new String(bytes));
            buf.clear();
        }
        if (key.attachment() != null) {
            String msg;
            msg = key.attachment() + ": " + stringBuffer.toString();
            System.out.println(msg);
            broadcast(msg);
            stringBuffer = null;
        } else {
            if (isNickTaken(stringBuffer.toString()) == false) {
                key.attach(stringBuffer.toString());
                String hello = "Welcome to NIOChat, " + stringBuffer.toString() + "!\n";
                String someoneNewOnTheChannel = stringBuffer.toString() + " join the chanel";
                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(hello.getBytes()));
                broadcast(someoneNewOnTheChannel);
            } else {
                ((SocketChannel) key.channel()).write(ByteBuffer.wrap("This user name has been taken. Enter another user name".getBytes()));

            }
        }
    }

    private boolean isNickTaken(String nick){
        boolean is = false;
        for (SelectionKey key: selector.keys()){
            if(key.isValid() && key.channel() instanceof SocketChannel){
                if(key.attachment() != null && key.attachment().equals(nick)){
                    is = true;
                }
            }
        }
        return is;
    }

    private void broadcast(String msg) throws IOException {
        ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel sch = (SocketChannel) key.channel();
                sch.write(msgBuf);
                msgBuf.rewind();
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
        String address = (new StringBuilder(sc.socket().getInetAddress().toString())).append(":").append(sc.socket().getPort()).toString();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
        System.out.println("accepted connection from: " + address);
    }

    public static void main(String[] args) throws IOException {
        NIOServer nioServer = new NIOServer(1234);
        new Thread(nioServer).start();
    }
}