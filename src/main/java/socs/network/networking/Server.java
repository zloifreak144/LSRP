package socs.network.networking;

import socs.network.events.Event;
import socs.network.events.EventHandler;
import socs.network.message.SOSPFPacket;
import socs.network.node.Link;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server
{
    private int portNum;
    private String ip;
    private ArrayList<ConnectionHandler> pendingConnections = new ArrayList<>();
    private ConnectionHandler[] links = new ConnectionHandler[4];
    public Event<Integer, SOSPFPacket> msgReceivedEvent = new Event<>();
    public Event<Integer, String> connectionAcceptedEvent = new Event<>();
    public Event<Integer, LinkDescription> linkCreatedEvent = new Event<>();
    public Event<Integer, LinkDescription> linkRemovedEvent = new Event<>();

    public Server(int portNum, String ip)
    {
        this.portNum = portNum;
        this.ip = ip;
        listen();
    }

    private void listen()
    {
            new Thread(() ->
            {
                try
                {
                    ServerSocket server = new ServerSocket(portNum);
                    while (true)
                    {
                        Socket client = server.accept();
                        connectionAcceptedEvent.invoke(client.getLocalPort(), client.getInetAddress().getHostName());

                        ConnectionHandler connectionHandler = new ConnectionHandler(client);

                        connectionHandler.msgReceived.addHandler((EventHandler<Integer, SOSPFPacket>) (index1, msg) ->
                        {
                            //NOTE: Server.links and Router.ports should be syncronized

                            //Hello received from a pending connection
                            if(msg.sospfType == 0 && connectionHandler.getIndex() == -1)
                            {
                               handleHello(connectionHandler, msg);
                            }

                            if(msg.sospfType == 1)
                            {
                                //TODO implement
                                handleLSAUPDATE(connectionHandler, msg);
                            }

                            //TODO again verify that it is a correct way to do it
                            //If connection was refused remove the link created on attach
                            if(msg.sospfType == 2)
                            {
                                handleConnectionRefused(connectionHandler, msg);
                            }


                            msgReceivedEvent.invoke(index1, msg);
                        });
                        pendingConnections.add(connectionHandler);
                        new Thread(connectionHandler).start();
                    }
                }
                catch(IOException e)
                {
                    System.err.println("FATAL_ERROR! Server thread interrupted\n" + e.getMessage());
                }
        }).start();
    }

    /**
     *
     * @return the index of the available entry in clients array, -1 on failure
     */
    private int getAvailableIndex()
    {
        for(int i = 0; i < links.length; i++)
        {
            if(links[i] == null)
            {
                return i;
            }
        }

        return -1;
    }

    private void handleHello(ConnectionHandler connectionHandler, SOSPFPacket msg)
    {
        int index = getAvailableIndex();

        //TODO send connection_refuse does not work
        //Links full
        if(index == -1)
        {
            System.out.println("Server capacity reached! Connection attempt refused to " + connectionHandler.getIncomingIP());
            //TODO check if we actually need to remove them
            pendingConnections.remove(connectionHandler);

            SOSPFPacket response = new SOSPFPacket();
            response.sospfType = 2; //Connection refused

            //TODO remove the hardcoded version of the srcProcessIP
            response.srcProcessIP = "localHost";
            response.dstIP = connectionHandler.getIncomingIP();
            response.srcIP = ip;
            response.srcProcessPort = (short) portNum;

            connectionHandler.send(response);
            //TODO make sure removes are done properly
            connectionHandler.close();
            return;
        }

        //remove from pending add to link
        pendingConnections.remove(connectionHandler);
        links[index] = connectionHandler;

        LinkDescription linkDescription = new LinkDescription();
        linkDescription.srcIP = msg.srcIP;
        linkDescription.srcProcessPort = msg.srcProcessPort;
        linkDescription.srcProcessIP = msg.srcProcessIP;

        linkCreatedEvent.invoke(index, linkDescription);
    }

    //TODO implement
    private void handleLSAUPDATE(ConnectionHandler connectionHandler, SOSPFPacket msg)
    {

    }


    private void handleConnectionRefused(ConnectionHandler connectionHandler, SOSPFPacket msg)
    {
        int linkIndex = connectionHandler.getIndex();

        System.out.println("Handling remove");

        if(linkIndex != -1)
        {
            System.out.println("Removing link");
            links[linkIndex].close();
            links[linkIndex] = null;

            LinkDescription linkDescription = new LinkDescription();
            linkDescription.srcIP = msg.srcIP;
            linkDescription.srcProcessPort = msg.srcProcessPort;
            linkDescription.srcProcessIP = msg.srcProcessIP;

            linkRemovedEvent.invoke(linkIndex, linkDescription);
        }
    }

    public void attach(String processIP, String simulatedIP ,short processPort)
    {
        Socket socket = null;

        try
        {
            socket = new Socket(processIP, processPort);
            int index = getAvailableIndex();
            if(index == -1)
            {
                System.err.println("ERROR! Port capacity reached!");
                socket.close();
                return;
            }

            ConnectionHandler connectionHandler = new ConnectionHandler(socket, index);
            links[index] = connectionHandler;

            LinkDescription link = new LinkDescription();
            link.srcIP = simulatedIP;
            link.srcProcessPort = processPort;
            link.srcProcessIP = processIP;

            linkCreatedEvent.invoke(index, link);
            new Thread(connectionHandler).start();
        }
        catch (IOException e)
        {
            System.err.println("ERROR! Failed to attach to " + processIP + " " + processPort + "\n" + e.getMessage() + " " + e.getClass());
        }
    }

    public void send(SOSPFPacket msg, int index)
    {
        if(links[index] != null)
        {
            links[index].send(msg);
        }
    }

    public void sync(Link[] ports)
    {
        //TODO might need to implement this
    }

    public class LinkDescription
    {
        public String srcProcessIP;
        public short srcProcessPort;
        public String srcIP;
    }
}


class ConnectionHandler implements Runnable
{
    private Socket client;
    private int index;
    private String incomingIP; //this is initialized only once the message is received
    private ObjectInputStream fromClient = null;
    private ObjectOutputStream toClient = null;
    private boolean run = true;
    public Event<Integer, SOSPFPacket> msgReceived = new Event<Integer, SOSPFPacket>();

    ConnectionHandler(Socket client, int index)
    {
        this.client = client;
        this.index = index;

        try
        {
            //TODO remove the comment
            /*
            You need to create the ObjectOutputStream before the ObjectInputStream at both sides of the connection(!). When the ObjectInputStream is created, it tries to read the object stream header from the InputStream. So if the ObjectOutputStream on the other side hasn't been created yet there is no object stream header to read, and it will block indefinitely.

Or phrased differently: If both sides first construct the ObjectInputStream, both will block trying to read the object stream header, which won't be written until the ObjectOutputStream has been created (on the other side of the line); which will never happen because both sides are blocked in the constructor of ObjectInputStream.
             */

            toClient = new ObjectOutputStream(client.getOutputStream());
            fromClient = new ObjectInputStream(client.getInputStream());

        }
        catch (Exception e)
        {
            System.err.println("ERROR! Failed to initialize Client Handler streams\n " + e.getMessage());
        }
    }

    ConnectionHandler(Socket client)
    {
        //-1 means that the connection is not a link
        this(client, -1);
    }

    private SOSPFPacket recv() throws Exception
    {
       return (SOSPFPacket) fromClient.readObject();
    }

    public void send(SOSPFPacket msg)
    {
        try
        {
            toClient.writeObject(msg);
        }
        catch(Exception e)
        {
            System.err.println("ERROR! Failed to send the message\n" + e.getMessage());
        }
    }

    public void run()
    {
        try
        {
            while(run)
            {
                SOSPFPacket msg = recv();
                incomingIP = msg.srcIP;
                msgReceived.invoke(index, msg);
            }
        }
        catch(SocketException e)
        {
            //TODO change the exception message
            System.out.println("Client disconnected");
            close();
        }
        catch (Exception e)
        {
            System.err.println("ERROR!: Client interrupted\n" + e.getMessage() + " " + e.getClass());
        }
    }

    public void close()
    {
        run = false;

        try
        {
            fromClient.close();
            toClient.close();
            client.close();
        }
        catch (IOException e)
        {
            //Do nothing
        }
    }


    public int getIndex()
    {
        return index;
    }

    public String getIncomingIP()
    {
        return  incomingIP;
    }

}
