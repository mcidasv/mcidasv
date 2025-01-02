/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;

import visad.Data;

public class Operand {
    boolean isEmpty = true;

    DataSource dataSource;
    int dataSourceId;
    DataSelection dataSelection;
    DataChoice dataChoice;
    Selection selection;
    String name;
    String dateTimeStr;
    String id;

    Compute compute;

    public Operand() {
    }

    public Data getData() throws Exception {
        Data data;
        if (compute != null) {
            data = compute.compute();
        } else {
            update();
            data = dataSource.getData(dataChoice, dataSelection);
        }
        return data;
    }

    //TODO: This is needed in case the region selection was changed with the same
    //      same set of components above.  A lot of this  complexity is to maintain
    //      backward compatibility. It uses the saved DataSelection created as the
    //      user selects the input components.
    public void update() {
        selection.applyToDataSelection(dataChoice, dataSelection);
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty() {
        dataSource = null;
        dataSourceId = -1;
        dataSelection = null;
        dataChoice = null;
        selection = null;
        isEmpty = true;
    }

    public void disable() {
    }

    public void enable() {
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return id;
    }

    public Operand clone() {
        Operand clone = new Operand();

        clone.isEmpty = this.isEmpty;
        clone.dataSource = this.dataSource;
        clone.dataSourceId = this.dataSourceId;
        clone.dataSelection = this.dataSelection;
        clone.dataChoice = this.dataChoice;
        clone.selection = this.selection;
        clone.name = this.name;
        clone.id = this.id;
        clone.compute = this.compute;

        return clone;
    }
}
