package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
    String shortest = "";
    return shortest;
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public void update(Link link)
  {
    //make it such that you can add/remove, not only add
    LinkDescription ld = new LinkDescription();
    ld.linkID = link.router2.simulatedIPAddress;
    ld.portNum = link.router2.processPortNumber;
    ld.tosMetrics = link.weight;
    LSA lsa = _store.get(rd.simulatedIPAddress);

    lsa.lsaSeqNumber++;

    if(!lsa.hasLink(ld))
    {
      lsa.links.add(ld);
    }
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public Path getShortestPath()
  {

  }

  class Path
  {
    ArrayList<PathSegment> path = new ArrayList<>();

    void addSegment(String srcID, String destID, short weight)
    {
      path.add(new PathSegment(srcID, destID, weight));
    }

    void removeFrom(String srcID)
    {
      int index = -1;

      for(int i = 0; i < path.size(); i++)
      {
        PathSegment temp = path.get(i);

        if(temp.getSrcID().equals(srcID))
        {
          index = i;
        }
      }

      if(index != -1)
      {
        for(int i = index; i < path.size(); i++)
        {
          path.remove(i);
        }
      }

    }

    @Override
    public String toString() {
      String path ="";

      for(PathSegment segment : this.path)
      {
        path += segment + " --> ";
      }

      return path;
    }
  }

  class PathSegment
  {
    short weight;
    String srcID;
    String destID;

    PathSegment(String srcID, String destID, short weight)
    {
      this.weight = weight;
      this.srcID = srcID;
      this.destID = destID;
    }

    short getWeight() { return weight; }
    String getDestID() { return srcID; }
    String getSrcID() { return destID; }

    @Override
    public String toString() {
      return srcID + "-(" + weight + ")-" + destID;
    }
  }

}
