package socs.network.networking;

import ch.qos.logback.classic.net.SimpleSocketServer;
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
    private int portNum;
    private ClientHandler[] clients;
    public Event<Integer, String> msgReceivedEvent;

    public Server(int portNum)
    {
        this.portNum = portNum;
        clients = new ClientHandler[4];
        msgReceivedEvent = new Event();
    }

    private void listen()
    {
        try
        {
            new Thread(new Runnable() {
                public void run() {

                }
            });
        }
        catch (Exception e)
        {

        }
    }

    //TODO add client
    public void attach(String processIP, short processPort)
    {

    }

    public void send(String msg)
    {
        for(ClientHandler client : clients)
        {
            if(client != null)
            {
                client.send(msg);
            }
        }
    }
}


class ClientHandler implements Runnable
{
    private Socket client;
    private int index;
    private DataInputStream fromClient = null;
    private DataOutputStream toClient = null;
    public Event<Integer, String> msgReceived;

    ClientHandler(@NotNull Socket client)
    {
        this.client = client;

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
