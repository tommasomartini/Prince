package com.prince;

import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.helpful.SimpleTableDemo;

public class NodeViewer extends JPanel {

	private static final long serialVersionUID = 1L;

	private String[] columnNames = {
			"Erra ID",
			"IP address",
			"Timestamp in",
			"State"
	};

	private HashMap<Integer, BootstrapNode.ErraNode> nodes;
	private JTable table;

	public NodeViewer(HashMap<Integer, BootstrapNode.ErraNode> newNodes) {
		nodes = newNodes;
//		table = new 
		table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
	}
	
	public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
	
	private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("SimpleTableDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the content pane.
        NodeViewer newContentPane = new NodeViewer(null);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
