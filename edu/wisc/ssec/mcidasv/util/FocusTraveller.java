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

package edu.wisc.ssec.mcidasv.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.Collection;

/**
 * Abstraction of {@link FocusTraversalPolicy} that allows for easy creation of
 * the traversal policy.
 * 
 * <p>Note that the {@literal "delta"} parameter of both 
 * {@link #cycle(Component, int)} and {@link #indexCycle(int, int)} can be any
 * positive or negative integer. Both methods compute indices using the wonders
 * of modular arithmetic.
 */
public class FocusTraveller extends FocusTraversalPolicy {

    /** Components to traverse, stored in the desired traversal order. */
    private final Component[] components;

    /**
     * Creates the {@link FocusTraversalPolicy}.
     * 
     * @param componentsToTraverse Components to traverse, in the desired order.
     * Cannot be {@code null}.
     */
    public FocusTraveller(final Component... componentsToTraverse) {
        components = componentsToTraverse;
    }

    /**
     * Creates the {@link FocusTraversalPolicy} for the given components.
     * 
     * @param componentsToTraverse Components to traverse. Cannot be {@code null}.
     */
    public FocusTraveller(final Collection<Component> componentsToTraverse) {
        components = componentsToTraverse.toArray(new Component[0]);
    }

    /**
     * Cycles through valid index values.
     * 
     * @param index Current index.
     * @param delta Index of next component, relative to {@code index}.
     * 
     * @return Next index value.
     */
    private int indexCycle(final int index, final int delta) {
        int size = components.length;
        return (index + delta + size) % size;
    }

    /**
     * Cycles through components. 
     * 
     * @param currentComponent Cannot be {@code null}.
     * @param delta Index of next component, relative to {@code currentComponent}.
     * 
     * @return The {@literal "next"} component in the traversal order.
     */
    private Component cycle(final Component currentComponent, final int delta) {
        int index = -1;
        loop: for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            for (Component c = currentComponent; c != null; c = c.getParent()) {
                if (component == c) {
                    index = i;
                    break loop;
                }
            }
        }
        
        // try to find enabled component in "delta" direction
        int initialIndex = index;
        while (true) {
            int newIndex = indexCycle(index, delta);
            if (newIndex == initialIndex) {
                break;
            }
            index = newIndex;
            
            Component component = components[newIndex];
            if (component.isEnabled() && component.isVisible() && component.isFocusable()) {
                return component;
            }
        }
        // not found
        return currentComponent;
    }

    /**
     * Gets the next component after {@code component}.
     * 
     * @param container Ignored.
     * @param component Cannot be {@code null}.
     * 
     * @return Next component after {@code component}.
     */
    public Component getComponentAfter(final Container container, final Component component) {
        return cycle(component, 1);
    }

    /**
     * Gets the component before {@code component}.
     * 
     * @param container Ignored.
     * @param component Cannot be {@code null}.
     * 
     * @return The {@link Component} before {@code component} in traversal order.
     */
    public Component getComponentBefore(final Container container, final Component component) {
        return cycle(component, -1);
    }

    /**
     * Gets the first component.
     * 
     * @param container Ignored.
     * 
     * @return The first {@link Component} in traversal order.
     */
    public Component getFirstComponent(final Container container) {
        return components[0];
    }

    /**
     * Gets the last component.
     * 
     * @param container Ignored.
     * 
     * @return The last {@link Component} in traversal order.
     */
    public Component getLastComponent(final Container container) {
        return components[components.length - 1];
    }

    /**
     * Not used. See {@link #getFirstComponent(Container)}.
     * 
     * @param container Ignored.
     * 
     * @return The first {@link Component} in traversal order.
     */
    public Component getDefaultComponent(final Container container) {
        return getFirstComponent(container);
    }
}
