package socs.network.node;

import socs.network.events.Event;

public class RouterDescription {
  //used to socket communication
  String processIPAddress;
  short processPortNumber;
  //used to identify the router in the simulated network space
  String simulatedIPAddress;
  //status of the router
  private RouterStatus status = RouterStatus.NONE;
  //public Event<RouterDescription,RouterStatus> updateEvent = new Event<>();


  public void setStatus(RouterStatus status)
  {
    this.status = status;

    System.out.println("Router status of router: " + simulatedIPAddress + ":" + processPortNumber  + " set to " + status);
    //updateEvent.invoke(this,status);

  }

  public RouterStatus getStatus()
  {
    return status;
  }
}
