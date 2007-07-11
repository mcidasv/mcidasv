<?xml version="1.0" encoding="ISO-8859-1"?>
<resourcebundle name="TabbedUI">
    <resources name="idv.resource.skin">
        <resource
           label="Tabs&gt;Map Display&gt;One Pane"
           location="%APPPATH%/skins/tabbed/oneviewskin.xml"/>
        <resource
           label="Tabs&gt;Map Display&gt;Two Panes"
           location="%APPPATH%/skins/tabbed/twoviewskin.xml">
          <property
             name="left_view_class"
             value="ucar.unidata.idv.MapViewManager"/>
          <property
             name="right_view_class"
             value="ucar.unidata.idv.MapViewManager"/>
        </resource>
        <resource
           label="Tabs&gt;Map Display&gt;Three Panes"
           location="%APPPATH%/skins/tabbed/threeviewskin.xml">
          <property
             name="view_class"
             value="ucar.unidata.idv.MapViewManager"/>
        </resource>
        <resource
           label="Tabs&gt;Map Display&gt;Four Panes"
           location="%APPPATH%/skins/tabbed/fourviewskin.xml">
          <property
             name="view_class"
             value="ucar.unidata.idv.MapViewManager"/>
        </resource>
        <resource
           label="Tabs&gt;Globe Display&gt;Two Panes"
           location="%APPPATH%/skins/tabbed/twoglobeskin.xml"/>
        <resource
           label="Tabs&gt;Globe Display&gt;Three Panes"
           location="%APPPATH%/skins/tabbed/threeglobeskin.xml"/>
        <resource
           label="Tabs&gt;Globe Display&gt;Four Panes"
           location="%APPPATH%/skins/tabbed/fourglobeskin.xml"/>
    </resources>
</resourcebundle>
