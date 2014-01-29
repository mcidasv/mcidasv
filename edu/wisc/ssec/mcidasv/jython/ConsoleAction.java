/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

package edu.wisc.ssec.mcidasv.jython;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Action;
import javax.swing.JTextPane;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import edu.wisc.ssec.mcidasv.jython.Console.Actions;

// TODO(jon): there has to be a less repetitive way...
public abstract class ConsoleAction extends TextAction {
    protected static final Map<JTextPane, Console> mapping = new ConcurrentHashMap<JTextPane, Console>();
    protected Console console;

    protected ConsoleAction(final Console console, final Actions type) {
        super(type.getId());
        this.console = console;

        JTextPane textPane = console.getTextPane();
        Keymap keyMap = textPane.getKeymap();
        keyMap.addActionForKeyStroke(type.getKeyStroke(), this);
        mapping.put(textPane, console);
    }

    /**
     * Attempts to return the console where the {@code ActionEvent} originated.
     * 
     * <p>Note: {@code TextAction}s are <i>weird</i>. The only 
     * somewhat-accurate way to determine where the action occurred is to check
     * {@link ActionEvent#getSource()}. Since that will be a {@code JTextPane},
     * you have to keep an updated mapping of {@code JTextPane}s to {@code Console}s.
     * 
     * @param e The action whose source {@code Console} you want.
     * 
     * @return Either the actual source {@code Console}, or the {@code Console}
     * provided to the constructor.
     * 
     * @see TextAction
     */
    protected Console getSourceConsole(final ActionEvent e) {
        if (console.getTextPane() != e.getSource()) {
            Console other = mapping.get((JTextPane)e.getSource());
            if (other != null) {
                return other;
            }
        }
        return console;
    }

    public abstract void actionPerformed(final ActionEvent e);
}

class PasteAction extends ConsoleAction {
    public PasteAction(final Console console, final Actions type) {
        super(console, type);
    }
    
    public void actionPerformed(final ActionEvent e) {
        getSourceConsole(e).handlePaste();
    }
}

class EnterAction extends ConsoleAction {
    private static final long serialVersionUID = 6866731355386212866L;

    public EnterAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        getSourceConsole(e).handleEnter();
    }
}

class DeleteAction extends ConsoleAction {
    private static final long serialVersionUID = 4084595316508126660L;

    public DeleteAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        getSourceConsole(e).handleDelete();
    }
}

class HomeAction extends ConsoleAction {
    private static final long serialVersionUID = 8416339385843554054L;

    public HomeAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        getSourceConsole(e).handleHome();
    }
}

class EndAction extends ConsoleAction {
    private static final long serialVersionUID = 5990936637916440399L;

    public EndAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        getSourceConsole(e).handleEnd();
    }
}

class TabAction extends ConsoleAction {
    private static final long serialVersionUID = 2303773619117479801L;

    public TabAction(final Console console, final Actions type) {
        super(console, type);
    }
    public void actionPerformed(final ActionEvent e) {
        getSourceConsole(e).handleTab();
    }
}

class UpAction extends ConsoleAction {
    private static final long serialVersionUID = 6710943250074726107L;
    private Action defaultAction;
    
    public UpAction(final Console console, final Actions type) {
        super(console, type);
        defaultAction = console.getTextPane().getActionMap().get(DefaultEditorKit.upAction);
    }

    public void actionPerformed(final ActionEvent e) {
        defaultAction.actionPerformed(e);
    }
}

class DownAction extends ConsoleAction {
    private static final long serialVersionUID = 5700659549452276829L;
    private Action defaultAction;

    public DownAction(final Console console, final Actions type) {
        super(console, type);
        defaultAction = console.getTextPane().getActionMap().get(DefaultEditorKit.downAction);
    }

    public void actionPerformed(final ActionEvent e) {
        defaultAction.actionPerformed(e);
    }
}
