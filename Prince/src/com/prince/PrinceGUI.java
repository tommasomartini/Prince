package com.prince;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;
import com.prince.PrinceNode.PrinceState;

public class PrinceGUI extends JFrame implements ActionListener {

	/**
	 * Randomly generated
	 */
	private static final long serialVersionUID = 1693846911440567274L;
	
	private static final Color myColor = new Color(238, 223, 249);
	private static final Color missingColor = new Color(186, 179, 253);
	
	private static final String IP = "10.10.10.10";
	
	private static final int POINT_X = 150;
	private static final int POINT_Y = 150;
	private static final int OVERVIEW_TABLE_WIDTH = 500;
	private static final int OVERVIEW_TABLE_HEIGTH = 100;
	private static final int NODE_INFO_TABLE_WIDTH = 450;
	private static final int NODE_INFO_TABLE_HEIGTH = 112;
	
	private String[] columnNamesOverview = {
			"IP address",
			"Prince owner",
			"Node state"
	};
	
	private String[] columnNamesInfoNode = {
			"IP address",
			"Node Type",
			"Prince owner",
			"State",
			"Day",
			"Time",
			"Protectorate"
	};
	
	private String[] fieldName = {
		"Parameter",
		"Value"
	};
	
	private SimpleDateFormat simpleDateFormatDay;
	private SimpleDateFormat simpleDateFormatTime;
	private Map<String, ErraNode> nodes;
	private Map<String, Integer> ipToRowTable;
	private String[][] overviewData;
	private String[][] nodeData;
	private String myIPAddress;
	private ErraNode relatedPrince;
	private ErraNode focusedNode;
	private PrinceState currentState;

	/*
	 * GUI elements
	 */
	private JPanel panel;
	
	// North panel
	private JPanel pnlNorth;
	private PrinceTable table;
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
	private JLabel lbStatus;
	private JLabel lbMessage;
	
//	public static void main(String[] args) {
//		PrinceGUI princeGUI = new PrinceGUI(generateNodes(), IP);
//	}

	public PrinceGUI(Map<String, ErraNode> newNodes, ErraNode princeNode) {
		super("Erra Prince: " + princeNode.getIPAddress());
		nodes = newNodes;
		myIPAddress = princeNode.getIPAddress();
		simpleDateFormatDay = new SimpleDateFormat("yyyy-MM-dd");
		simpleDateFormatTime = new SimpleDateFormat("HH:mm:ss");
		focusedNode = null;
		
		// Frame options
		setResizable(false);
		setLocation(new Point(POINT_X, POINT_Y));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);	// FIXME mettere do nothing on close
		
		nodeData = new String[columnNamesInfoNode.length][2];
		nodeData[0][0] = columnNamesInfoNode[0];
		nodeData[1][0] = columnNamesInfoNode[1];
		nodeData[2][0] = columnNamesInfoNode[2];
		nodeData[3][0] = columnNamesInfoNode[3];
		nodeData[4][0] = columnNamesInfoNode[4];
		nodeData[5][0] = columnNamesInfoNode[5];
		nodeData[6][0] = columnNamesInfoNode[6];
		nodeData[0][1] = "-";
		nodeData[1][1] = "-";
		nodeData[2][1] = "-";
		nodeData[3][1] = "-";
		nodeData[4][1] = "-";
		nodeData[5][1] = "-";
		nodeData[6][1] = "-";
		
		/********************************************************************************
		 * Graphics
		 */
		panel = new JPanel(new BorderLayout());
		
		// North panel	
		pnlNorth = new JPanel();
		pnlNorth.setBackground(myColor);
		table = new PrinceTable(new PrinceTableModel(overviewData, columnNamesOverview));
		table.addMouseListener(new PrinceMouseAdapter());
		table.setPreferredScrollableViewportSize(new Dimension(OVERVIEW_TABLE_WIDTH, OVERVIEW_TABLE_HEIGTH));
		table.setFillsViewportHeight(true);
		table.setBackground(myColor);
		scrollPaneOverviewTable = new JScrollPane(table);
		pnlNorth.add(scrollPaneOverviewTable);
		panel.add(pnlNorth, BorderLayout.NORTH);
		
		// East panel
		pnlEast = new JPanel(new GridLayout(3, 1));
		bt1 = new JButton("Send");
		bt1.setActionCommand("cmd1");
		bt1.addActionListener(this);
		bt2 = new JButton("Roll Call");
		bt2.setActionCommand("cmd2");
		bt2.addActionListener(this);
		bt3 = new JButton("Shutdown");
		bt3.setActionCommand("cmd3");
		bt3.addActionListener(this);
		pnlEast.add(bt1);
		pnlEast.add(bt2);
		pnlEast.add(bt3);
		panel.add(pnlEast, BorderLayout.EAST);
		
		// Center panel
		pnlCenter = new JPanel(new GridLayout(2, 1));
		lbStatus = new JLabel("No status");
		lbMessage = new JLabel("No message");
		nodeInfoPanel = new JPanel();
		nodeInfoPanel.setBackground(myColor);
		genericInfoPanel = new JPanel(new GridLayout(2, 1));
		genericInfoPanel.setBackground(myColor);
		genericInfoPanel.add(lbStatus);
		genericInfoPanel.add(lbMessage);
		tbNodeInfo = new JTable();
		tbNodeInfo.setModel(new PrinceTableModel(nodeData, fieldName));
		tbNodeInfo.setPreferredScrollableViewportSize(new Dimension(NODE_INFO_TABLE_WIDTH, NODE_INFO_TABLE_HEIGTH));
		tbNodeInfo.setFillsViewportHeight(true);
		scrollPaneNodeInfo = new JScrollPane(tbNodeInfo);
		nodeInfoPanel.add(scrollPaneNodeInfo);
		pnlCenter.add(nodeInfoPanel);
		pnlCenter.add(genericInfoPanel);
		panel.add(pnlCenter, BorderLayout.CENTER);
		
		setContentPane(panel);
		pack();
		setVisible(true);
		//***********************************************************
//		updateTable(nodes);
	}
	
	public void updateTable(Map<String, ErraNode> newNodes) {
		nodes = newNodes;
		generateOverviewData();
		table.setModel(new PrinceTableModel(overviewData, columnNamesOverview));
		((DefaultTableModel)table.getModel()).fireTableDataChanged();
//		table = new PrinceTable(new PrinceTableModel(overviewData, columnNamesOverview));
//		scrollPaneOverviewTable.removeAll();
//		scrollPaneOverviewTable.add(table);
		for (Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode erraNode = entry.getValue();
			switch (erraNode.getNodeState()) {
			case NODE_STATE_ALIVE:
//				table.setRowColor(ipToRowTable.get(erraNode.getIPAddress()), Color.GREEN);
				break;
			case NODE_STATE_DEAD:
//				table.setRowColor(ipToRowTable.get(erraNode.getIPAddress()), Color.RED);
				break;
			case NODE_STATE_MISSING:
				table.setRowColor(ipToRowTable.get(erraNode.getIPAddress()), missingColor);
				break;
			default:
				break;
			}
		}
		pnlNorth.validate();
		pnlNorth.repaint();
	}
	
	public void updateState(PrinceState newState) {
		currentState = newState;
		switch (currentState) {
		case STATE_RUNNING:
			lbStatus.setText("Running");
			break;
		case STATE_INITIALIZING:
			lbStatus.setText("Initializing");
			break;
		case STATE_ROLL_CALLING:
			lbStatus.setText("Roll calling");
			break;
		case STATE_SHUTTING_DOWN:
			lbStatus.setText("Shutting down");
			break;
		case STATE_SPREADING_CHANGES:
			lbStatus.setText("Spreading changes");
			break;
		default:
			lbStatus.setText("Unknown");
			break;
		}
		genericInfoPanel.validate();
		genericInfoPanel.repaint();
	}
	
	public void updateMessage(String msg) {
		lbMessage.setText(msg);
		genericInfoPanel.validate();
		genericInfoPanel.repaint();
		
	}

	private void generateOverviewData() {
		overviewData = new String[nodes.size()][columnNamesOverview.length];	// FIXME in nodes ci sono anche io???
		ipToRowTable = new HashMap<String, Integer>();
		int rowIndex = 0;
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			ipToRowTable.put(currentNode.getIPAddress(), rowIndex);
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
				overviewData[rowIndex][0] = currentNode.getIPAddress() + " (P)";
				overviewData[rowIndex][1] = "no owner";
			} else {
				switch (currentNode.getNodeType()) {
				case NODE_TYPE_PRINCE:
					overviewData[rowIndex][0] = currentNode.getIPAddress() + " (P)";
					overviewData[rowIndex][1] = "no owner";
					break;
				case NODE_TYPE_SUBJECT:
					overviewData[rowIndex][0] = currentNode.getIPAddress() + " (S)";		
					if (currentNode.isInMyCounty()) {
						overviewData[rowIndex][1] = "Me";
					} else {
						if (currentNode.getBootstrapOwner() == null) {
							overviewData[rowIndex][1] = "Unknown";
						} else {
							overviewData[rowIndex][1] = currentNode.getBootstrapOwner().getIPAddress();
						}
					}
					break;
				case UNKNOWN:
					overviewData[rowIndex][0] = currentNode.getIPAddress() + " (unknown)";
					if (currentNode.isInMyCounty()) {
						overviewData[rowIndex][1] = "Me";
					} else {
						if (currentNode.getBootstrapOwner() == null) {
							overviewData[rowIndex][1] = "Unknown";
						} else {
							overviewData[rowIndex][1] = currentNode.getBootstrapOwner().getIPAddress();
						}
					}
					break;
				default:
					break;
				}
			}
			rowIndex++;
		}
	}
	
	private void generateNodeInfoData(String nodeAddress) {
		ErraNode node = nodes.get(nodeAddress);
		nodeData[0][1] = node.getIPAddress();
		switch (node.getNodeType()) {
		case NODE_TYPE_PRINCE:
			nodeData[1][1] = "Prince";
			nodeData[2][1] = "No owner";
			break;
		case NODE_TYPE_SUBJECT:
			nodeData[1][1] = "Subject";
			if (node.getBootstrapOwner() == null) {
				nodeData[2][1] = "unknown";
			} else {
				nodeData[2][1] = node.getBootstrapOwner().getIPAddress();
			}
			break;
		case UNKNOWN:
			nodeData[1][1] = "Unknown type";
			if (node.getBootstrapOwner() == null) {
				nodeData[2][1] = "unknown";
			} else {
				nodeData[2][1] = node.getBootstrapOwner().getIPAddress();
			}
			break;
		default:
			break;
		}
		switch (node.getNodeState()) {
		case NODE_STATE_ALIVE:
			nodeData[3][1] = "Alive";
			break;	
		case NODE_STATE_DEAD:
			nodeData[3][1] = "Dead";
			break;
		case NODE_STATE_MISSING:
			nodeData[3][1] = "Missing";
			break;
		default:
			nodeData[3][1] = "Unknown state";
			break;
		}
		nodeData[4][1] = simpleDateFormatDay.format(node.getJoinTime());
		nodeData[5][1] = simpleDateFormatTime.format(node.getJoinTime());
		nodeData[6][1] = (relatedPrince.getProtectorate() == null ? relatedPrince.getProtectorate().getIPAddress() : "No protectorate");
	}
	
	private static Map<String, ErraNode> generateNodes() {
		Map<String, ErraNode> fooNodes = new HashMap<String, ErraNode>();
		
		ErraNode p1 = new ErraNode(IP, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
		p1.setInMyCounty(true);
		p1.setBootstrapOwner(null);
		
		ErraNode p2 = new ErraNode("20.20.20.20", NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
		p2.setInMyCounty(false);
		p1.setBootstrapOwner(null);
		
		ErraNode s1 = new ErraNode("40.40.40.40", NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_MISSING);
		s1.setInMyCounty(true);
		s1.setBootstrapOwner(p1);
		
		ErraNode s2 = new ErraNode("50.50.50.50", NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_DEAD);
		s2.setInMyCounty(true);
		s2.setBootstrapOwner(p1);
		
		ErraNode s3 = new ErraNode("60.60.60.60", NodeType.NODE_TYPE_SUBJECT, NodeState.UNKNOWN);
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
	
	private class PrinceTable extends JTable {
		
		private Map<Integer, Color> rowColor;

	     public PrinceTable(TableModel model) {
	          super(model);
	          rowColor = new HashMap<Integer, Color>();
	     }

	     @Override
	     public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
	          Component component = super.prepareRenderer(renderer, row, column);
	          if (!isRowSelected(row)) {
	               Color color = rowColor.get(row);
	               component.setBackground(color == null ? getBackground() : color);
	          }
	          return component;
	     }

	     public void setRowColor(int row, Color color) {
	          rowColor.put(row, color);
	     }
	}
	
	private class PrinceMouseAdapter extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			JTable currentTable = (JTable) e.getSource();
			int selRow = currentTable.getSelectedRow();
			currentTable.clearSelection();
			String ipSelected = null;
			for(Map.Entry<String, Integer> entry : ipToRowTable.entrySet()) {
				if (entry.getValue() == selRow) {
					ipSelected = entry.getKey();
					break;
				}
			}
			focusedNode = nodes.get(ipSelected);
			genericInfoPanel.removeAll();
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
	
	/*
	 * 
	 * static class MyTableModel extends DefaultTableModel {

    List<Color> rowColours = Arrays.asList(
        Color.RED,
        Color.GREEN,
        Color.CYAN
    );

    public void setRowColour(int row, Color c) {
        rowColours.set(row, c);
        fireTableRowsUpdated(row, row);
    }

    public Color getRowColour(int row) {
        return rowColours.get(row);
    }

    @Override
    public int getRowCount() {
        return 3;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return String.format("%d %d", row, column);
    }
}


static class MyTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        MyTableModel model = (MyTableModel) table.getModel();
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        c.setBackground(model.getRowColour(row));
        return c;
    }
}


model.setRowColour(1, Color.YELLOW);
	 */
}
