package edu.berkeley.sparrow.prototype;

import java.net.InetSocketAddress;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import edu.berkeley.sparrow.daemon.util.ThriftClientPool;
import edu.berkeley.sparrow.thrift.PongService;
import edu.berkeley.sparrow.thrift.PongService.AsyncClient;
import edu.berkeley.sparrow.thrift.PongService.AsyncClient.ping_call;

public class ThriftPongClient {
  private static ThriftClientPool<PongService.AsyncClient> pongClientPool =
      new ThriftClientPool<PongService.AsyncClient>(
          new ThriftClientPool.PongServiceMakerFactory());
  private static boolean USE_SYNCHRONOUS_CLIENT = true;

  private static class Callback implements AsyncMethodCallback<ping_call> {
    private InetSocketAddress address;
    private Long t0;

    Callback(InetSocketAddress address, Long t0) { this.address = address; this.t0 = t0; }

    @Override
    public void onComplete(ping_call response) {
      try {
        pongClientPool.returnClient(address, (AsyncClient) response.getClient());
        System.out.println("Took: " + (System.nanoTime() - t0) / (1000.0 * 1000.0) + "ms");
      } catch (Exception e) {
        System.out.println("ERROR!!!");
      }
    }

    @Override
    public void onError(Exception exception) {
      System.out.println("ERROR!!!");
    }

  }

  private static void pongUsingSynchronousClient(String hostname)
      throws TException, InterruptedException {
    TTransport tr = new TFramedTransport(new TSocket(hostname, 12345));
    TProtocol proto = new TBinaryProtocol(tr);
    PongService.Client client = new PongService.Client(proto);
    while (true) {
      Long t = System.nanoTime();
      client.ping("PING");
      System.out.println("Took: " + (System.nanoTime() - t) / (1000.0 * 1000.0) + "ms");
      Thread.sleep(500);
    }
  }

  private static void pongUsingAsynchronousClient(String hostname) throws Exception {
    InetSocketAddress address = new InetSocketAddress(hostname, 12345);
    while (true) {
      Long t = System.nanoTime();
      AsyncClient client = pongClientPool.borrowClient(address);
      System.out.println("Getting client took " + ((System.nanoTime() - t) / (1000 * 1000)) +
                         "ms");
      client.ping("PING", new Callback(address, System.nanoTime()));
      Thread.sleep(30);
    }
  }

  public static void main(String[] args) throws Exception {
    String hostname = args[0];

    if (USE_SYNCHRONOUS_CLIENT) {
      pongUsingSynchronousClient(hostname);
    } else {
      pongUsingAsynchronousClient(hostname);
    }
  }
}
