package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

public class LSA implements Serializable {

  //IP address of the router originate this LSA
  public String linkStateID;
  public int lsaSeqNumber = Integer.MIN_VALUE;

  public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
    for (LinkDescription ld : links) {
      sb.append(ld);
    }
    sb.append("\n");
    return sb.toString();
  }

  public boolean hasLink(LinkDescription link)
  {
    for(LinkDescription ld : links)
    {
      if(link.equals(link))
      {
        return true;
      }
    }

    return false;
  }

}
