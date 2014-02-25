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
import java.lang.Character.Subset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;
import com.prince.PrinceNode.PrinceState;

public class PrinceGUI extends JFrame implements ActionListener {

	/**
	 * Randomly generated
	 */
	private static final long serialVersionUID = 1693846911440567274L;
	
	private static final String IP = "10.10.10.10";
	
	private static final int POINT_X = 200;
	private static final int POINT_Y = 200;
	private static final int OVERVIEW_TABLE_WIDTH = 500;
	private static final int OVERVIEW_TABLE_HEIGTH = 300;
	private static final int NODE_INFO_TABLE_WIDTH = 300;
	private static final int NODE_INFO_TABLE_HEIGTH = 100;

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
	
	private String[] fieldName = {
		"Parameter",
		"Value"
	};
	
	private SimpleDateFormat simpleDateFormat;
	private Map<String, ErraNode> nodes;
	private String[][] overviewData;
	private String[][] nodeData;
	private String myIPAddress;
	private ErraNode focusedNode;
	private PrinceState currentState;

	/*
	 * GUI elements
	 */
	private JPanel panel;
	
	// North panel
	private JTable table;
	private JScrollPane scrollPaneOverviewTable;
	
	// East panel
	private JPanel pnlEast;
	private JButton bt1;
	private JButton bt2;
	private JButton bt3;
	
	// Center panel
	private JPanel pnlCenter;
	private JPanel nodeInfoPanel;
	private JPanel genericInfoPanel;
	private JTable tbNodeInfo;
	private JScrollPane scrollPaneNodeInfo;
	private JLabel status;
	
	public static void main(String[] args) {
		PrinceGUI princeGUI = new PrinceGUI(generateNodes(), IP);
	}

	public PrinceGUI(Map<String, ErraNode> newNodes, String myIP) {
		super("Erra Prince: " + myIP);
		nodes = newNodes;
		myIPAddress = myIP;
		simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSSS yyyy-MM-dd");
		focusedNode = null;
		
		// Frame options
//		setResizable(false);
		setLocation(new Point(POINT_X, POINT_Y));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);	// FIXME non bene chiudere il programma all'uscita!
		
		generateOverviewData();	
		nodeData = new String[columnNamesInfoNode.length][2];
		nodeData[0][0] = columnNamesInfoNode[0];
		nodeData[1][0] = columnNamesInfoNode[1];
		nodeData[2][0] = columnNamesInfoNode[2];
		nodeData[3][0] = columnNamesInfoNode[3];
		nodeData[0][1] = "-";
		nodeData[1][1] = "-";
		nodeData[2][1] = "-";
		nodeData[3][1] = "-";
		
		/********************************************************************************
		 * Graphics
		 */
		panel = new JPanel(new BorderLayout());
		
		// North panel		
		table = new JTable();
		table.addMouseListener(new PrinceMouseAdapter());
		table.setModel(new PrinceTableModel(overviewData, columnNamesOverview));
		table.setPreferredScrollableViewportSize(new Dimension(OVERVIEW_TABLE_WIDTH, OVERVIEW_TABLE_HEIGTH));
		table.setFillsViewportHeight(true);
//		table.setBackground(Color.BLACK);
		scrollPaneOverviewTable = new JScrollPane(table);
		panel.add(scrollPaneOverviewTable, BorderLayout.NORTH);
		
		// East panel
		pnlEast = new JPanel(new GridLayout(3, 1));
		bt1 = new JButton("A");
		bt1.setActionCommand("cmd1");
		bt1.addActionListener(this);
		bt2 = new JButton("B");
		bt2.setActionCommand("cmd2");
		bt2.addActionListener(this);
		bt3 = new JButton("C");
		bt3.setActionCommand("cmd3");
		bt3.addActionListener(this);
		pnlEast.add(bt1);
		pnlEast.add(bt2);
		pnlEast.add(bt3);
		panel.add(pnlEast, BorderLayout.EAST);
		
		// Center panel
		pnlCenter = new JPanel(new GridLayout(2, 1));
		nodeInfoPanel = new JPanel();
		nodeInfoPanel.setBackground(Color.YELLOW);
		genericInfoPanel = new JPanel();
		genericInfoPanel.setBackground(Color.CYAN);
		tbNodeInfo = new JTable();
		tbNodeInfo.setModel(new PrinceTableModel(nodeData, fieldName));
//		tbNodeInfo.setPreferredScrollableViewportSize(new Dimension(NODE_INFO_TABLE_WIDTH, NODE_INFO_TABLE_HEIGTH));
		tbNodeInfo.setFillsViewportHeight(true);
		scrollPaneNodeInfo = new JScrollPane(tbNodeInfo);
		nodeInfoPanel.add(scrollPaneNodeInfo);
		pnlCenter.add(nodeInfoPanel);
		pnlCenter.add(genericInfoPanel);
		panel.add(pnlCenter, BorderLayout.CENTER);
		
		setContentPane(panel);
		pack();
		setVisible(true);
	}
	
	public void updateTable(Map<String, ErraNode> newNodes) {
		nodes = newNodes;
		generateOverviewData();
		table.setModel(new PrinceTableModel(overviewData, columnNamesOverview));
	}
	
	public void updateState(PrinceState newState) {
		currentState = newState;
		switch (currentState) {
		case STATE_RUNNING:
			genericInfoPanel.add(new JLabel("Running"));
			break;
		case STATE_INITIALIZING:
			genericInfoPanel.add(new JLabel("Initializing"));
			break;
		case STATE_ROLL_CALLING:
			genericInfoPanel.add(new JLabel("Roll calling"));
			break;
		case STATE_SHUTTING_DOWN:
			genericInfoPanel.add(new JLabel("Shutting down"));
			break;
		case STATE_SPREADING_CHANGES:
			genericInfoPanel.add(new JLabel("Spreading changes"));
			break;
		default:
			genericInfoPanel.add(new JLabel("Unknown"));
			break;
		}
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
			if (currentNode.getIPAddress() == myIPAddress) {
				overviewData[rowIndex][0] = currentNode.getIPAddress();
				overviewData[rowIndex][1] = "---";
			} else {
				switch (currentNode.getNodeType()) {
				case NODE_TYPE_PRINCE:
					overviewData[rowIndex][0] = currentNode.getIPAddress();
					break;
				case NODE_TYPE_SUBJECT:
					overviewData[rowIndex][0] = currentNode.getIPAddress();		
					break;
				case UNKNOWN:
					overviewData[rowIndex][0] = currentNode.getIPAddress();
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
		ErraNode node = nodes.get(nodeAddress);

		switch (node.getNodeType()) {
		case NODE_TYPE_PRINCE:
			nodeData[0][1] = node.getIPAddress() + " (P)";
			break;
		case NODE_TYPE_SUBJECT:
			nodeData[0][1] = node.getIPAddress() + " (S)";
			break;
		case UNKNOWN:
			nodeData[0][1] = node.getIPAddress() + " (unknown type)";
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
	
	private static Map<String, ErraNode> generateNodes() {
		Map<String, ErraNode> fooNodes = new HashMap<String, ErraNode>();
		
		ErraNode p1 = new ErraNode(IP, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
		p1.setInMyCounty(true);
		p1.setBootstrapOwner(null);
		
		ErraNode p2 = new ErraNode("20.20.20.20", NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
		p2.setInMyCounty(false);
		p1.setBootstrapOwner(null);
		
		ErraNode s1 = new ErraNode("40.40.40.40", NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
		s1.setInMyCounty(true);
		s1.setBootstrapOwner(p1);
		
		ErraNode s2 = new ErraNode("50.50.50.50", NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
		s2.setInMyCounty(true);
		s2.setBootstrapOwner(p1);
		
		ErraNode s3 = new ErraNode("60.60.60.60", NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
		s3.setInMyCounty(false);
		s3.setBootstrapOwner(p2);
		
		fooNodes.put(p1.getIPAddress(), p1);
		fooNodes.put(p2.getIPAddress(), p2);
		fooNodes.put(s1.getIPAddress(), s1);
		fooNodes.put(s2.getIPAddress(), s2);
		fooNodes.put(s3.getIPAddress(), s3);
		
		return fooNodes;
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
			int selRow = currentTable.getSelectedRow();
//			int selCol = currentTable.getSelectedColumn();
			currentTable.clearSelection();
			genericInfoPanel.removeAll();
			String ipSelected = overviewData[selRow][0];
			focusedNode = nodes.get(ipSelected);
			System.out.println(ipSelected);
			generateNodeInfoData(focusedNode.getIPAddress());
			tbNodeInfo.setModel(new PrinceTableModel(nodeData, fieldName));	// TODO prova a mettere null anziche' fieldName
			nodeInfoPanel.validate();
			nodeInfoPanel.repaint();
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
