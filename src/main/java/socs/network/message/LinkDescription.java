package socs.network.message;

import socs.network.node.Link;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String linkID;
  public int portNum;
  public int tosMetrics;

  public String toString() {
    return linkID + ","  + portNum + "," + tosMetrics;
  }

  @Override
  public boolean equals(Object obj) {
    LinkDescription other = (LinkDescription) obj;

    return linkID.equals(other) && portNum == other.portNum && tosMetrics == other.tosMetrics;
  }
}
