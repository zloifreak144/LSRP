package socs.network.node;

import socs.network.events.Event;
import socs.network.events.EventHandler;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.networking.Server;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();
  Server server;
  int numConnections;

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.port");
    lsd = new LinkStateDatabase(rd);
    server = new Server(rd.processPortNumber, rd.simulatedIPAddress);

    server.linkCreatedEvent.addHandler((EventHandler<Integer, Server.LinkDescription>) (portIndex, link) ->
    {
      //figure out the weight
      addLink(link.srcProcessIP, link.srcProcessPort, link.srcIP, portIndex, (short) 0);
    });

    server.linkRemovedEvent.addHandler((EventHandler<Integer, Server.LinkDescription>) (portIndex, link)->
    {
      ports[portIndex] = null;
    });


    server.msgReceivedEvent.addHandler((EventHandler<Integer, SOSPFPacket>) (portNum, packet) ->
    {

      if (packet.sospfType == 0)
      {
        System.out.println("received HELLO from " + packet.srcIP);
      }

      if(packet.sospfType == 1)
      {
        System.out.println("received LSAUPDATE from " + packet.srcIP);
      }

      if(packet.sospfType == 2)
      {
        System.out.println("received CONNECTION_REFUSE from " + packet.srcIP);
      }
    });

    server.connectionAcceptedEvent.addHandler((EventHandler<Integer, String>) (portNum, host) ->
    {
        //TODO remove
      numConnections++;
      System.out.println("Incoming connection: " + portNum + " " + host);
    });


  }

  //TODO check if we need to use it
  private boolean LinkExists(String simIP)
  {
    for(Link port : ports)
    {
      if(port.router2.simulatedIPAddress.equals(simIP))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight)
  {
      server.attach(processIP, simulatedIP , processPort);
  }

  private void addLink(String processIP, short processPort, String simulatedIP, int index ,short weight)
  {
    RouterDescription rdOther = new RouterDescription();
    rdOther.processPortNumber = processPort;
    rdOther.simulatedIPAddress = simulatedIP;
    rdOther.processIPAddress = processIP;

    ports[index] =  new Link(this.rd, rdOther, weight);
  }


  /**
   * broadcast Hello to neighbors
   */
  private void processStart()
  {
    for (int i = 0; i < ports.length; i++)
    {
      if(ports[i] != null)
      {
        SOSPFPacket msg = new SOSPFPacket();
        msg.srcProcessIP = rd.processIPAddress;
        msg.srcProcessPort = rd.processPortNumber;
        msg.srcIP = rd.simulatedIPAddress;
        msg.dstIP = ports[i].router2.simulatedIPAddress;
        msg.sospfType = 0;
        //msg.neighborID = rd.simulatedIPAddress;
        server.send(msg,i);

        rd.status = RouterStatus.INIT;
      }

    }
  }

  private void send(String processIP, short processPort)
  {
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {

  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  /**
   *
   * @return Returns the index of a null entry, -1 on failure
   */
  private int getAvailableIndex()
  {
    for(int i = 0; i < ports.length; i++)
    {
      if(ports[i] == null)
      {
        return i;
      }
    }

    return -1;
  }


  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
