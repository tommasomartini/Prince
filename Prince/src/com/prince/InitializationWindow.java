package com.prince;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class InitializationWindow extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private static final String RADIO_BT_OPTION_AUTO = "Auto";
	private static final String RADIO_BT_OPTION_MANUAL = "Manual";
	
	private enum ActionCommand {
		ACTION_COMMAND_SELECTED_AUTO,
		ACTION_COMMAND_SELECTED_MANUAL
	}
	
	// RadioButtons
	private JRadioButton rbtAuto;
	private JRadioButton rbtManual;
	private ButtonGroup radioButtonGroup;
	private JPanel radioPanel;
	
	private JFrame frame;
	
	public InitializationWindow() {
		super(new GridLayout(3, 3));
		
		if (frame != null && frame.isShowing()) {
			frame.setVisible(false);
			frame.dispose();
		}
		this.removeAll();		
		frame = new JFrame("Initialization Prince");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		rbtAuto = new JRadioButton(RADIO_BT_OPTION_AUTO);
        rbtAuto.setMnemonic(KeyEvent.VK_A);
        rbtAuto.setActionCommand(RADIO_BT_OPTION_AUTO);
        rbtAuto.setSelected(true);
        rbtAuto.addActionListener(this);
 
        rbtManual = new JRadioButton(RADIO_BT_OPTION_MANUAL);
        rbtManual.setMnemonic(KeyEvent.VK_M);
        rbtManual.setActionCommand(RADIO_BT_OPTION_MANUAL);
        rbtManual.addActionListener(this);
        
        radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(rbtAuto);
        radioButtonGroup.add(rbtManual);
        
        radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(rbtAuto);
        radioPanel.add(rbtManual);
 
        add(radioPanel);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        this.setOpaque(true); //content panes must be opaque 
        frame.setContentPane(this);
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String args[]) {
		InitializationWindow initializationWindow = new InitializationWindow();
//		initializationWindow.startWin();
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		if (actionEvent.getActionCommand().equalsIgnoreCase(RADIO_BT_OPTION_AUTO)) {
			System.out.println("Scelto auto");
			
		} else if (actionEvent.getActionCommand().equalsIgnoreCase(RADIO_BT_OPTION_MANUAL)) {
			System.out.println("Scelto manual");
		}
	}

}
