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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.wisc.ssec.hydra.data;


/**
 * @author rink
 */
public class DataChoice {

    private final DataSource dataSource;
    private final String name;
    private final DataGroup group;
    private DataSelection dataSelection;

    public DataChoice(DataSource dataSource, String name, DataGroup group) {
        this.dataSource = dataSource;
        this.name = name;
        this.group = group;
    }

    public DataGroup getGroup() {
        return group;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getName() {
        return name;
    }

    public DataSelection getDataSelection() {
        return dataSelection;
    }

    public void setDataSelection(DataSelection dataSel) {
        this.dataSelection = dataSel;
    }

}
