package com.prince;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class NodeViewer extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String STRING_UNKNOWN = "unknown";
	private static final String NO_OWNER = "no owner";

	private String[] columnNames = {
			"IP address",
			"In my county",
			"Prince owner",
			"Timestamp in",
			"State",
			"Role"
	};

	private SimpleDateFormat simpleDateFormat;
	private Map<String, ErraNode> nodes;
	private ErraNode prince;
	private JFrame frame;
	private JTable table;
	private JScrollPane scrollPane;

	public NodeViewer() {
		super(new GridLayout(1,0));
		nodes = null;
		simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSSS yyyy-MM-dd");
	}
	
	public void startViewer(Map<String, ErraNode> newNodes, ErraNode newPrince) {
		nodes = newNodes;
		prince = newPrince;
		if (frame != null && frame.isShowing()) {
			frame.setVisible(false);
			frame.dispose();
		}
		this.removeAll();
		frame = new JFrame("ERRA Nodes");
		String[][] data = new String[nodes.size() + 1][columnNames.length];
		int rowIndex = 0;
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			data[rowIndex][0] = currentNode.getIPAddress();
			if (currentNode.isInMyCounty()) {
				data[rowIndex][1] = "Yes";
				data[rowIndex][2] = "ME (" + prince.getIPAddress() + ")";
			} else {
				data[rowIndex][1] = "No";
				if (currentNode.getBootstrapOwner() == null) {
					data[rowIndex][2] = STRING_UNKNOWN;
				} else {
					data[rowIndex][2] = currentNode.getBootstrapOwner().getIPAddress();
				}
			}
			data[rowIndex][3] = simpleDateFormat.format(currentNode.getJoinTime());
			switch (currentNode.getNodeState()) {
			case NODE_STATE_ALIVE:
				data[rowIndex][4] = "alive";
				break;
			case NODE_STATE_MISSING:
				data[rowIndex][4] = "missing";
				break;
			case NODE_STATE_DEAD:
				data[rowIndex][4] = "dead";
				break;
			default:
				data[rowIndex][4] = "unknown";
				break;
			}
			switch (currentNode.getNodeType()) {
			case NODE_TYPE_PRINCE:
				data[rowIndex][5] = "prince";
				break;
			case NODE_TYPE_SUBJECT:
				data[rowIndex][5] = "subject";
				break;
			default:
				data[rowIndex][5] = "unknown";
				break;
			}
			rowIndex++;
		}
		data[rowIndex][0] = "ME (" + prince.getIPAddress() + ")";
		data[rowIndex][1] = "-";
		data[rowIndex][2] = NO_OWNER;
		data[rowIndex][3] = simpleDateFormat.format(prince.getJoinTime());
		switch (prince.getNodeState()) {
		case NODE_STATE_ALIVE:
			data[rowIndex][4] = "alive";
			break;
		case NODE_STATE_MISSING:
			data[rowIndex][4] = "missing";
			break;
		case NODE_STATE_DEAD:
			data[rowIndex][4] = "dead";
			break;
		default:
			data[rowIndex][4] = "unknown";
			break;
		}
		switch (prince.getNodeType()) {
		case NODE_TYPE_PRINCE:
			data[rowIndex][5] = "prince";
			break;
		case NODE_TYPE_SUBJECT:
			data[rowIndex][5] = "subject";
			break;
		default:
			data[rowIndex][5] = "unknown";
			break;
		}
		table = new JTable(data, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(900, 200));
		table.setFillsViewportHeight(true);
		scrollPane = new JScrollPane(table);
		add(scrollPane);
		this.setOpaque(true);
		frame.setContentPane(this);
		frame.pack();
		frame.setVisible(true);
	}
	
	public void refreshNetworkViewer(Map<String, ErraNode> newNodes, ErraNode prince) {
		String[][] data = new String[nodes.size() + 1][columnNames.length];
		int rowIndex = 0;
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			data[rowIndex][0] = currentNode.getIPAddress();
			if (currentNode.isInMyCounty()) {
				data[rowIndex][1] = "Yes";
				data[rowIndex][2] = "ME (" + prince.getIPAddress() + ")";
			} else {
				data[rowIndex][1] = "No";
				if (currentNode.getBootstrapOwner() == null) {
					data[rowIndex][2] = STRING_UNKNOWN;
				} else {
					data[rowIndex][2] = currentNode.getBootstrapOwner().getIPAddress();
				}
			}
			data[rowIndex][3] = simpleDateFormat.format(currentNode.getJoinTime());
			switch (currentNode.getNodeState()) {
			case NODE_STATE_ALIVE:
				data[rowIndex][4] = "alive";
				break;
			case NODE_STATE_MISSING:
				data[rowIndex][4] = "missing";
				break;
			case NODE_STATE_DEAD:
				data[rowIndex][4] = "dead";
				break;
			default:
				data[rowIndex][4] = "unknown";
				break;
			}
			switch (currentNode.getNodeType()) {
			case NODE_TYPE_PRINCE:
				data[rowIndex][5] = "prince";
				break;
			case NODE_TYPE_SUBJECT:
				data[rowIndex][5] = "subject";
				break;
			default:
				data[rowIndex][5] = "unknown";
				break;
			}
			rowIndex++;
		}
		data[rowIndex][0] = "ME (" + prince.getIPAddress() + ")";
		data[rowIndex][1] = "-";
		data[rowIndex][2] = NO_OWNER;
		data[rowIndex][3] = simpleDateFormat.format(prince.getJoinTime());
		switch (prince.getNodeState()) {
		case NODE_STATE_ALIVE:
			data[rowIndex][4] = "alive";
			break;
		case NODE_STATE_MISSING:
			data[rowIndex][4] = "missing";
			break;
		case NODE_STATE_DEAD:
			data[rowIndex][4] = "dead";
			break;
		default:
			data[rowIndex][4] = "unknown";
			break;
		}
		switch (prince.getNodeType()) {
		case NODE_TYPE_PRINCE:
			data[rowIndex][5] = "prince";
			break;
		case NODE_TYPE_SUBJECT:
			data[rowIndex][5] = "subject";
			break;
		default:
			data[rowIndex][5] = "unknown";
			break;
		}
	}

	public void showNetwork(Map<String, ErraNode> newNodes, ErraNode prince) {
		nodes = newNodes;
		if (frame != null && frame.isShowing()) {
			frame.setVisible(false);
			frame.dispose();
		}
		this.removeAll();
		frame = new JFrame("ERRA Nodes");
		String[][] data = new String[nodes.size() + 1][columnNames.length];
		int rowIndex = 0;
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			data[rowIndex][0] = currentNode.getIPAddress();
			if (currentNode.isInMyCounty()) {
				data[rowIndex][1] = "Yes";
				data[rowIndex][2] = "ME (" + prince.getIPAddress() + ")";
			} else {
				data[rowIndex][1] = "No";
				if (currentNode.getBootstrapOwner() == null) {
					data[rowIndex][2] = STRING_UNKNOWN;
				} else {
					data[rowIndex][2] = currentNode.getBootstrapOwner().getIPAddress();
				}
			}
			data[rowIndex][3] = simpleDateFormat.format(currentNode.getJoinTime());
			switch (currentNode.getNodeState()) {
			case NODE_STATE_ALIVE:
				data[rowIndex][4] = "alive";
				break;
			case NODE_STATE_MISSING:
				data[rowIndex][4] = "missing";
				break;
			case NODE_STATE_DEAD:
				data[rowIndex][4] = "dead";
				break;
			default:
				data[rowIndex][4] = "unknown";
				break;
			}
			switch (currentNode.getNodeType()) {
			case NODE_TYPE_PRINCE:
				data[rowIndex][5] = "prince";
				break;
			case NODE_TYPE_SUBJECT:
				data[rowIndex][5] = "subject";
				break;
			default:
				data[rowIndex][5] = "unknown";
				break;
			}
			rowIndex++;
		}
		data[rowIndex][0] = "ME (" + prince.getIPAddress() + ")";
		data[rowIndex][1] = "-";
		data[rowIndex][2] = NO_OWNER;
		data[rowIndex][3] = simpleDateFormat.format(prince.getJoinTime());
		switch (prince.getNodeState()) {
		case NODE_STATE_ALIVE:
			data[rowIndex][4] = "alive";
			break;
		case NODE_STATE_MISSING:
			data[rowIndex][4] = "missing";
			break;
		case NODE_STATE_DEAD:
			data[rowIndex][4] = "dead";
			break;
		default:
			data[rowIndex][4] = "unknown";
			break;
		}
		switch (prince.getNodeType()) {
		case NODE_TYPE_PRINCE:
			data[rowIndex][5] = "prince";
			break;
		case NODE_TYPE_SUBJECT:
			data[rowIndex][5] = "subject";
			break;
		default:
			data[rowIndex][5] = "unknown";
			break;
		}
		table = new JTable(data, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(900, 200));
		table.setFillsViewportHeight(true);
		scrollPane = new JScrollPane(table);
		add(scrollPane);
		this.setOpaque(true);
		frame.setContentPane(this);
		frame.pack();
		frame.setVisible(true);
	}
}
