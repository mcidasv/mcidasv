/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.util.grouptable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class TextColorChooser extends JColorChooser {

    public TextColorChooser(Color target, Color reference, boolean isForgroundSelection) {
        super(target);
        if (isForgroundSelection) {
            setPreviewPanel(new TextPreviewLabel(target, reference,isForgroundSelection));
        } else {
            setPreviewPanel(new TextPreviewLabel(reference,target, isForgroundSelection));
        }
        updateUI();
    }

    public Color showDialog(Component component, String title) {
        ColorChooserDialog dialog = new ColorChooserDialog(component, title, this);
//        dialog.show();
        dialog.setVisible(true);
        Color col = dialog.getColor();
        dialog.dispose();
        return col;
    }
}

class TextPreviewLabel extends JLabel {
    private String sampleText = "  Sample Text  Sample Text  ";
    boolean isForgroundSelection;

    public TextPreviewLabel() {
        this(Color.black, Color.white, true);
    }

    public TextPreviewLabel(Color fore, Color back, boolean isForgroundSelection) {
        setOpaque(true);
        setForeground(fore);
        setBackground(back);
        this.isForgroundSelection = isForgroundSelection;
        setText(sampleText);
    }

    public void setForeground(Color col) {
        if (isForgroundSelection) {
            super.setForeground(col);
        } else {
            super.setBackground(col);
        }
    }
}

class ColorChooserDialog extends JDialog {
    private Color initialColor;
    private Color retColor;
    private JColorChooser chooserPane;

    public ColorChooserDialog(Component c, String title, final JColorChooser chooserPane) {
        super(JOptionPane.getFrameForComponent(c), title, true);
        setResizable(false);

        this.chooserPane = chooserPane;

        String okString = UIManager.getString("ColorChooser.okText");
        String cancelString = UIManager.getString("ColorChooser.cancelText");
        String resetString = UIManager.getString("ColorChooser.resetText");

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(chooserPane, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton(okString);
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                retColor = chooserPane.getColor();
                setVisible(false);
            }
        });
        buttonPane.add(okButton);

        JButton cancelButton = new JButton(cancelString);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                retColor = null;
                setVisible(false);
            }
        });
        buttonPane.add(cancelButton);

        JButton resetButton = new JButton(resetString);
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooserPane.setColor(initialColor);
            }
        });
        buttonPane.add(resetButton);
        contentPane.add(buttonPane, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(c);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    public Color getColor() {
        return retColor;
    }
}

