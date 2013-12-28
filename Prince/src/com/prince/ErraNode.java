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

	private final String ipAddress;
	private final Date joinTime;
	private NodeType nodeType;
	private NodeState nodeState;
	private boolean isInMyCounty;
	private ErraNode bootstrapOwner;

	public ErraNode(String ip, NodeType nodeType, NodeState nodeState) {
		ipAddress = ip;
		joinTime = Calendar.getInstance().getTime();
		this.nodeType = nodeType;
		this.nodeState = nodeState;
		isInMyCounty = false;
		bootstrapOwner = null;
	}
	
	public ErraNode(String ip) {
		ipAddress = ip;
		joinTime = Calendar.getInstance().getTime();
		nodeType = NodeType.UNKNOWN;
		nodeState = NodeState.UNKNOWN;
		isInMyCounty = false;
		bootstrapOwner = null;
	}

	public String getIPAddress() {
		return ipAddress;
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

	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}

	public boolean isInMyCounty() {
		return isInMyCounty;
	}

	public void setInMyCounty(boolean isInMyCounty) {
		this.isInMyCounty = isInMyCounty;
	}

	public ErraNode getBootstrapOwner() {
		return bootstrapOwner;
	}

	public void setBootstrapOwner(ErraNode bootstrapOwner) {
		this.bootstrapOwner = bootstrapOwner;
	}
}
