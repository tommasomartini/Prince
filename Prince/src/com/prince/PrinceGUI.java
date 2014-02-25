package com.prince;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public class PrinceGUI extends JFrame implements ActionListener {

	/**
	 * Randomly generated
	 */
	private static final long serialVersionUID = 1693846911440567274L;

	private static final String STRING_UNKNOWN = "unknown";
	private static final String NO_OWNER = "no owner";
	
	private String[] columnNamesOverview = {
			"IP address",
			"Prince owner",
			"Node state"
	};
	
	private String[] columnNamesInfoNode = {
			"IP address",
			"Prince owner",
			"State",
			"Timestamp in"
	};

	/*
	 * GUI elements
	 */
	
	private JPanel panel;
	
	// North panel
	private JTable table;
	private JScrollPane scrollPane;
	
	// East panel
	private JPanel pnlEast;
	private JButton bt1;
	private JButton bt2;
	private JButton bt3;
	
	// South panel
	private JPanel pnlSouth;
	private JPanel nodeInfoPanel;
	private JPanel genericInfoPanel;
	private JTable nodeInfo;
	
	private SimpleDateFormat simpleDateFormat;
	private Map<String, ErraNode> nodes;
	
	private String[][] overviewData;
	private String[][] nodeData;
	
	private String myIPAddress;
	private ErraNode me;
	
	public static void main(String[] args) {
		PrinceGUI princeGUI = new PrinceGUI(null, "");
	}

	public PrinceGUI(Map<String, ErraNode> newNodes, String myIP) {
		super("Erra Prince: " + myIP);
		nodes = newNodes;
		myIPAddress = myIP;
		
		// Frame options
		setResizable(false);
		setLocation(new Point(500, 500));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);	// FIXME non bene chiudere il programma all'uscita!
		
		int overviewColNum = columnNamesOverview.length;
		
		overviewData = new String[overviewColNum][overviewColNum];
		for (int i = 0; i < overviewData.length; i++) {
			for (int j = 0; j < overviewData[i].length; j++) {
				overviewData[i][j] = "cell: " + "(" + i + ", " + j + ")";
			}
		}

		simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSSS yyyy-MM-dd");
		
		// Graphics
		panel = new JPanel(new BorderLayout());
		
		// North panel		
		table = new JTable();
		table.addMouseListener(new PrinceMouseAdapter());
		table.setModel(new PrinceTableModel(overviewData, columnNamesOverview));
		table.setPreferredScrollableViewportSize(new Dimension(900, 200));
		table.setFillsViewportHeight(true);
		table.setBackground(Color.BLACK);
		scrollPane = new JScrollPane(table);
		panel.add(scrollPane, BorderLayout.NORTH);
		
		// East panel
		pnlEast = new JPanel(new GridLayout(3, 1));
		
		bt1 = new JButton("A");
		bt1.setBackground(Color.GREEN);
		bt1.setActionCommand("cmd1");
		bt1.addActionListener(this);
		
		bt2 = new JButton("B");
		bt2.setBackground(Color.YELLOW);
		bt2.setActionCommand("cmd2");
		bt2.addActionListener(this);
		
		bt3 = new JButton("C");
		bt3.setBackground(Color.RED);
		bt3.setActionCommand("cmd3");
		bt3.addActionListener(this);
		
		pnlEast.add(bt1);
		pnlEast.add(bt2);
		pnlEast.add(bt3);
		
		panel.add(pnlEast, BorderLayout.EAST);
		
		// South panel
		pnlSouth = new JPanel();
		pnlSouth.setBackground(Color.PINK);
		panel.add(pnlSouth, BorderLayout.CENTER);
		
		setContentPane(panel);
		pack();
		setVisible(true);
	}
	
	private void generateOverviewData() {
		overviewData = new String[nodes.size()][columnNamesOverview.length];	// FIXME in nodes ci sono anche io???
		int rowIndex = 0;
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			switch (currentNode.getNodeState()) {
			case NODE_STATE_ALIVE:
				overviewData[rowIndex][2] = "Alive";
				break;			
			case NODE_STATE_DEAD:
				overviewData[rowIndex][2] = "Dead";
				break;			
			case NODE_STATE_MISSING:
				overviewData[rowIndex][2] = "Missing";
				break;
			default:
				overviewData[rowIndex][2] = "Unknown state";
				break;
			}
			if (currentNode.getIPAddress() == me.getIPAddress()) {
				overviewData[rowIndex][0] = "Me: " + currentNode.getIPAddress() + "(P)";
				overviewData[rowIndex][1] = "---";
			} else {
				switch (currentNode.getNodeType()) {
				case NODE_TYPE_PRINCE:
					overviewData[rowIndex][0] = currentNode.getIPAddress() + "(P)";
					break;
				case NODE_TYPE_SUBJECT:
					overviewData[rowIndex][0] = currentNode.getIPAddress() + "(S)";		
					break;
				case UNKNOWN:
					overviewData[rowIndex][0] = currentNode.getIPAddress() + "(unknown type)";
					break;
				default:
					break;
				}
				if (currentNode.isInMyCounty()) {
					overviewData[rowIndex][1] = "Me";
				} else {
					overviewData[rowIndex][1] = "No";
					if (currentNode.getBootstrapOwner() == null) {
						overviewData[rowIndex][1] = STRING_UNKNOWN;
					} else {
						overviewData[rowIndex][1] = currentNode.getBootstrapOwner().getIPAddress();
					}
				}
			}
			rowIndex++;
		}
	}
	
	private void generateNodeInfoData(String nodeAddress) {
		nodeData = new String[columnNamesInfoNode.length][2];
		ErraNode node = nodes.get(nodeAddress);
		nodeData[0][0] = columnNamesInfoNode[0];
		nodeData[1][0] = columnNamesInfoNode[1];
		nodeData[2][0] = columnNamesInfoNode[2];
		nodeData[3][0] = columnNamesInfoNode[3];

		switch (node.getNodeType()) {
		case NODE_TYPE_PRINCE:
			nodeData[0][1] = node.getIPAddress() + "(P)";
			break;
		case NODE_TYPE_SUBJECT:
			nodeData[0][1] = node.getIPAddress() + "(S)";
			break;
		case UNKNOWN:
			nodeData[0][1] = node.getIPAddress() + "(unknown type)";
			break;
		default:
			break;
		}
		if (nodeAddress.equalsIgnoreCase(myIPAddress)) {
			nodeData[1][1] = "---";
		} else {
			if (node.getBootstrapOwner() == null) {
				nodeData[1][1] = "unknown";
			} else {
				nodeData[1][1] = node.getBootstrapOwner().getIPAddress();
			}
		}
		switch (node.getNodeState()) {
		case NODE_STATE_ALIVE:
			overviewData[2][1] = "Alive";
			break;	
		case NODE_STATE_DEAD:
			overviewData[2][1] = "Dead";
			break;
		case NODE_STATE_MISSING:
			overviewData[2][1] = "Missing";
			break;
		default:
			overviewData[2][1] = "Unknown state";
			break;
		}
		nodeData[3][1] = simpleDateFormat.format(node.getJoinTime());
	}
	
	/*
	 * Inherited methods
	 */

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		System.out.println(actionCommand);

	}
	
	/*
	 * Private classes
	 */
	
	private class PrinceTableModel extends DefaultTableModel {

		public PrinceTableModel(Object[][] data, Object[] columnNames) {
			super(data, columnNames);
		}

		public boolean isCellEditable(int row,int cols) {
			return false;
		}
	}
	
	private class PrinceMouseAdapter extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			JTable currentTable = (JTable) e.getSource();
			int selection = currentTable.getSelectedRow();
			int ss = currentTable.getSelectedColumn();
			currentTable.clearSelection();
			System.out.println("Mouse clicked on cell " + overviewData[selection][ss]);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
//			System.out.println("Mouse pressed!");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			super.mouseEntered(e);
//			System.out.println("Mouse entered!");
		}

		@Override
		public void mouseExited(MouseEvent e) {
			super.mouseExited(e);
//			System.out.println("Mouse exited!");
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			super.mouseWheelMoved(e);
//			System.out.println("Mouse whhel moved!");
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			super.mouseDragged(e);
//			System.out.println("Mouse dragged!");
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);
//			System.out.println("Mouse moved!");
		}
	}
}
