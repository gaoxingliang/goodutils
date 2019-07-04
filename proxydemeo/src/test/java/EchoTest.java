//import org.eclipse.jetty.util.BlockingArrayQueue;
//import org.eclipse.jetty.util.log.Log;
//import org.eclipse.jetty.util.log.Logger;
//
//import javax.websocket.CloseReason;
//import javax.websocket.ContainerProvider;
//import javax.websocket.Endpoint;
//import javax.websocket.EndpointConfig;
//import javax.websocket.MessageHandler;
//import javax.websocket.Session;
//import javax.websocket.WebSocketContainer;
//import java.io.IOException;
//import java.net.URI;
//import java.util.concurrent.TimeUnit;
//
//public class EchoTest {
//    public static void main(String[] args) throws Exception {
//        URI serverUri = new URI(String.format("ws://%s:%d/", "127.0.0.1", 9999));
//        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//        EndpointEchoClient echoer = new EndpointEchoClient();
//        // Issue connect using instance of class that extends Endpoint
//        Session session = container.connectToServer(echoer, serverUri);
//        session.getBasicRemote().sendText("Echo");
//        String echoed = echoer.textCapture.messages.poll(1, TimeUnit.SECONDS);
//        System.out.println("!!!!!!!!!MSG:" + echoed);
//    }
//
//    public static class EndpointEchoClient extends Endpoint
//    {
//        private static final Logger LOG = Log.getLogger(EndpointEchoClient.class);
//        private Session session = null;
//        private CloseReason close = null;
//        public EchoCaptureHandler textCapture = new EchoCaptureHandler();
//
//        public CloseReason getClose()
//        {
//            return close;
//        }
//
//        @Override
//        public void onOpen(Session session, EndpointConfig config)
//        {
//            if (LOG.isDebugEnabled())
//                LOG.debug("onOpen({}, {})", session, config);
//            this.session = session;
//            this.session.getUserProperties().put("endpoint", this);
//            this.session.addMessageHandler(textCapture);
//        }
//
//        public void sendText(String text) throws IOException
//        {
//            if (session != null)
//            {
//                session.getBasicRemote().sendText("YES, IT WEBSOCKCET:" + text);
//            }
//        }
//    }
//
//    public static class EchoCaptureHandler implements MessageHandler.Whole<String>
//    {
//        public BlockingArrayQueue<String> messages = new BlockingArrayQueue<>();
//
//        @Override
//        public void onMessage(String message)
//        {
//            messages.offer(message);
//        }
//    }
//}
