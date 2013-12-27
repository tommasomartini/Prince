package com.prince;

import java.util.Calendar;
import java.util.Date;

public class ErraNode {
	
	public enum NodeType {
		NODE_TYPE_PRINCE,
		NODE_TYPE_SUBJECT,
		UNKNOWN
	}
	
	public enum NodeState {
		NODE_STATE_ALIVE,
		NODE_STATE_MISSING,
		NODE_STATE_DEAD, 
		UNKNOWN
	}

	private final int ID;
	private final String IP_ADDRESS;
	private final Date joinTime;
	private final NodeType nodeType;
	private NodeState nodeState;

	public ErraNode(int id, String ip, NodeType nodeType, NodeState nodeState) {
		ID = id;
		IP_ADDRESS = ip;
		joinTime = Calendar.getInstance().getTime();
		this.nodeType = nodeType;
		this.nodeState = nodeState;
	}
	
	public ErraNode(int id, String ip) {
		ID = id;
		IP_ADDRESS = ip;
		joinTime = Calendar.getInstance().getTime();
		nodeType = NodeType.UNKNOWN;
		nodeState = NodeState.UNKNOWN;
	}

	public int getID() {
		return ID;
	}

	public String getIP_ADDRESS() {
		return IP_ADDRESS;
	}

	public Date getJoinTime() {
		return joinTime;
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	public NodeState getNodeState() {
		return nodeState;
	}

	public void setNodeState(NodeState nodeState) {
		this.nodeState = nodeState;
	}
}
