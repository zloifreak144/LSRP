package socs.network.node;

public class RouterDescription {
  //used to socket communication
  String processIPAddress;
  short processPortNumber;
  //used to identify the router in the simulated network space
  String simulatedIPAddress;
  //status of the router
  private RouterStatus status = RouterStatus.NONE;

  public void setStatus(RouterStatus status)
  {
    this.status = status;

    System.out.println("Router status of router: " + simulatedIPAddress + ":" + processPortNumber  + " set to " + status);
  }

  public RouterStatus getStatus()
  {
    return status;
  }
}
