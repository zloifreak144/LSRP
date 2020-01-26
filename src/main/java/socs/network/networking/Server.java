package socs.network.networking;

import ch.qos.logback.core.net.server.Client;
import com.sun.istack.internal.NotNull;
import socs.network.events.Event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    private ClientHandler[] clients;
    public Event<Integer, String> msgReceivedEvent;

    public Server()
    {
        clients = new ClientHandler[4];
        msgReceivedEvent = new Event();
    }

    //TODO add client
    public void attach(int portNum)
    {
        
    }

    public void send(String msg, int portNum)
    {
        for(ClientHandler client : clients)
        {
            if(client != null && client.getPortNum() == portNum)
            {
                client.send(msg);
            }
        }
    }
}


class ClientHandler implements Runnable
{
    private Socket client;
    private int portNum;
    private DataInputStream fromClient = null;
    private DataOutputStream toClient = null;
    public Event<Integer, String> msgReceived;

    ClientHandler(@NotNull Socket client, int portNum)
    {
        this.client = client;
        this.portNum = portNum;

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

    private String recv() throws IOException
    {
       return fromClient.readUTF();
    }

    public void send(String msg)
    {
        try
        {
            toClient.writeUTF(msg);
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
                String msg = recv();
                msgReceived.invoke(portNum, msg);
            }
        }
        catch (Exception e)
        {
            System.err.println("ERROR!: Client interrupted\n" + e.getStackTrace());
        }
    }

    public int getPortNum()
    {
        return portNum;
    }
}
