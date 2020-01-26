package socs.network.networking;

import ch.qos.logback.core.net.server.Client;
import com.sun.istack.internal.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    private int portNum;
    private boolean isListening;

    public Server(int portNum)
    {
        this.portNum = portNum;
        isListening = false;
    }

    public void listen()
    {
        if(isListening)
        {
            return;
        }

        isListening = true;

        new Thread(new Runnable() {
            public void run() {
                try
                {
                    ServerSocket server = new ServerSocket(portNum);

                    while(true)
                    {
                        //Getting the client
                        Socket client = server.accept();

                        ClientHandler clientHandler = new ClientHandler(client);

                        //TODO might change this
                        new Thread(clientHandler).start();
                    }
                }
                catch (Exception e)
                {
                    System.err.println("FATAL_ERROR: Server was interrupted! \n" + e.getStackTrace());
                }
            }
        }).start();

    }
}


class ClientHandler implements Runnable
{
    private Socket client;
    private DataInputStream fromClient = null;
    private DataOutputStream toClient = null;

    public ClientHandler(@NotNull Socket client)
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

    public void run()
    {

    }
}
