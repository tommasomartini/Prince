package com.prince;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class NodeViewer extends JPanel {

	private static final long serialVersionUID = 1L;

	private String[] columnNames = {
			"Erra ID",
			"IP address",
			"Timestamp in",
			"State",
			"Role"
	};

	private SimpleDateFormat simpleDateFormat;
	private Map<Integer, ErraNode> nodes;
	private JFrame frame;
	private JTable table;
	private JLabel label;
	private JScrollPane scrollPane;

	public NodeViewer() {
		super(new GridLayout(1,0));
		nodes = null;
		simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSSS yyyy-MM-dd");
	}

	public void showNetwork(Map<Integer, ErraNode> newNodes, ErraNode bootstrap) {
		nodes = newNodes;
		if (frame != null && frame.isShowing()) {
			frame.setVisible(false);
			frame.dispose();
		}
		this.removeAll();
		frame = new JFrame("ERRA Nodes");
		if (!nodes.isEmpty()) {
			String[][] data = new String[nodes.size() + 1][columnNames.length];
			int rowIndex = 0;
			for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
				ErraNode currentNode = entry.getValue();
				data[rowIndex][0] = String.valueOf(currentNode.getID());
				data[rowIndex][1] = currentNode.getIP_ADDRESS();	
				data[rowIndex][2] = simpleDateFormat.format(currentNode.getJoinTime());
				switch (currentNode.getNodeState()) {
				case NODE_STATE_ALIVE:
					data[rowIndex][3] = "alive";
					break;
				case NODE_STATE_MISSING:
					data[rowIndex][3] = "missing";
					break;
				case NODE_STATE_DEAD:
					data[rowIndex][3] = "dead";
					break;
				default:
					data[rowIndex][3] = "unknown";
					break;
				}
				switch (currentNode.getNodeType()) {
				case NODE_TYPE_PRINCE:
					data[rowIndex][4] = "prince";
					break;
				case NODE_TYPE_SUBJECT:
					data[rowIndex][4] = "subject";
					break;
				default:
					data[rowIndex][4] = "unknown";
					break;
				}
				rowIndex++;
			}
			data[rowIndex][0] = String.valueOf(bootstrap.getID());
			data[rowIndex][1] = bootstrap.getIP_ADDRESS();	
			data[rowIndex][2] = simpleDateFormat.format(bootstrap.getJoinTime());
			switch (bootstrap.getNodeState()) {
			case NODE_STATE_ALIVE:
				data[rowIndex][3] = "alive";
				break;
			case NODE_STATE_MISSING:
				data[rowIndex][3] = "missing";
				break;
			case NODE_STATE_DEAD:
				data[rowIndex][3] = "dead";
				break;
			default:
				data[rowIndex][3] = "unknown";
				break;
			}
			switch (bootstrap.getNodeType()) {
			case NODE_TYPE_PRINCE:
				data[rowIndex][4] = "prince";
				break;
			case NODE_TYPE_SUBJECT:
				data[rowIndex][4] = "subject";
				break;
			default:
				data[rowIndex][4] = "unknown";
				break;
			}
			table = new JTable(data, columnNames);
			table.setPreferredScrollableViewportSize(new Dimension(1400, 200));
			table.setFillsViewportHeight(true);
			scrollPane = new JScrollPane(table);
			add(scrollPane);
			this.setOpaque(true);
			frame.setContentPane(this);
			frame.pack();
			frame.setVisible(true);
		} else {
			label = new JLabel("no nodes");
			add(label);
			this.setOpaque(true);
			frame.setContentPane(this);
			frame.pack();
			frame.setVisible(true);
		}
	}
}
