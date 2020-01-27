package socs.network.networking;

import com.sun.istack.internal.NotNull;
import socs.network.events.Event;
import socs.network.events.EventHandler;
import socs.network.message.SOSPFPacket;

import java.io.*;
import java.net.*;

public class Server
{
    private int portNum;
    private ClientHandler[] clients;
    public Event<Integer, SOSPFPacket> msgReceivedEvent;
    public Event<Integer, String> connectionAcceptedEvent;

    public Server(int portNum)
    {
        this.portNum = portNum;
        clients = new ClientHandler[4];
        msgReceivedEvent = new Event();
        listen();
    }

    private void listen()
    {
            new Thread(new Runnable() {
                public void run() {
                    try
                    {
                        while (true)
                        {
                            ServerSocket server = new ServerSocket(portNum);
                            Socket client = server.accept();
                            connectionAcceptedEvent.invoke(client.getPort(), client.getInetAddress().getHostName());
                            int index = getAvailableIndex();

                            if(index == -1)
                            {
                                System.err.println("ERROR! Connections Capacity Reached!");
                                break;
                            }

                            ClientHandler clientHandler = new ClientHandler(client, index);

                            clientHandler.msgReceived.addHandler(new EventHandler<Integer, SOSPFPacket>() {
                                public void handle(Integer index, SOSPFPacket msg) {
                                    msgReceivedEvent.invoke(index, msg);
                                }
                            });

                            new Thread(clientHandler).start();
                        }
                    }
                    catch(Exception e)
                    {
                        System.err.println("FATAL_ERROR! Server thread interrupted\n" + e.getStackTrace());
                    }
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

    public void attach(String processIP, short processPort)
    {
        try
        {
            Socket socket = new Socket();
            InetAddress inetAddress = InetAddress.getByName(processIP);
            SocketAddress bindingPoint = new InetSocketAddress(inetAddress, processPort);
            socket.bind(bindingPoint);
        }
        catch (Exception e)
        {
            System.err.println("ERROR! Failed to attache to " + processIP + " " + processPort + "\n" + e.getStackTrace());
        }

    }

    public void send(SOSPFPacket msg, int index)
    {
        if(clients[index] != null){
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
    public Event<Integer, SOSPFPacket> msgReceived;

    ClientHandler(@NotNull Socket client, int index)
    {
        this.client = client;
        this.index = index;

        try
        {
             DataInputStream fromClient = new DataInputStream(client.getInputStream());
             DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
        }
        catch (Exception e)
        {
            System.err.println("ERROR! Failed to initialize Client Handler streams\n " + e.getStackTrace());
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
            System.err.println("ERROR! Failed to send the message\n" + e.getStackTrace());
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
            System.err.println("ERROR!: Client interrupted\n" + e.getStackTrace());
        }
    }

    public int getIndex()
    {
        return index;
    }
}
