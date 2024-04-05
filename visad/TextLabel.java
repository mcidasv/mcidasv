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

package visad;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.UIManager;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

//import com.eteks.sweethome3d.model.Home;
//import com.eteks.sweethome3d.model.Label;
//import com.eteks.sweethome3d.model.TextStyle;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.image.TextureLoader;

/**
 * Root of a label branch.
 * @author Emmanuel Puybaret
 */
public class TextLabel extends BranchGroup {

    // The coloring attributes used for drawing outline
    protected static final ColoringAttributes OUTLINE_COLORING_ATTRIBUTES =
            new ColoringAttributes(new Color3f(0.16f, 0.16f, 0.16f), ColoringAttributes.FASTEST);
    protected static final PolygonAttributes OUTLINE_POLYGON_ATTRIBUTES =
            new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0);
    protected static final LineAttributes OUTLINE_LINE_ATTRIBUTES =
            new LineAttributes(0.5f, LineAttributes.PATTERN_SOLID, true);

    protected static final Integer  DEFAULT_COLOR         = 0xFFFFFF;
    protected static final Integer  DEFAULT_AMBIENT_COLOR = 0x333333;
    protected static final Material DEFAULT_MATERIAL      = new Material();

    private static final Map<Long, Material>                materials = new HashMap<Long, Material>();
//    private static final Map<TextureKey, TextureAttributes> textureAttributes = new HashMap<TextureKey, TextureAttributes>();
//    private static final Map<Home, Map<Texture, Texture>>   homesTextures = new WeakHashMap<Home, Map<Texture, Texture>>();

    static {
        DEFAULT_MATERIAL.setCapability(Material.ALLOW_COMPONENT_READ);
        DEFAULT_MATERIAL.setShininess(1);
        DEFAULT_MATERIAL.setSpecularColor(0, 0, 0);
    }

    private static final TransparencyAttributes DEFAULT_TRANSPARENCY_ATTRIBUTES =
            new TransparencyAttributes(TransparencyAttributes.NICEST, 0);
    private static final PolygonAttributes      DEFAULT_POLYGON_ATTRIBUTES =
            new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0, false);
    private static final TextureAttributes MODULATE_TEXTURE_ATTRIBUTES = new TextureAttributes();

    static {
        MODULATE_TEXTURE_ATTRIBUTES.setTextureMode(TextureAttributes.MODULATE);
    }

    private String      text;
    //    private TextStyle   style;
    private Integer     color;
    private Transform3D baseLineTransform;
    private Texture     texture;

    //    public Label3D(Label label, Home home, boolean waitForLoading) {
    public TextLabel(TextControl label, boolean waitForLoading) {
        setUserData(label);

        // Allow piece branch to be removed from its parent
        setCapability(BranchGroup.ALLOW_DETACH);
        setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        update();
    }

    public void update() {
        TextControl label = (TextControl) getUserData();
        ShadowType.LabelStuff labelStuff = label.getLabelStuff();
        Float pitch = label.getPitch();
//        TextStyle style = label.getStyle();
        if (pitch != null
//                && style != null
//                && (label.getLevel() == null
//                || label.getLevel().isViewableAndVisible())
        ) {
            String text = labelStuff.text;
            Integer color = labelStuff.textColor;
            Integer outlineColor = null;
            if (!text.equals(this.text)
//                    || (style == null && this.style != null)
//                    || (style != null && !style.equals(this.style))
                    || (color == null && this.color != null)
                    || (color != null && !color.equals(this.color))) {
                // If text, style and color changed, recompute label texture
                int fontStyle = Font.PLAIN;
//                if (style.isBold()) {
//                    fontStyle = Font.BOLD;
//                }
//                if (style.isItalic()) {
//                    fontStyle |= Font.ITALIC;
//                }
                Font defaultFont;
//                if (style.getFontName() != null) {
//                    defaultFont = new Font(style.getFontName(), fontStyle, 1);
//                } else {
//                    defaultFont = UIManager.getFont("TextField.font");
//                }
                if (label.getFont() != null) {
                    defaultFont = label.getFont();
                } else {
                    defaultFont = UIManager.getFont("TextField.font");
                }
//                BasicStroke stroke = new BasicStroke(outlineColor != null ? style.getFontSize() * 0.05f : 0f);
//                Font font = defaultFont.deriveFont(fontStyle, style.getFontSize() - stroke.getLineWidth());
                BasicStroke stroke = new BasicStroke(outlineColor != null ? (float) (label.getSize() * 0.05f) : 0f);
                Font font = defaultFont.deriveFont(fontStyle, (float) (label.getSize() - stroke.getLineWidth()));

                BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2D = (Graphics2D)dummyImage.getGraphics();
                g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                FontMetrics fontMetrics = g2D.getFontMetrics(font);

                String [] lines = text.split("\n");
                float [] lineWidths = new float [lines.length];
                float textWidth = -Float.MAX_VALUE;
                float baseLineShift = 0;
                for (int i = 0; i < lines.length; i++) {
                    Rectangle2D lineBounds = fontMetrics.getStringBounds(lines [i], g2D);
                    if (i == 0) {
                        baseLineShift = -(float)lineBounds.getY() + fontMetrics.getHeight() * (lines.length - 1);
                    }
                    lineWidths [i] = (float)lineBounds.getWidth() + 2 * stroke.getLineWidth();
//                    if (style.isItalic()) {
//                        lineWidths [i] += fontMetrics.getAscent() * 0.2;
//                    }
                    textWidth = Math.max(lineWidths [i], textWidth);
                }
                g2D.dispose();

                float textHeight = (float)fontMetrics.getHeight() * lines.length + 2 * stroke.getLineWidth();
                float textRatio = (float)Math.sqrt((float)textWidth / textHeight);
                int width;
                int height;
                float scale;
                // Ensure that text image size is between 256x256 and 512x512 pixels
                if (textRatio > 1) {
                    width = (int)Math.ceil(Math.max(255 * textRatio, Math.min(textWidth, 511 * textRatio)));
                    scale = (float)(width / textWidth);
                    height = (int)Math.ceil(scale * textHeight);
                } else {
                    height = (int)Math.ceil(Math.max(255 * textRatio, Math.min(textHeight, 511 / textRatio)));
                    scale = (float)(height / textHeight);
                    width = (int)Math.ceil(scale * textWidth);
                }

                if (width > 0 && height > 0) {
                    // Draw text in an image
                    BufferedImage textureImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    g2D = (Graphics2D)textureImage.getGraphics();
                    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                    g2D.setTransform(AffineTransform.getScaleInstance(scale, scale));
                    g2D.translate(0, baseLineShift);
                    for (int i = lines.length - 1; i >= 0; i--) {
                        String line = lines [i];
                        float translationX;
//                        if (style.getAlignment() == TextStyle.Alignment.LEFT) {
//                            translationX = 0;
//                        } else if (style.getAlignment() == TextStyle.Alignment.RIGHT) {
//                            translationX = textWidth - lineWidths [i];
//                        } else { // CENTER
//                            translationX = (textWidth - lineWidths [i]) / 2;
//                        }
                        if (label.getJustification() == TextControl.Justification.LEFT) {
                            translationX = 0;
                        } else if (label.getJustification() == TextControl.Justification.RIGHT) {
                            translationX = textWidth - lineWidths [i];
                        } else { // CENTER
                            translationX = (textWidth - lineWidths [i]) / 2;
                        }
                        translationX += stroke.getLineWidth() / 2;
                        g2D.translate(translationX, 0);
                        if (outlineColor != null) {
                            g2D.setColor(new Color(outlineColor));
                            g2D.setStroke(stroke);
                            if (line.length() > 0) {
                                TextLayout textLayout = new TextLayout(line, font, g2D.getFontRenderContext());
                                g2D.draw(textLayout.getOutline(null));
                            }
                        }
                        g2D.setFont(font);
                        g2D.setColor(color != null ?  new Color(color) : UIManager.getColor("TextField.foreground"));
                        g2D.drawString(line, 0f, 0f);
                        g2D.translate(-translationX, -fontMetrics.getHeight());
                    }
                    g2D.dispose();

                    Transform3D scaleTransform = new Transform3D();
                    scaleTransform.setScale(new Vector3d(textWidth, 1, textHeight));
                    // Move to the middle of base line
                    this.baseLineTransform = new Transform3D();
                    float translationX;
//                    if (style.getAlignment() == TextStyle.Alignment.LEFT) {
//                        translationX = textWidth / 2;
//                    } else if (style.getAlignment() == TextStyle.Alignment.RIGHT) {
//                        translationX = -textWidth / 2;
//                    } else { // CENTER
//                        translationX = 0;
//                    }
                    if (label.getJustification() == TextControl.Justification.LEFT) {
                        translationX = textWidth / 2;
                    } else if (label.getJustification() == TextControl.Justification.RIGHT) {
                        translationX = -textWidth / 2;
                    } else { // CENTER
                        translationX = 0;
                    }
                    this.baseLineTransform.setTranslation(new Vector3d(translationX, 0, textHeight / 2 - baseLineShift));
                    this.baseLineTransform.mul(scaleTransform);
                    this.texture = new TextureLoader(textureImage, TextureLoader.ALLOW_NON_POWER_OF_TWO).getTexture();
                    this.text = text;
//                    this.style = style;
                    this.color = color;
                } else {
                    clear();
                }
            }

            if (this.texture != null) {
                if (numChildren() == 0) {
                    BranchGroup group = new BranchGroup();
                    group.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                    group.setCapability(BranchGroup.ALLOW_DETACH);

                    TransformGroup transformGroup = new TransformGroup();
                    // Allow the change of the transformation that sets label size, position and orientation
                    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                    transformGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                    group.addChild(transformGroup);

                    Appearance appearance = new Appearance();
                    appearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, 0));
                    appearance.setPolygonAttributes(DEFAULT_POLYGON_ATTRIBUTES);
//                    appearance.setTextureAttributes(MODULATE_TEXTURE_ATTRIBUTES);
                    appearance.setTextureAttributes(getTextureAttributes(false));
                    appearance.setTransparencyAttributes(DEFAULT_TRANSPARENCY_ATTRIBUTES);
                    appearance.setTexCoordGeneration(new TexCoordGeneration(TexCoordGeneration.OBJECT_LINEAR,
                            TexCoordGeneration.TEXTURE_COORDINATE_2, new Vector4f(1, 0, 0, .5f), new Vector4f(0, 1, -1, .5f)));
                    appearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);

                    // Do not share box geometry or cleaning up the universe after an offscreen rendering may cause some bugs
                    Box box = new Box(0.5f, 0f, 0.5f, Box.GEOMETRY_NOT_SHARED | Box.GENERATE_NORMALS, appearance);
                    Shape3D shape = box.getShape(Box.TOP);
                    box.removeChild(shape);
                    transformGroup.addChild(shape);

                    addChild(group);
                }

                TransformGroup transformGroup = (TransformGroup)(((Group)getChild(0)).getChild(0));
                // Apply pitch rotation
                Transform3D pitchRotation = new Transform3D();
                pitchRotation.rotX(pitch);
                pitchRotation.mul(this.baseLineTransform);
                // Apply rotation around vertical axis
                Transform3D rotationY = new Transform3D();
                rotationY.rotY(-label.getAngle());
                rotationY.mul(pitchRotation);
                Transform3D transform = new Transform3D();
                transform.setTranslation(new Vector3d(labelStuff.xoff, label.getGroundElevation() + (pitch == 0f && label.getElevation() < 0.1f ? 0.1f : 0), labelStuff.yoff));
                transform.mul(rotationY);
                transformGroup.setTransform(transform);
                ((Shape3D)transformGroup.getChild(0)).getAppearance().setTexture(this.texture);
            }
        } else {
            clear();
        }
    }

    /**
     * Removes children and clear fields.
     */
    private void clear() {
        removeAllChildren();
        this.text  = null;
//        this.style = null;
        this.color = null;
        this.texture = null;
        this.baseLineTransform = null;
    }

    /**
     * Returns a cloned instance of texture shared per <code>home</code> or
     * the texture itself if <code>home</code> is <code>null</code>.
     * As sharing textures across universes might cause some problems,
     * it's safer to handle a copy of textures for a given home.
     */
//    protected Texture getHomeTextureClone(Texture texture, Home home) {
//        if (home == null || texture == null) {
//            return texture;
//        } else {
//            Map<Texture, Texture> homeTextures = homesTextures.get(home);
//            if (homeTextures == null) {
//                homeTextures = new WeakHashMap<Texture, Texture>();
//                homesTextures.put(home, homeTextures);
//            }
//            Texture clonedTexture = homeTextures.get(texture);
//            if (clonedTexture == null) {
//                clonedTexture = (Texture)texture.cloneNodeComponent(false);
//                homeTextures.put(texture, clonedTexture);
//            }
//            return clonedTexture;
//        }
//    }

    /**
     * Returns the closed shape matching the coordinates in <code>points</code> array.
     */
    protected Shape getShape(float [][] points) {
//        return getShape(points, true, null);
        GeneralPath path = new GeneralPath();
        path.moveTo(points [0][0], points [0][1]);
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points [i][0], points [i][1]);
        }
//        if (closedPath) {
        path.closePath();
//        }
//        if (transform != null) {
//            path.transform(transform);
//        }
        return path;
    }

    /**
     * Returns a shared material instance matching the given color.
     */
    protected Material getMaterial(Integer diffuseColor, Integer ambientColor, float shininess) {
        if (diffuseColor != null) {
            Long materialKey = new Long(diffuseColor + (ambientColor << 24) + ((char)(shininess * 128) << 48));
            Material material = materials.get(materialKey);
            if (material == null) {
                Color3f ambientMaterialColor = new Color3f(((ambientColor >>> 16) & 0xFF) / 255f,
                        ((ambientColor >>> 8) & 0xFF) / 255f,
                        (ambientColor & 0xFF) / 255f);
                Color3f diffuseMaterialColor = new Color3f(((diffuseColor >>> 16) & 0xFF) / 255f,
                        ((diffuseColor >>> 8) & 0xFF) / 255f,
                        (diffuseColor & 0xFF) / 255f);
                material = new Material(ambientMaterialColor, new Color3f(), diffuseMaterialColor,
                        new Color3f(shininess, shininess, shininess), Math.max(1, shininess * 128));
                material.setCapability(Material.ALLOW_COMPONENT_READ);
                // Store created materials in cache
                materials.put(materialKey, material);
            }
            return material;
        } else {
            return getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, shininess);
        }
    }

    /**
     * Returns shared texture attributes matching transformation applied to the given texture.
     */
//    protected TextureAttributes getTextureAttributes(HomeTexture texture) {
//        return getTextureAttributes(texture, false);
//    }

    private static float getUIScaleProperty(float defaultValue) {
        return Float.valueOf(System.getProperty("sun.java2d.uiScale", String.valueOf(defaultValue)));
    }

    /**
     * Returns shared texture attributes matching transformation applied to the given texture
     * and scaled if required.
     */
//    protected TextureAttributes getTextureAttributes(HomeTexture texture, boolean scaled) {
    protected TextureAttributes getTextureAttributes(boolean scaled) {
//        float textureWidth = texture.getWidth();
//        float textureHeight = texture.getHeight();
        float textureWidth = -1;
        float textureHeight = -1;
        if (textureWidth == -1 || textureHeight == -1) {
            // Set a default value of 1m for textures with width and height equal to -1
            // (this may happen for textures retrieved from 3D models)
            textureWidth = 100;
            textureHeight = 100;
        }
//        float textureXOffset = texture.getXOffset();
//        float textureYOffset = texture.getYOffset();
        float textureXOffset = -100;
        float textureYOffset = -100;
//        float textureAngle = texture.getAngle();
        float textureAngle = 0.0f;
        float textureScale = 1 / getUIScaleProperty(1.0f);
//        TextureKey key = scaled
//                ? new TextureKey(textureWidth, textureHeight, textureXOffset, textureYOffset, textureAngle, textureScale)
//                : new TextureKey(-1f, -1f, textureXOffset, textureYOffset, textureAngle, textureScale);
//        TextureAttributes textureAttributes = Object3DBranch.textureAttributes.get(key);
//        TextureAttributes textureAttributes = this.textureAttributes.get(key);
        TextureAttributes textureAttributes = null;
        if (textureAttributes == null) {
            textureAttributes = new TextureAttributes();
            // Mix texture and color
            textureAttributes.setTextureMode(TextureAttributes.MODULATE);
            Transform3D rotation = new Transform3D();
            rotation.rotZ(textureAngle);
            Transform3D translation = new Transform3D();
            Transform3D transform = new Transform3D();
            // Change scale if required
            if (scaled) {
                translation.setTranslation(new Vector3f(-textureXOffset / textureScale * textureWidth, -textureYOffset / textureScale * textureHeight, 0));
                transform.setScale(new Vector3d(textureScale / textureWidth, textureScale / textureHeight, textureScale));
            } else {
                translation.setTranslation(new Vector3f(-textureXOffset / textureScale, -textureYOffset / textureScale, 0));
                transform.setScale(textureScale);
            }
            rotation.mul(translation);
            transform.mul(rotation);
            textureAttributes.setTextureTransform(transform);
            textureAttributes.setCapability(TextureAttributes.ALLOW_TRANSFORM_READ);
//            this.textureAttributes.put(key, textureAttributes);
        }
        return textureAttributes;
    }

//    /**
//     * Returns texture attributes with a transformation scaled to fit the surface matching <code>areaPoints</code>.
//     */
//    protected TextureAttributes getTextureAttributesFittingArea(HomeTexture texture, float [][] areaPoints, boolean invertY) {
//        float minX = Float.POSITIVE_INFINITY;
//        float minY = Float.POSITIVE_INFINITY;
//        float maxX = Float.NEGATIVE_INFINITY;
//        float maxY = Float.NEGATIVE_INFINITY;
//        for (int i = 0; i < areaPoints.length; i++) {
//            minX = Math.min(minX, areaPoints [i][0]);
//            minY = Math.min(minY, areaPoints [i][1]);
//            maxX = Math.max(maxX, areaPoints [i][0]);
//            maxY = Math.max(maxY, areaPoints [i][1]);
//        }
//        if (maxX - minX <= 0 || maxY - minY <= 0) {
//            return getTextureAttributes(texture, true);
//        }
//
//        TextureAttributes textureAttributes = new TextureAttributes();
//        textureAttributes.setTextureMode(TextureAttributes.MODULATE);
//        Transform3D translation = new Transform3D();
//        translation.setTranslation(new Vector3f(-minX, invertY ? minY : -minY, 0));
//        Transform3D transform = new Transform3D();
//        transform.setScale(new Vector3d(1 / (maxX - minX),  1 / (maxY - minY), 1));
//        transform.mul(translation);
//        textureAttributes.setTextureTransform(transform);
//        textureAttributes.setCapability(TextureAttributes.ALLOW_TRANSFORM_READ);
//        return textureAttributes;
//    }

    /**
     * Returns the list of polygons points matching the given <code>area</code>.
     */
    protected List<float [][]> getAreaPoints(Area area,
                                             float flatness,
                                             boolean reversed) {
        return getAreaPoints(area, null, null, flatness, reversed);
    }

    /**
     * Returns the list of polygons points matching the given <code>area</code> with detailed information in
     * <code>areaPoints</code> and <code>areaHoles</code>.
     */
    protected List<float [][]> getAreaPoints(Area area,
                                             List<float [][]> areaPoints,
                                             List<float [][]> areaHoles,
                                             float flatness,
                                             boolean reversed) {
        List<List<float []>> areaPointsLists = new LinkedList<List<float[]>>();
        List<List<float []>> areaHolesLists = new LinkedList<List<float[]>>();
        ArrayList<float []>  currentPathPoints = null;
        float [] previousPoint = null;
        for (PathIterator it = area.getPathIterator(null, flatness); !it.isDone(); it.next()) {
            float [] point = new float [2];
            switch (it.currentSegment(point)) {
                case PathIterator.SEG_MOVETO :
                    currentPathPoints = new ArrayList<float[]>();
                    currentPathPoints.add(point);
                    previousPoint = point;
                    break;
                case PathIterator.SEG_LINETO :
                    if (point [0] != previousPoint [0]
                            || point [1] != previousPoint [1]) {
                        currentPathPoints.add(point);
                    }
                    previousPoint = point;
                    break;
                case PathIterator.SEG_CLOSE:
                    float [] firstPoint = currentPathPoints.get(0);
                    if (firstPoint [0] == previousPoint [0]
                            && firstPoint [1] == previousPoint [1]) {
                        currentPathPoints.remove(currentPathPoints.size() - 1);
                    }
                    if (currentPathPoints.size() > 2) {
                        float [][] areaPartPoints = currentPathPoints.toArray(new float [currentPathPoints.size()][]);
                        Room subRoom = new Room(areaPartPoints);
                        if (subRoom.getArea() > 0) {
                            boolean pathPointsClockwise = subRoom.isClockwise();
                            if (pathPointsClockwise) {
                                // Keep holes points to remove them from the area once all points are retrieved
                                areaHolesLists.add(currentPathPoints);
                            } else {
                                areaPointsLists.add(currentPathPoints);
                            }

                            if (areaPoints != null || areaHoles != null) {
                                // Store path points in returned lists
                                if (pathPointsClockwise ^ reversed) {
                                    currentPathPoints = (ArrayList<float []>)currentPathPoints.clone();
                                    Collections.reverse(currentPathPoints);
                                    currentPathPoints.toArray(areaPartPoints);
                                }
                                if (pathPointsClockwise) {
                                    if (areaHoles != null) {
                                        areaHoles.add(areaPartPoints);
                                    }
                                } else {
                                    if (areaPoints != null) {
                                        areaPoints.add(areaPartPoints);
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }

        List<float [][]> areaPointsWithoutHoles = new ArrayList<float[][]>();
        if (areaHolesLists.isEmpty() && areaPoints != null) {
            areaPointsWithoutHoles.addAll(areaPoints);
        } else if (areaPointsLists.isEmpty() && !areaHolesLists.isEmpty()) {
            if (areaHoles != null) {
                areaHoles.clear();
            }
        } else {
            // Sort areas from larger areas to smaller ones included in larger ones
            List<List<float []>> sortedAreaPoints;
            Map<List<float []>, Area> subAreas = new HashMap<List<float []>, Area>(areaPointsLists.size());
            if (areaPointsLists.size() > 1) {
                sortedAreaPoints = new ArrayList<List<float[]>>(areaPointsLists.size());
                for (int i = 0; !areaPointsLists.isEmpty(); ) {
                    List<float []> testedArea = areaPointsLists.get(i);
                    int j = 0;
                    for ( ; j < areaPointsLists.size(); j++) {
                        if (i != j) {
                            List<float []> testedAreaPoints = areaPointsLists.get(j);
                            Area subArea = subAreas.get(testedAreaPoints);
                            if (subArea == null) {
                                subArea = new Area(getShape(testedAreaPoints.toArray(new float [testedAreaPoints.size()][])));
                                // Store computed area for future reuse
                                subAreas.put(testedAreaPoints, subArea);
                            }
                            if (subArea.contains(testedArea.get(0) [0], testedArea.get(0) [1])) {
                                break;
                            }
                        }
                    }
                    if (j == areaPointsLists.size()) {
                        areaPointsLists.remove(i);
                        sortedAreaPoints.add(testedArea);
                        i = 0;
                    } else if (i < areaPointsLists.size()) {
                        i++;
                    } else {
                        i = 0;
                    }
                }
            } else {
                sortedAreaPoints = areaPointsLists;
            }
            for (int i = sortedAreaPoints.size() - 1; i >= 0; i--) {
                List<float []> enclosingAreaPartPoints = sortedAreaPoints.get(i);
                Area subArea = subAreas.get(enclosingAreaPartPoints);
                if (subArea == null) {
                    subArea = new Area(getShape(enclosingAreaPartPoints.toArray(new float [enclosingAreaPartPoints.size()][])));
                    // No need to store computed area because it won't be reused
                }
                List<List<float []>> holesInArea = new ArrayList<List<float []>>();
                // Search the holes contained in the current area part
                for (List<float []> holePoints : areaHolesLists) {
                    if (subArea.contains(holePoints.get(0) [0], holePoints.get(0) [1])) {
                        holesInArea.add(holePoints);
                    }
                }

                float [] lastEnclosingAreaPointJoiningHoles = null;
                while (!holesInArea.isEmpty()) {
                    // Search the closest points in the enclosing area and the holes
                    float minDistance = Float.MAX_VALUE;
                    int closestHolePointsIndex = 0;
                    int closestPointIndex = 0;
                    int areaClosestPointIndex = 0;
                    for (int j = 0; j < holesInArea.size() && minDistance > 0; j++) {
                        List<float []> holePoints = holesInArea.get(j);
                        for (int k = 0; k < holePoints.size() && minDistance > 0; k++) {
                            for (int l = 0; l < enclosingAreaPartPoints.size() && minDistance > 0; l++) {
                                float [] enclosingAreaPartPoint = enclosingAreaPartPoints.get(l);
                                float distance = (float) Point2D.distanceSq(holePoints.get(k) [0], holePoints.get(k) [1],
                                        enclosingAreaPartPoint [0], enclosingAreaPartPoint [1]);
                                if (distance < minDistance
                                        && lastEnclosingAreaPointJoiningHoles != enclosingAreaPartPoint) {
                                    minDistance = distance;
                                    closestHolePointsIndex = j;
                                    closestPointIndex = k;
                                    areaClosestPointIndex = l;
                                }
                            }
                        }
                    }
                    // Combine the areas at their closest points
                    List<float []> closestHolePoints = holesInArea.get(closestHolePointsIndex);
                    if (minDistance != 0) {
                        // Store the point joining enclosing area to the current hole to avoid reusing it for next hole
                        lastEnclosingAreaPointJoiningHoles = enclosingAreaPartPoints.get(areaClosestPointIndex);
                        enclosingAreaPartPoints.add(areaClosestPointIndex, lastEnclosingAreaPointJoiningHoles);
                        enclosingAreaPartPoints.add(++areaClosestPointIndex, closestHolePoints.get(closestPointIndex));
                    }
                    List<float []> lastPartPoints = closestHolePoints.subList(closestPointIndex, closestHolePoints.size());
                    enclosingAreaPartPoints.addAll(areaClosestPointIndex, lastPartPoints);
                    enclosingAreaPartPoints.addAll(areaClosestPointIndex + lastPartPoints.size(), closestHolePoints.subList(0, closestPointIndex));

                    holesInArea.remove(closestHolePointsIndex);
                    areaHolesLists.remove(closestHolePoints);
                }
            }

            for (List<float []> pathPoints : sortedAreaPoints) {
                if (reversed) {
                    Collections.reverse(pathPoints);
                }
                areaPointsWithoutHoles.add(pathPoints.toArray(new float [pathPoints.size()][]));
            }
        }

        return areaPointsWithoutHoles;
    }

    protected static String createId(String prefix) {
        return prefix + '-' + UUID.randomUUID();
    }


    public static class Room {

        /**
         * The properties of a room that may change. <code>PropertyChangeListener</code>s added
         * to a room will be notified under a property name equal to the string value of one these properties.
         */
        public enum Property {NAME, NAME_X_OFFSET, NAME_Y_OFFSET, NAME_STYLE, NAME_ANGLE,
            POINTS, AREA_VISIBLE, AREA_X_OFFSET, AREA_Y_OFFSET, AREA_STYLE, AREA_ANGLE,
            FLOOR_COLOR, FLOOR_TEXTURE, FLOOR_VISIBLE, FLOOR_SHININESS,
            CEILING_COLOR, CEILING_TEXTURE, CEILING_VISIBLE, CEILING_SHININESS, CEILING_FLAT, LEVEL}

        private static final long serialVersionUID = 1L;

        private static final double TWICE_PI = 2 * Math.PI;

        private String              name;
        private float               nameXOffset;
        private float               nameYOffset;
        private TextStyle           nameStyle;
        private float               nameAngle;
        private float [][]          points;
        private boolean             areaVisible;
        private float               areaXOffset;
        private float               areaYOffset;
        private TextStyle areaStyle;
        private float               areaAngle;
        private boolean             floorVisible;
        private Integer             floorColor;
        //        private HomeTexture         floorTexture;
        private float               floorShininess;
        private boolean             ceilingVisible;
        private Integer             ceilingColor;
        //        private HomeTexture         ceilingTexture;
        private float               ceilingShininess;
        private boolean             ceilingFlat;
//        private Level               level;

        private transient Shape       shapeCache;
        private transient Rectangle2D boundsCache;
        private transient Float       areaCache;

        /**
         * Creates a room from its name and the given coordinates.
         */
        public Room(float [][] points) {
            this(createId("room"), points);
        }

        /**
         * Creates a room from its name and the given coordinates.
         */
        public Room(String id, float [][] points) {
//            super(id);
            if (points.length <= 1) {
                throw new IllegalStateException("Room points must containt at least two points");
            }
            this.points = deepCopy(points);
            this.areaVisible = true;
            this.nameYOffset = -40f;
            this.floorVisible = true;
            this.ceilingVisible = true;
            this.ceilingFlat = true;
        }

//        /**
//         * Initializes new room fields to their default values
//         * and reads room from <code>in</code> stream with default reading method.
//         */
//        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//            this.ceilingFlat = false;
//            in.defaultReadObject();
//        }

        private transient PropertyChangeSupport propertyChangeSupport;

        /**
         * Fires a property change of PropertyChangeEvent class to listeners.
         * @since 6.4
         */
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            if (this.propertyChangeSupport != null) {
                this.propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
            }
        }


        /**
         * Returns the name of this room.
         */
        public String getName() {
            return this.name;
        }

        /**
         * Sets the name of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         */
        public void setName(String name) {
            if (name != this.name
                    && (name == null || !name.equals(this.name))) {
                String oldName = this.name;
                this.name = name;
                firePropertyChange(Property.NAME.name(), oldName, name);
            }
        }

        /**
         * Returns the distance along x axis applied to room center abscissa
         * to display room name.
         */
        public float getNameXOffset() {
            return this.nameXOffset;
        }

        /**
         * Sets the distance along x axis applied to room center abscissa to display room name.
         * Once this room  is updated, listeners added to this room will receive a change notification.
         */
        public void setNameXOffset(float nameXOffset) {
            if (nameXOffset != this.nameXOffset) {
                float oldNameXOffset = this.nameXOffset;
                this.nameXOffset = nameXOffset;
                firePropertyChange(Property.NAME_X_OFFSET.name(), oldNameXOffset, nameXOffset);
            }
        }

        /**
         * Returns the distance along y axis applied to room center ordinate
         * to display room name.
         */
        public float getNameYOffset() {
            return this.nameYOffset;
        }

        /**
         * Sets the distance along y axis applied to room center ordinate to display room name.
         * Once this room is updated, listeners added to this room will receive a change notification.
         */
        public void setNameYOffset(float nameYOffset) {
            if (nameYOffset != this.nameYOffset) {
                float oldNameYOffset = this.nameYOffset;
                this.nameYOffset = nameYOffset;
                firePropertyChange(Property.NAME_Y_OFFSET.name(), oldNameYOffset, nameYOffset);
            }
        }

        /**
         * Returns the text style used to display room name.
         */
        public TextStyle getNameStyle() {
            return this.nameStyle;
        }

        /**
         * Sets the text style used to display room name.
         * Once this room is updated, listeners added to this room will receive a change notification.
         */
        public void setNameStyle(TextStyle nameStyle) {
            if (nameStyle != this.nameStyle) {
                TextStyle oldNameStyle = this.nameStyle;
                this.nameStyle = nameStyle;
                firePropertyChange(Property.NAME_STYLE.name(), oldNameStyle, nameStyle);
            }
        }

        /**
         * Returns the angle in radians used to display the room name.
         * @since 3.6
         */
        public float getNameAngle() {
            return this.nameAngle;
        }

        /**
         * Sets the angle in radians used to display the room name. Once this piece is updated,
         * listeners added to this piece will receive a change notification.
         * @since 3.6
         */
        public void setNameAngle(float nameAngle) {
            // Ensure angle is always positive and between 0 and 2 PI
            nameAngle = (float)((nameAngle % TWICE_PI + TWICE_PI) % TWICE_PI);
            if (nameAngle != this.nameAngle) {
                float oldNameAngle = this.nameAngle;
                this.nameAngle = nameAngle;
                firePropertyChange(Property.NAME_ANGLE.name(), oldNameAngle, nameAngle);
            }
        }

        /**
         * Returns the points of the polygon matching this room.
         * @return an array of the (x,y) coordinates of the room points.
         */
        public float [][] getPoints() {
            return deepCopy(this.points);
        }

        /**
         * Returns the number of points of the polygon matching this room.
         * @since 2.0
         */
        public int getPointCount() {
            return this.points.length;
        }

        private float [][] deepCopy(float [][] points) {
            float [][] pointsCopy = new float [points.length][];
            for (int i = 0; i < points.length; i++) {
                pointsCopy [i] = points [i].clone();
            }
            return pointsCopy;
        }

        /**
         * Sets the points of the polygon matching this room. Once this room
         * is updated, listeners added to this room will receive a change notification.
         */
        public void setPoints(float [][] points) {
            if (!Arrays.deepEquals(this.points, points)) {
                updatePoints(points);
            }
        }

        /**
         * Update the points of the polygon matching this room.
         */
        private void updatePoints(float [][] points) {
            float [][] oldPoints = this.points;
            this.points = deepCopy(points);
            this.shapeCache = null;
            this.boundsCache = null;
            this.areaCache  = null;
            firePropertyChange(Property.POINTS.name(), oldPoints, points);
        }

        /**
         * Adds a point at the end of room points.
         * @since 2.0
         */
        public void addPoint(float x, float y) {
            addPoint(x, y, this.points.length);
        }

        /**
         * Adds a point at the given <code>index</code>.
         * @throws IndexOutOfBoundsException if <code>index</code> is negative or > <code>getPointCount()</code>
         * @since 2.0
         */
        public void addPoint(float x, float y, int index) {
            if (index < 0 || index > this.points.length) {
                throw new IndexOutOfBoundsException("Invalid index " + index);
            }

            float [][] newPoints = new float [this.points.length + 1][];
            System.arraycopy(this.points, 0, newPoints, 0, index);
            newPoints [index] = new float [] {x, y};
            System.arraycopy(this.points, index, newPoints, index + 1, this.points.length - index);

            float [][] oldPoints = this.points;
            this.points = newPoints;
            this.shapeCache = null;
            this.boundsCache = null;
            this.areaCache  = null;
            firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
        }

        /**
         * Sets the point at the given <code>index</code>.
         * @throws IndexOutOfBoundsException if <code>index</code> is negative or >= <code>getPointCount()</code>
         * @since 2.0
         */
        public void setPoint(float x, float y, int index) {
            if (index < 0 || index >= this.points.length) {
                throw new IndexOutOfBoundsException("Invalid index " + index);
            }
            if (this.points [index][0] != x
                    || this.points [index][1] != y) {
                float [][] oldPoints = this.points;
                this.points = deepCopy(this.points);
                this.points [index][0] = x;
                this.points [index][1] = y;
                this.shapeCache = null;
                this.boundsCache = null;
                this.areaCache  = null;
                firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
            }
        }

        /**
         * Removes the point at the given <code>index</code>.
         * @throws IndexOutOfBoundsException if <code>index</code> is negative or >= <code>getPointCount()</code>
         * @since 2.0
         */
        public void removePoint(int index) {
            if (index < 0 || index >= this.points.length) {
                throw new IndexOutOfBoundsException("Invalid index " + index);
            } else if (this.points.length <= 1) {
                throw new IllegalStateException("Room points must containt at least one point");
            }

            float [][] newPoints = new float [this.points.length - 1][];
            System.arraycopy(this.points, 0, newPoints, 0, index);
            System.arraycopy(this.points, index + 1, newPoints, index, this.points.length - index - 1);

            float [][] oldPoints = this.points;
            this.points = newPoints;
            this.shapeCache = null;
            this.boundsCache = null;
            this.areaCache  = null;
            firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
        }

        /**
         * Returns the minimum coordinates of the rectangle bounding this room.
         * @since 7.0
         */
        public float[] getBoundsMinimumCoordinates() {
            if (this.boundsCache == null) {
                this.boundsCache = getShape().getBounds2D();
            }
            return new float [] {(float)this.boundsCache.getMinX(), (float)this.boundsCache.getMinY()};
        }

        /**
         * Returns the maximum coordinates of the rectangle bounding this room.
         * @since 7.0
         */
        public float[] getBoundsMaximumCoordinates() {
            if (this.boundsCache == null) {
                this.boundsCache = getShape().getBounds2D();
            }
            return new float [] {(float)this.boundsCache.getMaxX(), (float)this.boundsCache.getMaxY()};
        }

        /**
         * Returns whether the area of this room is visible or not.
         */
        public boolean isAreaVisible() {
            return this.areaVisible;
        }

        /**
         * Sets whether the area of this room is visible or not. Once this room
         * is updated, listeners added to this room will receive a change notification.
         */
        public void setAreaVisible(boolean areaVisible) {
            if (areaVisible != this.areaVisible) {
                this.areaVisible = areaVisible;
                firePropertyChange(Property.AREA_VISIBLE.name(), !areaVisible, areaVisible);
            }
        }

        /**
         * Returns the distance along x axis applied to room center abscissa
         * to display room area.
         */
        public float getAreaXOffset() {
            return this.areaXOffset;
        }

        /**
         * Sets the distance along x axis applied to room center abscissa to display room area.
         * Once this room  is updated, listeners added to this room will receive a change notification.
         */
        public void setAreaXOffset(float areaXOffset) {
            if (areaXOffset != this.areaXOffset) {
                float oldAreaXOffset = this.areaXOffset;
                this.areaXOffset = areaXOffset;
                firePropertyChange(Property.AREA_X_OFFSET.name(), oldAreaXOffset, areaXOffset);
            }
        }

        /**
         * Returns the distance along y axis applied to room center ordinate
         * to display room area.
         */
        public float getAreaYOffset() {
            return this.areaYOffset;
        }

        /**
         * Sets the distance along y axis applied to room center ordinate to display room area.
         * Once this room is updated, listeners added to this room will receive a change notification.
         */
        public void setAreaYOffset(float areaYOffset) {
            if (areaYOffset != this.areaYOffset) {
                float oldAreaYOffset = this.areaYOffset;
                this.areaYOffset = areaYOffset;
                firePropertyChange(Property.AREA_Y_OFFSET.name(), oldAreaYOffset, areaYOffset);
            }
        }

        /**
         * Returns the text style used to display room area.
         */
        public TextStyle getAreaStyle() {
            return this.areaStyle;
        }

        /**
         * Sets the text style used to display room area.
         * Once this room is updated, listeners added to this room will receive a change notification.
         */
        public void setAreaStyle(TextStyle areaStyle) {
            if (areaStyle != this.areaStyle) {
                TextStyle oldAreaStyle = this.areaStyle;
                this.areaStyle = areaStyle;
                firePropertyChange(Property.AREA_STYLE.name(), oldAreaStyle, areaStyle);
            }
        }

        /**
         * Returns the angle in radians used to display the room area.
         * @since 3.6
         */
        public float getAreaAngle() {
            return this.areaAngle;
        }

        /**
         * Sets the angle in radians used to display the room area. Once this piece is updated,
         * listeners added to this piece will receive a change notification.
         * @since 3.6
         */
        public void setAreaAngle(float areaAngle) {
            // Ensure angle is always positive and between 0 and 2 PI
            areaAngle = (float)((areaAngle % TWICE_PI + TWICE_PI) % TWICE_PI);
            if (areaAngle != this.areaAngle) {
                float oldAreaAngle = this.areaAngle;
                this.areaAngle = areaAngle;
                firePropertyChange(Property.AREA_ANGLE.name(), oldAreaAngle, areaAngle);
            }
        }

        /**
         * Returns the abscissa of the center point of this room.
         */
        public float getXCenter() {
            float xMin = this.points [0][0];
            float xMax = this.points [0][0];
            for (int i = 1; i < this.points.length; i++) {
                xMin = Math.min(xMin, this.points [i][0]);
                xMax = Math.max(xMax, this.points [i][0]);
            }
            return (xMin + xMax) / 2;
        }

        /**
         * Returns the ordinate of the center point of this room.
         */
        public float getYCenter() {
            float yMin = this.points [0][1];
            float yMax = this.points [0][1];
            for (int i = 1; i < this.points.length; i++) {
                yMin = Math.min(yMin, this.points [i][1]);
                yMax = Math.max(yMax, this.points [i][1]);
            }
            return (yMin + yMax) / 2;
        }

        /**
         * Returns the floor color of this room.
         */
        public Integer getFloorColor() {
            return this.floorColor;
        }

        /**
         * Sets the floor color of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         */
        public void setFloorColor(Integer floorColor) {
            if (floorColor != this.floorColor
                    && (floorColor == null || !floorColor.equals(this.floorColor))) {
                Integer oldFloorColor = this.floorColor;
                this.floorColor = floorColor;
                firePropertyChange(Property.FLOOR_COLOR.name(), oldFloorColor, floorColor);
            }
        }

        /**
         * Returns the floor texture of this room.
         */
//        public HomeTexture getFloorTexture() {
//            return this.floorTexture;
//        }

        /**
         * Sets the floor texture of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         */
//        public void setFloorTexture(HomeTexture floorTexture) {
//            if (floorTexture != this.floorTexture
//                    && (floorTexture == null || !floorTexture.equals(this.floorTexture))) {
//                HomeTexture oldFloorTexture = this.floorTexture;
//                this.floorTexture = floorTexture;
//                firePropertyChange(Property.FLOOR_TEXTURE.name(), oldFloorTexture, floorTexture);
//            }
//        }

        /**
         * Returns whether the floor of this room is visible or not.
         */
        public boolean isFloorVisible() {
            return this.floorVisible;
        }

        /**
         * Sets whether the floor of this room is visible or not. Once this room
         * is updated, listeners added to this room will receive a change notification.
         */
        public void setFloorVisible(boolean floorVisible) {
            if (floorVisible != this.floorVisible) {
                this.floorVisible = floorVisible;
                firePropertyChange(Property.FLOOR_VISIBLE.name(), !floorVisible, floorVisible);
            }
        }

        /**
         * Returns the floor shininess of this room.
         * @return a value between 0 (matt) and 1 (very shiny)
         * @since 3.0
         */
        public float getFloorShininess() {
            return this.floorShininess;
        }

        /**
         * Sets the floor shininess of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         * @since 3.0
         */
        public void setFloorShininess(float floorShininess) {
            if (floorShininess != this.floorShininess) {
                float oldFloorShininess = this.floorShininess;
                this.floorShininess = floorShininess;
                firePropertyChange(Property.FLOOR_SHININESS.name(), oldFloorShininess, floorShininess);
            }
        }

        /**
         * Returns the ceiling color color of this room.
         */
        public Integer getCeilingColor() {
            return this.ceilingColor;
        }

        /**
         * Sets the ceiling color of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         */
        public void setCeilingColor(Integer ceilingColor) {
            if (ceilingColor != this.ceilingColor
                    && (ceilingColor == null || !ceilingColor.equals(this.ceilingColor))) {
                Integer oldCeilingColor = this.ceilingColor;
                this.ceilingColor = ceilingColor;
                firePropertyChange(Property.CEILING_COLOR.name(), oldCeilingColor, ceilingColor);
            }
        }

        /**
         * Returns the ceiling texture of this room.
         */
//        public HomeTexture getCeilingTexture() {
//            return this.ceilingTexture;
//        }

        /**
         * Sets the ceiling texture of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         */
//        public void setCeilingTexture(HomeTexture ceilingTexture) {
//            if (ceilingTexture != this.ceilingTexture
//                    && (ceilingTexture == null || !ceilingTexture.equals(this.ceilingTexture))) {
//                HomeTexture oldCeilingTexture = this.ceilingTexture;
//                this.ceilingTexture = ceilingTexture;
//                firePropertyChange(Property.CEILING_TEXTURE.name(), oldCeilingTexture, ceilingTexture);
//            }
//        }

        /**
         * Returns whether the ceiling of this room is visible or not.
         */
        public boolean isCeilingVisible() {
            return this.ceilingVisible;
        }

        /**
         * Sets whether the ceiling of this room is visible or not. Once this room
         * is updated, listeners added to this room will receive a change notification.
         */
        public void setCeilingVisible(boolean ceilingVisible) {
            if (ceilingVisible != this.ceilingVisible) {
                this.ceilingVisible = ceilingVisible;
                firePropertyChange(Property.CEILING_VISIBLE.name(), !ceilingVisible, ceilingVisible);
            }
        }

        /**
         * Returns the ceiling shininess of this room.
         * @return a value between 0 (matt) and 1 (very shiny)
         * @since 3.0
         */
        public float getCeilingShininess() {
            return this.ceilingShininess;
        }

        /**
         * Sets the ceiling shininess of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         * @since 3.0
         */
        public void setCeilingShininess(float ceilingShininess) {
            if (ceilingShininess != this.ceilingShininess) {
                float oldCeilingShininess = this.ceilingShininess;
                this.ceilingShininess = ceilingShininess;
                firePropertyChange(Property.CEILING_SHININESS.name(), oldCeilingShininess, ceilingShininess);
            }
        }

        /**
         * Returns <code>true</code> if the ceiling should remain flat whatever its environment.
         * @since 7.0
         */
        public boolean isCeilingFlat() {
            return this.ceilingFlat;
        }

        /**
         * Sets whether the floor texture should remain flat. Once this room is updated,
         * listeners added to this room will receive a change notification.
         * @since 7.0
         */
        public void setCeilingFlat(boolean ceilingFlat) {
            if (ceilingFlat != this.ceilingFlat) {
                this.ceilingFlat = ceilingFlat;
                firePropertyChange(Property.CEILING_FLAT.name(), !ceilingFlat, ceilingFlat);
            }
        }

        /**
         * Returns the level which this room belongs to.
         * @since 3.4
         */
//        public Level getLevel() {
//            return this.level;
//        }

        /**
         * Sets the level of this room. Once this room is updated,
         * listeners added to this room will receive a change notification.
         * @since 3.4
         */
//        public void setLevel(Level level) {
//            if (level != this.level) {
//                Level oldLevel = this.level;
//                this.level = level;
//                firePropertyChange(Property.LEVEL.name(), oldLevel, level);
//            }
//        }

        /**
         * Returns <code>true</code> if this room is at the given <code>level</code>
         * or at a level with the same elevation and a smaller elevation index.
         * @since 3.4
         */
//        public boolean isAtLevel(Level level) {
//            return this.level == level
//                    || this.level != null && level != null
//                    && this.level.getElevation() == level.getElevation()
//                    && this.level.getElevationIndex() < level.getElevationIndex();
//        }

        /**
         * Returns the area of this room.
         */
        public float getArea() {
            if (this.areaCache == null) {
                Area roomArea = new Area(getShape());
                if (roomArea.isSingular()) {
                    this.areaCache = Math.abs(getSignedArea(getPoints()));
                } else {
                    // Add the surface of the different polygons of this room
                    float area = 0;
                    List<float []> currentPathPoints = new ArrayList<float[]>();
                    for (PathIterator it = roomArea.getPathIterator(null); !it.isDone(); ) {
                        float [] roomPoint = new float[2];
                        switch (it.currentSegment(roomPoint)) {
                            case PathIterator.SEG_MOVETO :
                                currentPathPoints.add(roomPoint);
                                break;
                            case PathIterator.SEG_LINETO :
                                currentPathPoints.add(roomPoint);
                                break;
                            case PathIterator.SEG_CLOSE :
                                float [][] pathPoints =
                                        currentPathPoints.toArray(new float [currentPathPoints.size()][]);
                                area += getSignedArea(pathPoints);
                                currentPathPoints.clear();
                                break;
                        }
                        it.next();
                    }
                    this.areaCache = area;
                }
            }
            return this.areaCache;
        }

        private float getSignedArea(float areaPoints [][]) {
            // From "Area of a General Polygon" algorithm described in
            // http://www.davidchandler.com/AreaOfAGeneralPolygon.pdf
            double area = 0; // Compute in double to avoid precision loss with complex areas
            for (int i = 1; i < areaPoints.length; i++) {
                area += (double)areaPoints [i][0] * areaPoints [i - 1][1];
                area -= (double)areaPoints [i][1] * areaPoints [i - 1][0];
            }
            area += (double)areaPoints [0][0] * areaPoints [areaPoints.length - 1][1];
            area -= (double)areaPoints [0][1] * areaPoints [areaPoints.length - 1][0];
            return (float)area / 2;
        }

        /**
         * Returns <code>true</code> if the points of this room are in clockwise order.
         */
        public boolean isClockwise() {
            return getSignedArea(getPoints()) < 0;
        }

        /**
         * Returns <code>true</code> if this room is comprised of only one polygon.
         */
        public boolean isSingular() {
            return new Area(getShape()).isSingular();
        }

        /**
         * Returns <code>true</code> if this room intersects
         * with the horizontal rectangle which opposite corners are at points
         * (<code>x0</code>, <code>y0</code>) and (<code>x1</code>, <code>y1</code>).
         */
        public boolean intersectsRectangle(float x0, float y0, float x1, float y1) {
            Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
            rectangle.add(x1, y1);
            return getShape().intersects(rectangle);
        }

        /**
         * Returns <code>true</code> if this room contains
         * the point at (<code>x</code>, <code>y</code>) with a given <code>margin</code>.
         */
        public boolean containsPoint(float x, float y, float margin) {
            return containsShapeAtWithMargin(getShape(), x, y, margin);
        }

        /**
         * Returns the index of the point of this room equal to
         * the point at (<code>x</code>, <code>y</code>) with a given <code>margin</code>.
         * @return the index of the first found point or -1.
         */
        public int getPointIndexAt(float x, float y, float margin) {
            for (int i = 0; i < this.points.length; i++) {
                if (Math.abs(x - this.points [i][0]) <= margin && Math.abs(y - this.points [i][1]) <= margin) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Returns <code>true</code> if the center point at which is displayed the name
         * of this room is equal to the point at (<code>x</code>, <code>y</code>)
         * with a given <code>margin</code>.
         */
        public boolean isNameCenterPointAt(float x, float y, float margin) {
            return Math.abs(x - getXCenter() - getNameXOffset()) <= margin
                    && Math.abs(y - getYCenter() - getNameYOffset()) <= margin;
        }

        /**
         * Returns <code>true</code> if the center point at which is displayed the area
         * of this room is equal to the point at (<code>x</code>, <code>y</code>)
         * with a given <code>margin</code>.
         */
        public boolean isAreaCenterPointAt(float x, float y, float margin) {
            return Math.abs(x - getXCenter() - getAreaXOffset()) <= margin
                    && Math.abs(y - getYCenter() - getAreaYOffset()) <= margin;
        }

        /**
         * Returns <code>true</code> if <code>shape</code> contains
         * the point at (<code>x</code>, <code>y</code>)
         * with a given <code>margin</code>.
         */
        private boolean containsShapeAtWithMargin(Shape shape, float x, float y, float margin) {
            if (margin == 0) {
                return shape.contains(x, y);
            } else {
                return shape.intersects(x - margin, y - margin, 2 * margin, 2 * margin);
            }
        }

        /**
         * Returns the shape matching this room.
         */
        private Shape getShape() {
            if (this.shapeCache == null) {
                GeneralPath roomShape = new GeneralPath();
                roomShape.moveTo(this.points [0][0], this.points [0][1]);
                for (int i = 1; i < this.points.length; i++) {
                    roomShape.lineTo(this.points [i][0], this.points [i][1]);
                }
                roomShape.closePath();
                // Cache roomShape
                this.shapeCache = roomShape;
            }
            return this.shapeCache;
        }

        /**
         * Moves this room of (<code>dx</code>, <code>dy</code>) units.
         */
        public void move(float dx, float dy) {
            if (dx != 0 || dy != 0) {
                float [][] points = getPoints();
                for (int i = 0; i < points.length; i++) {
                    points [i][0] += dx;
                    points [i][1] += dy;
                }
                updatePoints(points);
            }
        }

        /**
         * Returns a clone of this room.
         */
//        @Override
//        public Room clone() {
//            Room clone = (Room)super.clone();
//            clone.level = null;
//            return clone;
//        }
    }
}
