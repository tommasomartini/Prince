package com.prince;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class GUI extends Frame implements WindowListener,ActionListener 
{
        
        Button b;


        public GUI(String title) 
        {

                super(title);
                setLayout(new FlowLayout());
                addWindowListener(this);
                b = new Button("Send file");
                add(b);
                b.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) 
        {

        	String S=e.getActionCommand();
        	try {
        		
        		if (S.toLowerCase().contains("send"))
        		{
        			NewErraClient.send("","");
        		}
        		
        		if (S.toLowerCase().contains("refresh"))
        		{
        			NewErraClient.showTopology();
        		}
        		
        		if (S.toLowerCase().contains("exit"))
        		{
  			
        			NewErraClient.sayGoodbye();
        			
					System.exit(0);
					
        		}
				
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
  
        }

        public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
        }
       

        public void update()
        {
        	
        	String[][] data = new String[NewErraClient.nodes.size()][1];
        	int rowIndex = 0;
        	for(Map.Entry<String, ErraNode> entry : NewErraClient.nodes.entrySet()) 
        	{
        		ErraNode currentNode = entry.getValue();
        		data[rowIndex][0] = currentNode.getIPAddress();
        		
        		
        		rowIndex++;
        	}
        	String[] columnNames = {"IP addresses"};
        	JTable table = new JTable(data, columnNames);
        	table.setPreferredScrollableViewportSize(new Dimension(200, 330));
        	table.setFillsViewportHeight(true);
        	JScrollPane scrollPane = new JScrollPane(table);
        	
        	
        	removeAll();
        	setLayout(new GridLayout(3,1)); // split the panel in 1 rows and 2 cols
        	addWindowListener(this);

        	
        	JPanel bottoni=new JPanel();
        	bottoni.setLayout(new GridLayout(5,1)); // split the panel in 1 rows and 2 cols
        	b = new Button("Send file");
        	b.setBackground(Color.green);
        	b.addActionListener(this);
        	bottoni.add(b);
        	b = new Button("Refresh");
        	b.setBackground(Color.yellow);
        	b.addActionListener(this);
        	bottoni.add(b);
        	b = new Button("Exit");
        	b.addActionListener(this);
        	b.setBackground(Color.red);
        	bottoni.add(b);
        	JLabel L2=new JLabel("You are host "+NewErraClient.getMyIP(),JLabel.CENTER);
        	L2.setBackground(Color.pink);
           	bottoni.add(L2);
           	
        	JLabel L=new JLabel("Available nodes in the network",JLabel.CENTER);
           	bottoni.add(L);
        	
        	add(bottoni);
      	
        	
        	JPanel lista=new JPanel();
        	lista.setLayout(new GridLayout(1,1)); // split the panel in 1 rows and 2 cols
        	lista.add(scrollPane);
        	
        	add(lista);

        	JPanel ric=new JPanel();
        	ric.setLayout(new GridLayout(1,1));
        	String[][] dati = new String[NewErraClient.notifications.size()][1];
        	rowIndex = 0;
        	for (Iterator<String> it = NewErraClient.notifications.iterator(); it.hasNext();)
			{
        		String S = it.next();
        		dati[rowIndex][0] = S;
        		rowIndex++;
        	}
        	String[] names = {"Received and sent files"};
        	JTable table2 = new JTable(dati, names);
        	//table.setPreferredScrollableViewportSize(new Dimension(200, 330));
        	table2.setFillsViewportHeight(true);
        	JScrollPane scrollPane2 = new JScrollPane(table2);
        	
        	ric.add(scrollPane2);
        	add(ric);

        	validate();
        	repaint();
        	doLayout();
        }
        
        
        public void windowOpened(WindowEvent e) {}
        public void windowActivated(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowClosed(WindowEvent e) {}

}