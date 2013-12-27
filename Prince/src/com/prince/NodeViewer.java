package com.prince;

import java.awt.Dimension;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.prince.BootstrapNode.ErraNode;

public class NodeViewer extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String UNKNOWN = "unknown";

	private String[] columnNames = {
			"Erra ID",
			"IP address",
			"Timestamp in",
			"State"
	};

	private Map<Integer, BootstrapNode.ErraNode> nodes;
	
	public NodeViewer() {
		super();
		nodes = null;
	}

	public void showNetwork(Map<Integer, BootstrapNode.ErraNode> newNodes) {
		nodes = newNodes;
		if (!nodes.isEmpty()) {
			JFrame frame = new JFrame("ERRA Nodes");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			String[][] data = new String[nodes.size()][columnNames.length];
			int rowIndex = 0;
			for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
				ErraNode currentNode = entry.getValue();
				data[rowIndex][0] = String.valueOf(currentNode.getID());
				data[rowIndex][1] = currentNode.getIP_ADDRESS();
				data[rowIndex][2] = UNKNOWN;
				data[rowIndex][3] = "alive";
				rowIndex++;
			}
			JTable table = new JTable(data, columnNames);
			table.setPreferredScrollableViewportSize(new Dimension(500, 70));
	        table.setFillsViewportHeight(true);
			JScrollPane scrollPane = new JScrollPane(table);
			add(scrollPane);
			this.setOpaque(true);
			frame.setContentPane(this);
			frame.pack();
			frame.setVisible(true);
		} else {
			
		}
	}
}
