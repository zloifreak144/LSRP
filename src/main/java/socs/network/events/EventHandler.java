package socs.network.events;

public interface EventHandler<T, R>
{
    public void handle(T eventArg1, R eventArg2);
}
