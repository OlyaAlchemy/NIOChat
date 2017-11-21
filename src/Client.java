import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Client implements Runnable {
    private int serverPort;
    private String IPaddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(256);

    Client(int serverPort, String IPaddress) throws IOException {
        this.serverPort = serverPort;
        this.IPaddress = IPaddress;
        this.socketChannel = SocketChannel.open();
        this.socketChannel.connect(new InetSocketAddress(IPaddress, serverPort));
        this.socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    @Override
    public void run() {
        System.out.print("Enter user name: ");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            socketChannel.write(ByteBuffer.wrap(bufferedReader.readLine().getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {         // read thread
            @Override
            public void run() {
                try {
                    while (socketChannel.isOpen()) {
                        BufferedReader messageToSend = new BufferedReader(new InputStreamReader(System.in));
                        socketChannel.write(ByteBuffer.wrap(messageToSend.readLine().getBytes()));
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {         //wright thread
            @Override
            public void run() {
                try {
                    while (socketChannel.isOpen()) {
                        buf.clear();
                        StringBuffer stringBuffer = new StringBuffer();
                        int read = 0;
                        while ((read = socketChannel.read(buf)) > 0) {
                            buf.flip();
                            byte[] bytes = new byte[buf.limit()];
                            buf.get(bytes);     //write buff into bytes
                            stringBuffer.append(new String(bytes));
                            buf.clear();
                            System.out.println(stringBuffer.toString());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        Client nioClientOUT = new Client(1234, "127.0.0.1");
        new Thread(nioClientOUT).start();
    }
}
