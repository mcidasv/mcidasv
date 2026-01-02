/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;

public class FormulaSelection extends SelectionAdapter {

    FormulaSelection() {
        super();
    }

    public void applyToDataSelection(DataSelection select) {
    }

    public int applyToDataSelection(DataChoice choice, DataSelection select) {
        return -1;
    }

    public DataChoice getSelectedDataChoice() {
        return null;
    }

    public void fireSelectionEvent(Compute compute, String name) {
        // Don't notify Tools of RGBComposite selection
        if (compute instanceof RGBComposite) {
            return;
        }
        SelectionEvent e = new SelectionEvent(this, compute, name);
        for (int k = 0; k < selectionListeners.size(); k++) {
            selectionListeners.get(k).selectionPerformed(e);
        }
    }

    public Object getLastSelectedLeafPath() {
        return null;
    }

    public Object getLastSelectedComp() {
        return null;
    }

    public void setSelected(Object obj) {
    }
}
