package socs.network.networking;

import socs.network.events.Event;
import socs.network.events.EventHandler;
import socs.network.message.SOSPFPacket;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Server
{
    private int portNum;
    private ArrayList<ConnectionHandler> pendingConnections = new ArrayList<>();
    private ConnectionHandler[] links = new ConnectionHandler[4];
    public Event<Integer, SOSPFPacket> msgReceivedEvent = new Event<>();
    public Event<Integer, String> connectionAcceptedEvent = new Event<>();

    public Server(int portNum)
    {
        this.portNum = portNum;
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

                        connectionHandler.msgReceived.addHandler((EventHandler<Integer, SOSPFPacket>) (index1, msg) -> msgReceivedEvent.invoke(index1, msg));
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

    public boolean tryAttach(String processIP, short processPort)
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
                return false;
            }

            ConnectionHandler connectionHandler = new ConnectionHandler(socket, index);
            links[index] = connectionHandler;
            new Thread(connectionHandler).start();
        }
        catch (IOException e)
        {
            System.err.println("ERROR! Failed to attach to " + processIP + " " + processPort + "\n" + e.getMessage() + " " + e.getClass());
            return false;
        }

        return true;
    }

    public void send(SOSPFPacket msg, int index)
    {
        if(links[index] != null){
            System.out.println("HERE");
            links[index].send(msg);
        }
    }
}


class ConnectionHandler implements Runnable
{
    private Socket client;
    private int index;
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
                msgReceived.invoke(index, msg);
            }
        }
        catch (Exception e)
        {
            System.err.println("ERROR!: Client interrupted\n" + e.getMessage());
        }
    }

    public int getIndex()
    {
        return index;
    }
}
