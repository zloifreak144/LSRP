package socs.network.events;

import java.util.ArrayList;

public class Event {
    private ArrayList<EventHandler> handlers;

    public void invoke()
    {
        for(EventHandler handler : handlers)
        {
            handler.handle();
        }
    }

    public void addHandler(EventHandler handler)
    {
        handlers.add(handler);
    }
}
