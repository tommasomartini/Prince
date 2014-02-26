package com.helpful;



import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author Jigar
 */
public class JTableDemo  extends MouseAdapter   {
int selection;


    public static void main(String[] args) throws Exception
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        String[] headers = {"A", "B", "C"};
        Object[][] data = {{1, 2, 3}, {4, 5, 6}};
        JTable table = new JTable(data, headers);
        JScrollPane scroll = new JScrollPane();
        scroll.setViewportView(table);
        frame.add(scroll);
        frame.pack();
        frame.setVisible(true);
        table.addMouseListener(new JTableDemo());
        scroll.addMouseListener(new JTableDemo());
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        JTable jtable = (JTable) e.getSource();
        selection = jtable.getSelectedRow();
        jtable.clearSelection();
        System.out.println("Mouse pressed on " + selection);
    }
    @Override
    public void mouseReleased(MouseEvent e){
        JTable jtable = (JTable) e.getSource();
        System.out.println("Mouse released on " + selection);
    }



}

