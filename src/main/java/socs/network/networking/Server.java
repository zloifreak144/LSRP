package socs.network.networking;

import ch.qos.logback.core.net.server.Client;
import com.sun.istack.internal.NotNull;
import socs.network.events.Event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    private ClientHandler[] clients;
    public Event msgReceivedEvent;

    public Server()
    {
        clients = new ClientHandler[4];
        msgReceivedEvent = new Event();
    }

    //TODO add client
    public void attach(int portNum)
    {

    }

    //TODO send message
    public void msgSend(String msg)
    {

    }

}


class ClientHandler implements Runnable
{
    private Socket client;
    private int portNum;
    private DataInputStream fromClient = null;
    private DataOutputStream toClient = null;

    public ClientHandler(@NotNull Socket client, int portNum)
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

    public void run()
    {

    }
}
