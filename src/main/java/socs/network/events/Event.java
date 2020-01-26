package socs.network.events;

import java.util.ArrayList;

public class Event<T, R> {
    private ArrayList<EventHandler<T, R>> handlers;

    public void invoke(T eventArg1, R eventArg2)
    {
        for(EventHandler handler : handlers)
        {
            handler.handle(eventArg1, eventArg2);
        }
    }

    public void addHandler(EventHandler handler)
    {
        handlers.add(handler);
    }
}
