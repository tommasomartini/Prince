package com.helpful;

import java.awt.*;

import java.awt.event.*;

import javax.swing.*;

import javax.swing.table.*;

class SimpleTableTest extends JFrame

{

	private JPanel topPanel ;

	private JTable table;

	private JScrollPane scrollPane;

	private String[] columnNames= new String[3];

	private String[][] dataValues=new String[3][3] ;

	public SimpleTableTest() {

		setTitle("JTable Cell Not Editable");

		setSize(300,300);

		topPanel= new JPanel();

		topPanel.setLayout(new BorderLayout());

		getContentPane().add(topPanel);

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		columnNames=new String[] {"Column 1" , "Column 2" , "Column 3"};

		dataValues = new String[][]   {

				{"1","2","3"},

				{"4","5","6"},

				{"7","8","9"}

		};

		TableModel model=new myTableModel();

		table =new JTable( );

		table.setRowHeight(50);

		table.setModel(model);

		scrollPane=new JScrollPane(table);

		topPanel.add(scrollPane,BorderLayout.CENTER);    

	}

	public class myTableModel extends DefaultTableModel

	{

		myTableModel( )

		{

			super(dataValues,columnNames);

			System.out.println("Inside myTableModel");

		}

		public boolean isCellEditable(int row,int cols)

		{

			if(cols==0 ){return false;}

			//It will make the cells of Column-1 not Editable

			return true;                                                                                    

		}

	}         

	public static void main(String args[])

	{

		SimpleTableTest mainFrame=new SimpleTableTest();

		mainFrame.setVisible(true);

	}         

}
