/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.jython;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JTextPane;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import edu.wisc.ssec.mcidasv.jython.Console.Actions;

public abstract class ConsoleAction extends TextAction {
    protected Console console;

    protected ConsoleAction(final Console console, final Actions type) {
        super(type.getId());
        this.console = console;

        JTextPane textPane = console.getTextPane();
        Keymap keyMap = textPane.getKeymap();
        keyMap.addActionForKeyStroke(type.getKeyStroke(), this);
    }

    public abstract void actionPerformed(final ActionEvent e);
}

class EnterAction extends ConsoleAction {
    private static final long serialVersionUID = 6866731355386212866L;

    public EnterAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnter();
    }
}

class DeleteAction extends ConsoleAction {
    private static final long serialVersionUID = 4084595316508126660L;

    public DeleteAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleDelete();
    }
}

class HomeAction extends ConsoleAction {
    private static final long serialVersionUID = 8416339385843554054L;

    public HomeAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleHome();
    }
}

class EndAction extends ConsoleAction {
    private static final long serialVersionUID = 5990936637916440399L;

    public EndAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnd();
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