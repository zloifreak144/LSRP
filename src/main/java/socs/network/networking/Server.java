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
    private ServerSocket server;

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
                    server = new ServerSocket(portNum);
                    while (true)
                    {
                        Socket client = server.accept();
                        connectionAcceptedEvent.invoke(client.getLocalPort(), client.getInetAddress().getHostName());

                        ConnectionHandler connectionHandler = new ConnectionHandler(client);

                        init_connectionHandler(connectionHandler);

                        pendingConnections.add(connectionHandler);
                        new Thread(connectionHandler).start();
                    }
                }
                catch(IOException e)
                {
                    System.err.println("FATAL_ERROR! Server thread interrupted\n" + e.getMessage());
                    System.exit(-1);
                }
        }).start();
    }

    /**
     * Close all connections and the server
     */
    public void close()
    {
        for(int i=0;i<links.length;i++)
        {
            if(links[i] != null)
            {
                links[i].close();
            }
        }

        for(ConnectionHandler connection: pendingConnections)
        {
            if(connection != null)
            {
                connection.close();
            }
        }

        try {
            server.close();
        } catch (Exception e){
            //do nothing
        }
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

    private void init_connectionHandler(ConnectionHandler connectionHandler)
    {
        connectionHandler.msgReceived.addHandler((EventHandler<Integer, SOSPFPacket>) (index1, msg) ->
        {
            //NOTE: Server.links and Router.ports should be syncronized
            //Hello received from a pending connection
            if(msg.sospfType == 0 && connectionHandler.index == -1)
            {
                handleHello(connectionHandler, msg);
            }
            else if(msg.sospfType == 1)
            {
                //TODO implement
                handleLSAUPDATE(connectionHandler, msg);
            }

            //TODO again verify that it is a correct way to do it
            //If connection was refused remove the link created on attach
            else if(msg.sospfType == 2)
            {
                handleConnectionRefused(connectionHandler, msg);
            }
            else
            {
                msgReceivedEvent.invoke(index1, msg);
            }

        });
    }

    private void handleHello(ConnectionHandler connectionHandler, SOSPFPacket msg)
    {
        int index = getAvailableIndex();

        //Links full
        if(index == -1)
        {
            System.out.println("Server capacity reached! Connection attempt refused to " + connectionHandler.getIncomingIP());

            pendingConnections.remove(connectionHandler);

            SOSPFPacket response = new SOSPFPacket();
            response.sospfType = 2; //Connection refused

            response.srcProcessIP = "localHost";
            response.dstIP = connectionHandler.getIncomingIP();
            response.srcIP = ip;
            response.srcProcessPort = (short) portNum;

            connectionHandler.send(response);

            connectionHandler.close();
            return;
        }

        //remove from pending add to link
        pendingConnections.remove(connectionHandler);
        connectionHandler.index = index;
        links[index] = connectionHandler;

        LinkDescription linkDescription = new LinkDescription();
        linkDescription.srcIP = msg.srcIP;
        linkDescription.srcProcessPort = msg.srcProcessPort;
        linkDescription.srcProcessIP = msg.srcProcessIP;

        linkCreatedEvent.invoke(index, linkDescription);
        msgReceivedEvent.invoke(index, msg);
    }

    //TODO implement
    private void handleLSAUPDATE(ConnectionHandler connectionHandler, SOSPFPacket msg)
    {

    }


    private void handleConnectionRefused(ConnectionHandler connectionHandler, SOSPFPacket msg)
    {
        int linkIndex = connectionHandler.index;

        if(linkIndex != -1)
        {
            links[linkIndex].close();
            links[linkIndex] = null;

            LinkDescription linkDescription = new LinkDescription();
            linkDescription.srcIP = msg.srcIP;
            linkDescription.srcProcessPort = msg.srcProcessPort;
            linkDescription.srcProcessIP = msg.srcProcessIP;

            linkRemovedEvent.invoke(linkIndex, linkDescription);
        }

        msgReceivedEvent.invoke(linkIndex, msg);
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
            init_connectionHandler(connectionHandler);
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
    public int index;
    private String incomingIP; //this is initialized only once the message is received
    private ObjectInputStream fromClient = null;
    private ObjectOutputStream toClient = null;
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
            while(true)
            {
                SOSPFPacket msg = recv();
                incomingIP = msg.srcIP;
                msgReceived.invoke(index, msg);
            }
        }
        catch(SocketException e)
        {
            System.out.println("Client " + incomingIP + " disconnected: " + e.getMessage());
            close();
        }
        catch (Exception e)
        {
            System.err.println("ERROR!: Client interrupted\n" + e.getMessage() + " " + e.getClass());
        }
    }

    public void close()
    {
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

    public String getIncomingIP()
    {
        return  incomingIP;
    }
}
