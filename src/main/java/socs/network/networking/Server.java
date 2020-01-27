package socs.network.networking;

import socs.network.events.Event;
import socs.network.events.EventHandler;
import socs.network.message.SOSPFPacket;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server
{
    private int portNum;
    private ClientHandler[] clients = new ClientHandler[4];
    private ClientHandler[] outcomingConnections = new ClientHandler[4];
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
                        int index = getAvailableIndex();

                        if(index == -1)
                        {
                            System.err.println("ERROR! Connections Capacity Reached!");
                            break;
                        }

                        ClientHandler clientHandler = new ClientHandler(client, index);

                        clientHandler.msgReceived.addHandler((EventHandler<Integer, SOSPFPacket>) (index1, msg) -> msgReceivedEvent.invoke(index1, msg));

                        new Thread(clientHandler).start();
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
        for(int i = 0; i < clients.length; i++)
        {
            if(clients[i] == null)
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

            //TODO need to create the clientHandlers and store them somewhere to make proper communication

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
        if(clients[index] != null){
            System.out.println("HERE");
            clients[index].send(msg);
        }
    }
}


class ClientHandler implements Runnable
{
    private Socket client;
    private int index;
    private ObjectInputStream fromClient = null;
    private ObjectOutputStream toClient = null;
    public Event<Integer, SOSPFPacket> msgReceived = new Event<Integer, SOSPFPacket>();

    ClientHandler(Socket client, int index)
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
