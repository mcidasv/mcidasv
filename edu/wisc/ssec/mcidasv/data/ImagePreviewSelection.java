/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

package edu.wisc.ssec.mcidasv.data;

import java.awt.Image;
import java.rmi.RemoteException;

import javax.swing.JComponent;

import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import visad.VisADException;
import edu.wisc.ssec.mcidasv.ui.JPanelImage;


public class ImagePreviewSelection extends DataSelectionComponent {
	private Image theImage;

	public ImagePreviewSelection(Image previewImage) throws VisADException, RemoteException {
		super("Preview");
		theImage = previewImage;
	}

	protected JComponent doMakeContents() {
		return (new JPanelImage(theImage));
	}

	public void applyToDataSelection(DataSelection dataSelection) {
		return;
	}

}
