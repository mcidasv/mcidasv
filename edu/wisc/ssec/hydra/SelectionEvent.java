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
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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

public class SelectionEvent { //should extend EventObject?
    DataSource dataSource;
    DataChoice choice;
    String name;
    Selection selection;

    Compute compute;
    boolean fromCompute = false;

    public SelectionEvent(Selection selection, DataSource dataSource, DataChoice choice) {
        this(selection, dataSource, choice, null);
    }

    public SelectionEvent(Selection selection, DataSource dataSource, DataChoice choice, String name) {
        this.dataSource = dataSource;
        this.choice = choice;
        this.name = name;
        this.selection = selection;
    }

    public SelectionEvent(Selection selection, Compute compute, String name) {
        this.selection = selection;
        this.compute = compute;
        this.name = name;
        this.fromCompute = true;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DataChoice getDataChoice() {
        return choice;
    }

    public String getName() {
        return name;
    }

    public Selection getSelection() {
        return selection;
    }
}
